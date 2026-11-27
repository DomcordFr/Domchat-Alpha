package me.lucid.domchat.data.ai

import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.data.model.Assistant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * SillyTavern-grade Roleplay Prompt & Macro Assembler.
 * Handles depth-based prompt injection, non-recursive single-pass macro evaluation, and context layering.
 */
object RoleplayPromptAssembler {

    private val MACRO_SINGLE_PASS_REGEX = Regex(
        """\{\{(char|char_name|user|user_name|user_pronouns|user_pronoun|pronouns|sub|obj|poss|poss_p|ref|bot|model|time|date|lastMessage|lastUserMessage|lastCharMessage|random::[^}]+)\}\}|\{(char|char_name|user|user_name|bot)\}|<(?i:char|user|bot)>""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Replaces standard SillyTavern macros across arbitrary prompt text in a single pass.
     * Prevents macro recursion where user input or character name containing macros triggers secondary replacements.
     */
    fun evaluateMacros(
        text: String,
        charName: String,
        userName: String,
        modelName: String = "",
        lastMessage: String = "",
        lastUserMessage: String = "",
        lastCharMessage: String = "",
        userPronouns: String = ""
    ): String {
        if (text.isBlank()) return ""

        val timeStr by lazy { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
        val dateStr by lazy { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }

        return MACRO_SINGLE_PASS_REGEX.replace(text) { match ->
            val value = match.value
            val macroKey = when {
                value.startsWith("{{") && value.endsWith("}}") -> value.substring(2, value.length - 2)
                value.startsWith("{") && value.endsWith("}") -> value.substring(1, value.length - 1)
                value.startsWith("<") && value.endsWith(">") -> value.substring(1, value.length - 1)
                else -> value
            }.lowercase(Locale.ROOT)
            when {
                macroKey == "char" || macroKey == "char_name" || macroKey == "bot" -> charName
                macroKey == "user" || macroKey == "user_name" -> userName
                macroKey == "user_pronouns" || macroKey == "user_pronoun" || macroKey == "pronouns" -> userPronouns
                macroKey == "sub" -> pronounParts(userPronouns).sub
                macroKey == "obj" -> pronounParts(userPronouns).obj
                macroKey == "poss" -> pronounParts(userPronouns).poss
                macroKey == "poss_p" -> pronounParts(userPronouns).possP
                macroKey == "ref" -> pronounParts(userPronouns).ref
                macroKey == "model" -> modelName
                macroKey == "time" -> timeStr
                macroKey == "date" -> dateStr
                macroKey.startsWith("random::") -> {
                    val rawOptions = value.removePrefix("{{").substringAfter("::").removeSuffix("}}")
                    val options = rawOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (options.isNotEmpty()) options[Random.nextInt(options.size)] else ""
                }
                macroKey == "lastmessage" -> lastMessage
                macroKey == "lastusermessage" -> lastUserMessage
                macroKey == "lastcharmessage" -> lastCharMessage
                else -> match.value
            }
        }
    }

    /** Parses a slash-separated pronoun string (e.g. "he/him/his/himself") into its five grammatical cases. */
    private fun pronounParts(pronouns: String): PronounParts {
        val parts = pronouns.split("/").map { it.trim() }.filter { it.isNotEmpty() }
        val sub = parts.getOrElse(0) { "" }
        val obj = parts.getOrElse(1) { "" }
        val poss = parts.getOrElse(2) { obj }
        val possP = parts.getOrElse(3) { poss }
        val ref = parts.getOrElse(4) { reflexiveFor(obj) }
        return PronounParts(sub, obj, poss, possP, ref)
    }

    private fun reflexiveFor(obj: String): String {
        return when (obj.lowercase()) {
            "them" -> "themselves"
            else -> if (obj.isNotBlank()) obj + "self" else ""
        }
    }

    private data class PronounParts(
        val sub: String,
        val obj: String,
        val poss: String,
        val possP: String,
        val ref: String,
    )

    /**
     * Injects an Author's Note or directive at depth D from the end of the message list.
     * Depth 0 = injected as the very last message.
     * Depth 3 = injected 3 messages before the final message.
     */
    fun injectAuthorsNote(
        messages: List<UIMessage>,
        authorsNote: String,
        depth: Int = 3,
        role: MessageRole = MessageRole.SYSTEM
    ): List<UIMessage> {
        if (authorsNote.isBlank() || messages.isEmpty()) return messages

        var targetIndex = (messages.size - depth).coerceIn(0, messages.size)
        // Prevent inserting between tool call and tool result
        while (targetIndex > 0 && targetIndex < messages.size && messages[targetIndex].role == MessageRole.TOOL) {
            targetIndex--
        }

        val noteText = if (role == MessageRole.USER) {
            "[Author's Note: ${authorsNote.trim()}]"
        } else {
            authorsNote.trim()
        }

        val noteMsg = UIMessage(
            role = role,
            parts = listOf(UIMessagePart.Text(text = noteText))
        )

        val result = messages.toMutableList()
        result.add(targetIndex, noteMsg)
        return result
    }

    /**
     * Injects an arbitrary directive or preset message at depth D from the end of the message list.
     */
    fun injectPresetMessage(
        messages: List<UIMessage>,
        content: String,
        depth: Int = 0,
        role: MessageRole = MessageRole.SYSTEM
    ): List<UIMessage> {
        if (content.isBlank() || messages.isEmpty()) return messages

        var targetIndex = (messages.size - depth).coerceIn(0, messages.size)
        while (targetIndex > 0 && targetIndex < messages.size && messages[targetIndex].role == MessageRole.TOOL) {
            targetIndex--
        }

        val noteMsg = UIMessage(
            role = role,
            parts = listOf(UIMessagePart.Text(text = content.trim()))
        )

        val result = messages.toMutableList()
        result.add(targetIndex, noteMsg)
        return result
    }

    /**
     * Builds structured roleplay system prompts preserving separated SillyTavern blocks.
     */
    fun assembleSystemPrompt(
        globalPrompt: String?,
        assistant: Assistant,
        userName: String,
        modelName: String = ""
    ): String {
        val builder = StringBuilder()

        // 1. Global Roleplay Directive / Master Jailbreak
        if (!globalPrompt.isNullOrBlank()) {
            builder.append(evaluateMacros(globalPrompt.trim(), assistant.name, userName, modelName))
                .append("\n\n")
        }

        // 2. Character Persona & Core System Prompt
        if (assistant.systemPrompt.isNotBlank()) {
            builder.append(evaluateMacros(assistant.systemPrompt.trim(), assistant.name, userName, modelName))
                .append("\n\n")
        }

        return builder.toString().trim()
    }
}
