package me.lucid.domchat.data.sync.importer

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.MessageNode
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.common.http.jsonArrayOrNull
import me.lucid.common.http.jsonObjectOrNull
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Conversation
import me.lucid.domchat.utils.JsonInstant
import me.lucid.domchat.utils.jsonPrimitiveOrNull
import java.io.File
import java.time.Instant
import kotlin.time.Clock
import kotlin.time.Instant as KotlinInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.Uuid

object SillyTavernImporter {
    data class Result(
        val conversations: List<Conversation>,
        val assistants: List<Assistant> = emptyList(),
    )

    fun import(file: File): Result {
        return import(file.readText())
    }

    fun import(json: String): Result {
        val root = JsonInstant.parseToJsonElement(json)
        val elements = when (root) {
            is JsonArray -> root
            is JsonObject -> root["messages"]?.jsonArrayOrNull
                ?: root["conversation"]?.jsonArrayOrNull
                ?: listOf(root)
            else -> emptyList()
        }

        var header: Header? = null
        val uiMessages = mutableListOf<UIMessage>()

        elements.forEach { element ->
            when {
                looksLikeHeader(element) -> header = parseHeader(element)
                else -> parseMessage(element)?.let { uiMessages.add(it) }
            }
        }

        val resolvedHeader = header ?: Header("", "", "SillyTavern Import")
        val assistant = Assistant(
            name = resolvedHeader.characterName.ifBlank { "Imported Character" },
            systemPrompt = "",
        )
        val conversation = Conversation(
            id = Uuid.random(),
            assistantId = assistant.id,
            title = resolvedHeader.title,
            messageNodes = uiMessages.map { MessageNode.of(it) },
            createAt = Instant.now(),
            updateAt = Instant.now(),
        )

        return Result(
            conversations = listOf(conversation),
            assistants = listOf(assistant),
        )
    }

    private data class Header(
        val characterName: String,
        val userName: String,
        val title: String,
    )

    private fun looksLikeHeader(element: JsonElement): Boolean {
        val obj = element.jsonObjectOrNull ?: return false
        // A header carries identity fields (name / character_name) but must never
        // carry message body (mes / content). Regular messages with a name field
        // always carry mes or content, so they are never misclassified as a header.
        return (obj["name"] != null || obj["character_name"] != null)
            && obj["mes"] == null
            && obj["content"] == null
    }

    private fun parseHeader(element: JsonElement): Header {
        val obj = element.jsonObjectOrNull ?: return Header("", "", "SillyTavern Import")
        val characterName = obj.string("character_name", "name")?.ifBlank { null } ?: ""
        val userName = obj.string("user_name")?.ifBlank { null } ?: ""
        val title = characterName.ifBlank { obj.string("title") ?: "SillyTavern Import" }
        return Header(characterName, userName, title)
    }

    private fun parseMessage(element: JsonElement): UIMessage? {
        val obj = element.jsonObjectOrNull ?: return null
        val body = obj.string("mes", "content")?.takeIf { it.isNotBlank() }
            ?: return null

        val role = when (obj.string("role")?.lowercase()) {
            "assistant" -> MessageRole.ASSISTANT
            "system" -> MessageRole.SYSTEM
            else -> MessageRole.USER
        }

        val parts = mutableListOf<UIMessagePart>()
        parts.add(UIMessagePart.Text(text = body))

        parseSwipes(obj["swipes"]?.jsonArrayOrNull).forEach { swipeText ->
            parts.add(
                UIMessagePart.Text(
                    text = swipeText,
                    metadata = buildJsonObject { put("source", "sillytavern_swipe") },
                )
            )
        }

        if (parts.isEmpty()) return null

        return UIMessage(
            id = obj.string("id")?.toUuidOrNull() ?: Uuid.random(),
            role = role,
            parts = parts,
            createdAt = obj.long("timestamp", "time", "created", "createdAt")
                ?.let { KotlinInstant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault()) }
                ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        )
    }

    private fun parseSwipes(swipes: JsonArray?): List<String> {
        if (swipes == null) return emptyList()
        return swipes.mapNotNull { swipe ->
            val swipeText = when (swipe) {
                is JsonPrimitive -> swipe.jsonPrimitiveOrNull?.contentOrNull
                is JsonObject -> swipe.string("mes")
                else -> null
            }
            swipeText?.takeIf { it.isNotBlank() }
        }
    }

    private fun JsonObject.string(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitiveOrNull?.contentOrNull
        }
    }

    private fun JsonObject.long(vararg keys: String): Long? {
        return keys.firstNotNullOfOrNull { key ->
            this[key]?.jsonPrimitiveOrNull?.longOrNull
                ?: this[key]?.jsonPrimitiveOrNull?.contentOrNull?.toLongOrNull()
        }
    }

    private fun String?.toUuidOrNull(): Uuid? {
        return this?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    }
}
