package me.lucid.domchat.data.sync

import android.content.Context
import at.bitfire.dav4jvm.okhttp.DavCollection
import at.bitfire.dav4jvm.okhttp.Response
import at.bitfire.dav4jvm.okhttp.exception.NotFoundException
import at.bitfire.dav4jvm.property.webdav.DisplayName
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.lucid.ai.provider.ProviderSetting
import me.lucid.tts.provider.TTSProviderSetting
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.data.datastore.SettingsStore
import me.lucid.domchat.data.datastore.SecretKeyManager
import me.lucid.domchat.data.datastore.WebDavConfig
import me.lucid.domchat.data.datastore.sanitize
import me.lucid.domchat.data.db.AppDatabase
import me.lucid.domchat.utils.LogUtil
import me.lucid.workspace.hasUsableRootfs
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val TAG = "DataSync"

class WebdavSync(
    private val settingsStore: SettingsStore,
    private val json: Json,
    private val context: Context,
    private val secretKeyManager: SecretKeyManager,
    private val appDatabase: AppDatabase,
    private val webDavClientFactory: WebDavClientFactory,
) {
    suspend fun testWebdav(webDavConfig: WebDavConfig) {
        val davCollection = webDavClientFactory.collection(webDavConfig, path = "")

        withContext(Dispatchers.IO) {
            davCollection.propfind(depth = 1) { response, relation ->
                LogUtil.i(TAG, "testWebdav: $response | $relation")
            }
        }
    }

    suspend fun backupToWebDav(webDavConfig: WebDavConfig) = withContext(Dispatchers.IO) {
        val file = prepareBackupFile(webDavConfig)
        val collection = webDavClientFactory.collection(webDavConfig)
        collection.ensureCollectionExists()
        val target = webDavClientFactory.collection(webDavConfig, file.name)
        webDavClientFactory.putFile(target, file) { response ->
            LogUtil.i(TAG, "backupToWebDav: $response")
        }
    }

    suspend fun listBackupFiles(webDavConfig: WebDavConfig): List<WebDavBackupItem> =
        withContext(Dispatchers.IO) {
            val collection = webDavClientFactory.collection(webDavConfig)
            val files = mutableListOf<WebDavBackupItem>()
            collection.propfind(depth = 1) { response, relation ->
                LogUtil.i(TAG, "listBackupFiles: ${response.properties} ${response.href}")
                if (relation == Response.HrefRelation.MEMBER) {
                    val displayName = response.properties.filterIsInstance<DisplayName>()
                        .firstOrNull()?.displayName ?: "Unknown"
                    val size = response.properties.filterIsInstance<GetContentLength>()
                        .firstOrNull()?.contentLength ?: 0L
                    val lastModified = response.properties.filterIsInstance<GetLastModified>()
                        .firstOrNull()?.lastModified ?: Instant.EPOCH
                    files.add(
                        WebDavBackupItem(
                            href = response.href.toString(),
                            displayName = displayName,
                            size = size,
                            lastModified = lastModified,
                        )
                    )
                }
            }
            files
        }

    suspend fun restoreFromWebDav(
        webDavConfig: WebDavConfig,
        item: WebDavBackupItem,
    ): RestoreResult = withContext(Dispatchers.IO) {
        val collection = webDavClientFactory.hrefCollection(webDavConfig, item.href)
        val backupFile = File.createTempFile("webdav_restore_", ".zip", context.cacheDir)

        collection.get(
            accept = "",
            headers = null,
        ) { response ->
            if (response.isSuccessful) {
                LogUtil.i(
                    TAG,
                    "restoreFromWebDav: Downloading ${item.displayName} to ${backupFile.absolutePath}",
                )
                response.body?.byteStream()?.use { inputStream ->
                    backupFile.sink().buffer().outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                LogUtil.e(
                    TAG,
                    "restoreFromWebDav: Failed to download ${item.displayName}, response: $response",
                )
                throw Exception("Failed to download backup file: ${response.message}")
            }
        }

        LogUtil.i(TAG, "restoreFromWebDav: Downloaded ${backupFile.length()} bytes")

        try {
            restoreFromBackupFile(backupFile)
        } finally {
            if (backupFile.exists()) {
                backupFile.delete()
                LogUtil.i(TAG, "restoreFromWebDav: Cleaned up temporary backup file")
            }
        }
    }

    suspend fun deleteWebDavBackupFile(webDavConfig: WebDavConfig, item: WebDavBackupItem) =
        withContext(Dispatchers.IO) {
            val collection = webDavClientFactory.hrefCollection(webDavConfig, item.href)
            collection.delete { response ->
                LogUtil.i(TAG, "deleteWebDavBackupFile: $response")
            }
        }

    suspend fun restoreFromLocalFile(file: File, webDavConfig: WebDavConfig): RestoreResult =
        withContext(Dispatchers.IO) {
            LogUtil.i(TAG, "restoreFromLocalFile: Starting restore from ${file.absolutePath}")

            if (!file.exists()) {
                throw Exception("Backup file does not exist")
            }

            if (!file.canRead()) {
                throw Exception("Cannot read backup file")
            }

            try {
                restoreFromBackupFile(file)
            } catch (e: Exception) {
                LogUtil.e(TAG, "restoreFromLocalFile: Failed to restore from local file", e)
                throw Exception("Restore failed: ${e.message}")
            }
        }

    suspend fun prepareBackupFile(
        webDavConfig: WebDavConfig,
        selectedContent: Set<BackupContentOption>? = null,
        selectedKeyIds: Set<kotlin.uuid.Uuid>? = null,
    ): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = File(context.cacheDir, "Domchat_backup_$timestamp.zip")
        if (backupFile.exists()) {
            backupFile.delete()
        }

        val includesDatabase = webDavConfig.items.contains(WebDavConfig.BackupItem.DATABASE)
        val includesFiles = webDavConfig.items.contains(WebDavConfig.BackupItem.FILES)

        // Determine which managed dirs and DB table sets to include based on selectedContent.
        // When selectedContent is null (e.g., WebDAV full backup), everything is included.
        val managedDirsToExport = if (selectedContent == null) {
            BackupArchiveFormat.MANAGED_FILE_DIRS
        } else {
            buildList {
                if (BackupContentOption.CHARACTERS in selectedContent) {
                    add("avatars")
                    add("assistant_backgrounds")
                    add("wallpapers")
                    add("backgrounds")
                    add("custom_icons")
                    add("custom_fonts")
                }
                if (BackupContentOption.CHATS in selectedContent) {
                    add("upload")
                    add("chat_files")
                    add("tool_outputs")
                    add("images")
                    add("wallpapers")
                }
                if (BackupContentOption.PROVIDERS in selectedContent) {
                    add("auto_icons")
                    add("custom_icons")
                }
                if (BackupContentOption.SETTINGS in selectedContent) {
                    add("wallpapers")
                    add("backgrounds")
                    add("custom_fonts")
                    add("custom_icons")
                    add("auto_icons")
                }
                if (BackupContentOption.WORKSPACES in selectedContent) {
                    add(BackupArchiveFormat.WORKSPACES_DIR)
                }
                if (BackupContentOption.SKILLS in selectedContent) {
                    add("skills")
                }
                if (BackupContentOption.LOREBOOKS in selectedContent) {
                    add("lorebook_covers")
                    add("lorebook_attachments")
                }
            }.filter { BackupArchiveFormat.MANAGED_FILE_DIRS.contains(it) }
        }

        val allowedDbTables: Set<String>? = if (selectedContent == null) {
            null // null = all tables
        } else {
            buildSet {
                if (BackupContentOption.CHATS in selectedContent) addAll(DatabaseSanitizer.CHAT_TABLES)
                if (BackupContentOption.WORKSPACES in selectedContent) addAll(DatabaseSanitizer.WORKSPACE_TABLES)
            }
        }

        val manifest = BackupManifest(
            includesDatabase = includesDatabase,
            includesFiles = includesFiles,
            managedFileDirs = if (includesFiles) managedDirsToExport else emptyList(),
            sharedPrefsStores = BackupArchiveFormat.PORTABLE_SHARED_PREF_STORES,
            selectedContent = selectedContent?.map { it.name } ?: emptyList(),
            workspacesMetadata = collectWorkspaceExportMetadata(
                include = selectedContent == null || BackupContentOption.WORKSPACES in selectedContent,
            ),
        )

        ZipOutputStream(backupFile.sink().buffer().outputStream()).use { zipOut ->
            val rawSettings = settingsStore.settingsFlow.value
            val settingsForExport = secretKeyManager.populateSecretsForExport(
                settings = rawSettings,
                selectedKeyIds = if (selectedContent != null && BackupContentOption.API_KEYS !in selectedContent) {
                    emptySet() // export no keys
                } else {
                    selectedKeyIds
                },
            )

            // Apply content filtering to the Settings object
            val filteredSettings = if (selectedContent == null) {
                settingsForExport
            } else {
                filterSettingsForExport(settingsForExport, selectedContent)
            }

            addVirtualFileToZip(
                zipOut = zipOut,
                name = BackupArchiveFormat.SETTINGS_ENTRY,
                content = json.encodeToString(filteredSettings),
            )
            addVirtualFileToZip(
                zipOut = zipOut,
                name = BackupArchiveFormat.MANIFEST_ENTRY,
                content = json.encodeToString(manifest),
            )

            BackupArchiveFormat.PORTABLE_SHARED_PREF_STORES.forEach { storeName ->
                val snapshot = exportSharedPreferencesSnapshot(context, storeName)
                addVirtualFileToZip(
                    zipOut = zipOut,
                    name = BackupArchiveFormat.prefEntryName(storeName),
                    content = json.encodeToString(snapshot),
                )
            }

            if (includesDatabase) {
                checkpointDatabase()
                if (allowedDbTables != null && allowedDbTables.isEmpty()) {
                    // No DB tables selected — skip database entirely
                    LogUtil.i(TAG, "prepareBackupFile: Skipping database (no DB content selected)")
                } else if (allowedDbTables != null) {
                    // Export a filtered copy of the database
                    val filteredDb = DatabaseSanitizer.createFilteredExportDatabase(context, allowedDbTables)
                    try {
                        addFilteredDatabaseEntries(zipOut, filteredDb)
                    } finally {
                        // Clean up temp export db (use the sanitized name pattern)
                        context.deleteDatabase(filteredDb.name)
                    }
                } else {
                    addDatabaseEntries(zipOut)
                }
            }

            if (includesFiles) {
                addManagedFileEntries(zipOut, managedDirsToExport)
            }
        }

        backupFile
    }

    /**
     * Snapshot workspace environment metadata so a restore can reinstall the
     * (excluded-from-zip) Linux rootfs in the background. Filesystem checks
     * only — no proot execution — so this stays cheap during export.
     */
    private suspend fun collectWorkspaceExportMetadata(include: Boolean): List<WorkspaceExportMetadata> {
        if (!include) return emptyList()
        val entities = runCatching { appDatabase.workspaceDao().getAll() }
            .getOrDefault(emptyList())
        if (entities.isEmpty()) return emptyList()
        val workspacesRoot = File(context.filesDir, BackupArchiveFormat.WORKSPACES_DIR)
        return entities.map { ws ->
            val linuxDir = File(File(workspacesRoot, ws.root), "linux")
            val hasRootfs = runCatching { linuxDir.hasUsableRootfs() }.getOrDefault(false)
            val hasPython = if (hasRootfs) {
                File(linuxDir, "usr/bin/python3").isFile || File(linuxDir, "bin/python3").isFile
            } else {
                false
            }
            WorkspaceExportMetadata(
                id = ws.id,
                name = ws.name,
                root = ws.root,
                hasRootfs = hasRootfs,
                hasPython = hasPython,
                rootfsUrl = null,
            )
        }
    }

    /**
     * Filter a [Settings] object to only include fields corresponding to [selectedContent].
     * Non-selected fields are replaced with their empty/default equivalents so no
     * structural data (provider IDs, etc.) is lost, but content the user opted-out of is blank.
     */
    private fun filterSettingsForExport(
        settings: Settings,
        selectedContent: Set<BackupContentOption>,
    ): Settings {
        var result = settings
        if (BackupContentOption.CHARACTERS !in selectedContent) {
            result = result.copy(assistants = emptyList())
        }
        if (BackupContentOption.PROVIDERS !in selectedContent) {
            result = result.copy(providers = emptyList(), ttsProviders = emptyList())
        } else if (BackupContentOption.API_KEYS !in selectedContent) {
            // Keep providers structure, but keys are already blanked out by populateSecretsForExport
        }
        if (BackupContentOption.LOREBOOKS !in selectedContent) {
            result = result.copy(lorebooks = emptyList())
        }
        if (BackupContentOption.SKILLS !in selectedContent) {
            result = result.copy(skills = emptyList())
        }
        // If SETTINGS is not selected, reset general appearance/behavior preferences to defaults
        // so they don't override the importing device's settings. Data lists (providers, assistants,
        // lorebooks, skills) are already handled by their respective options above.
        if (BackupContentOption.SETTINGS !in selectedContent) {
            val defaults = Settings()
            result = result.copy(
                dynamicColor = defaults.dynamicColor,
                themeId = defaults.themeId,
                developerMode = defaults.developerMode,
                enableRagLogging = defaults.enableRagLogging,
                displaySetting = defaults.displaySetting,
                enableWebSearch = defaults.enableWebSearch,
                titlePrompt = defaults.titlePrompt,
                translatePrompt = defaults.translatePrompt,
                suggestionPrompt = defaults.suggestionPrompt,
                learningModePrompt = defaults.learningModePrompt,
                ocrPrompt = defaults.ocrPrompt,
                ttsAutoplayMode = defaults.ttsAutoplayMode,
                chatStorage = defaults.chatStorage,
                textSelectionConfig = defaults.textSelectionConfig,
                assistantOverlayConfig = defaults.assistantOverlayConfig,
                webServerEnabled = defaults.webServerEnabled,
                webServerPort = defaults.webServerPort,
                webServerJwtEnabled = defaults.webServerJwtEnabled,
                webServerAccessPassword = defaults.webServerAccessPassword,
            )
        }
        return result
    }


    data class RestoreResult(
        val sanitization: DatabaseSanitizer.SanitizationResult,
        val settingsCleanup: BackupCleanupResult,
        val workspacesMetadata: List<WorkspaceExportMetadata> = emptyList(),
    )

    private suspend fun restoreFromBackupFile(backupFile: File): RestoreResult =
        withContext(Dispatchers.IO) {
            LogUtil.i(TAG, "restoreFromBackupFile: Starting restore from ${backupFile.absolutePath}")

            var unsupportedZipEntriesBytes = 0L
            var settingsCleanupResult = BackupCleanupResult()
            var sanitizationResult = DatabaseSanitizer.SanitizationResult()
            var settingsJson: String? = null
            var manifest: BackupManifest? = null
            val stagedPrefs = linkedMapOf<String, SharedPreferencesSnapshot>()
            val stagedManagedDirs = linkedSetOf<String>()
            var foundSupportedEntry = false

            val restoreTempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            val stagedFilesDir = File(restoreTempDir, "files")
            val stagedDbDir = File(restoreTempDir, "db")
            restoreTempDir.mkdirs()
            stagedFilesDir.mkdirs()
            stagedDbDir.mkdirs()

            try {
                ZipInputStream(backupFile.source().buffer().inputStream()).use { zipIn ->
                    var entry: ZipEntry?
                    while (zipIn.nextEntry.also { entry = it } != null) {
                        val zipEntry = entry ?: continue
                        val normalizedName = BackupArchiveFormat.normalizeEntryName(zipEntry.name)
                        LogUtil.i(TAG, "restoreFromBackupFile: Processing entry $normalizedName")

                        when {
                            normalizedName == BackupArchiveFormat.SETTINGS_ENTRY -> {
                                settingsJson = zipIn.readBytes().toString(Charsets.UTF_8)
                                foundSupportedEntry = true
                            }

                            normalizedName == BackupArchiveFormat.MANIFEST_ENTRY -> {
                                val manifestJson = zipIn.readBytes().toString(Charsets.UTF_8)
                                manifest = json.decodeFromString<BackupManifest>(manifestJson)
                                foundSupportedEntry = true
                            }

                            BackupArchiveFormat.isDatabaseEntry(normalizedName) -> {
                                stageDatabaseEntry(zipIn, stagedDbDir, normalizedName)
                                foundSupportedEntry = true
                            }

                            normalizedName.startsWith("${BackupArchiveFormat.PREFS_DIR}/") &&
                                normalizedName.endsWith(".json") -> {
                                val storeName = normalizedName
                                    .removePrefix("${BackupArchiveFormat.PREFS_DIR}/")
                                    .removeSuffix(".json")
                                if (BackupArchiveFormat.PORTABLE_SHARED_PREF_STORES.contains(storeName)) {
                                    val snapshotJson = zipIn.readBytes().toString(Charsets.UTF_8)
                                    val snapshot =
                                        json.decodeFromString<SharedPreferencesSnapshot>(snapshotJson)
                                    stagedPrefs[storeName] = snapshot
                                    foundSupportedEntry = true
                                } else {
                                    unsupportedZipEntriesBytes += zipEntry.size.coerceAtLeast(0L)
                                }
                            }

                            BackupArchiveFormat.managedDirForEntry(normalizedName) != null -> {
                                val managedDir = BackupArchiveFormat.managedDirForEntry(normalizedName)
                                    ?: error("Managed directory was null for $normalizedName")
                                stageManagedFileEntry(zipIn, stagedFilesDir, normalizedName, zipEntry.isDirectory)
                                stagedManagedDirs.add(managedDir)
                                foundSupportedEntry = true
                            }

                            else -> {
                                LogUtil.i(
                                    TAG,
                                    "restoreFromBackupFile: Skipping unsupported entry $normalizedName (${zipEntry.size} bytes)",
                                )
                                unsupportedZipEntriesBytes += zipEntry.size.coerceAtLeast(0L)
                            }
                        }

                        zipIn.closeEntry()
                    }
                }

                if (!foundSupportedEntry) {
                    throw Exception("Backup file did not contain any supported entries")
                }

                val sanitizedSettings = settingsJson?.let { rawSettingsJson ->
                    try {
                        val parsedSettings = json.decodeFromString<Settings>(rawSettingsJson)
                        parsedSettings.sanitize().also { (_, cleanup) ->
                            settingsCleanupResult = cleanup
                        }
                    } catch (e: Exception) {
                        LogUtil.e(TAG, "restoreFromBackupFile: Failed to parse settings", e)
                        throw Exception("Failed to restore settings: ${e.message}")
                    }
                }

                restorePortableSharedPreferences(
                    stagedSnapshots = stagedPrefs,
                    manifest = manifest,
                )

                val stagedDatabase = File(stagedDbDir, BackupArchiveFormat.DB_ENTRY)
                if (stagedDatabase.exists()) {
                    LogUtil.i(TAG, "restoreFromBackupFile: Starting database sanitization")
                    try {
                        sanitizationResult = restoreDatabase(stagedDatabase, manifest)
                        LogUtil.i(TAG, "restoreFromBackupFile: Database restored and sanitized")
                        resetWorkspaceShellStatesForMissingRootfs()
                    } catch (e: Exception) {
                        LogUtil.e(TAG, "restoreFromBackupFile: Failed to restore database", e)
                        throw Exception("Database sanitization failed: ${e.message}")
                    }
                }

                sanitizedSettings?.let { (cleanedSettings, _) ->
                    val finalSettings = if (manifest != null && manifest.selectedContent.isNotEmpty()) {
                        val live = settingsStore.settingsFlow.value
                        val selected = manifest.selectedContent.toSet()

                        val resolvedProviders = if (BackupContentOption.PROVIDERS.name in selected) {
                            if (BackupContentOption.API_KEYS.name in selected) {
                                cleanedSettings.providers
                            } else {
                                val liveMap = live.providers.associateBy { it.id }
                                cleanedSettings.providers.map { restored ->
                                    val liveProv = liveMap[restored.id]
                                    if (liveProv != null) {
                                        when (restored) {
                                            is ProviderSetting.OpenAI -> (liveProv as? ProviderSetting.OpenAI)?.let {
                                                restored.copy(apiKey = it.apiKey, apiKeyPool = it.apiKeyPool)
                                            } ?: restored
                                            is ProviderSetting.Google -> (liveProv as? ProviderSetting.Google)?.let {
                                                restored.copy(apiKey = it.apiKey, privateKey = it.privateKey, apiKeyPool = it.apiKeyPool)
                                            } ?: restored
                                            is ProviderSetting.Claude -> (liveProv as? ProviderSetting.Claude)?.let {
                                                restored.copy(apiKey = it.apiKey, apiKeyPool = it.apiKeyPool)
                                            } ?: restored
                                            else -> restored
                                        }
                                    } else restored
                                }
                            }
                        } else {
                            live.providers
                        }

                        val resolvedTtsProviders = if (BackupContentOption.PROVIDERS.name in selected) {
                            if (BackupContentOption.API_KEYS.name in selected) {
                                cleanedSettings.ttsProviders
                            } else {
                                val liveTtsMap = live.ttsProviders.associateBy { it.id }
                                cleanedSettings.ttsProviders.map { restored ->
                                    val liveTts = liveTtsMap[restored.id]
                                    if (liveTts != null) {
                                        when (restored) {
                                            is TTSProviderSetting.OpenAI -> (liveTts as? TTSProviderSetting.OpenAI)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.Gemini -> (liveTts as? TTSProviderSetting.Gemini)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.MiniMax -> (liveTts as? TTSProviderSetting.MiniMax)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.ElevenLabs -> (liveTts as? TTSProviderSetting.ElevenLabs)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.Qwen -> (liveTts as? TTSProviderSetting.Qwen)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.Cartesia -> (liveTts as? TTSProviderSetting.Cartesia)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.FishAudio -> (liveTts as? TTSProviderSetting.FishAudio)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.PlayHT -> (liveTts as? TTSProviderSetting.PlayHT)?.let { restored.copy(apiKey = it.apiKey) } ?: restored
                                            is TTSProviderSetting.SystemTTS -> restored
                                        }
                                    } else restored
                                }
                            }
                        } else {
                            live.ttsProviders
                        }

                        live.copy(
                            assistants = if (BackupContentOption.CHARACTERS.name in selected) cleanedSettings.assistants else live.assistants,
                            assistantTags = if (BackupContentOption.CHARACTERS.name in selected) cleanedSettings.assistantTags else live.assistantTags,
                            recentlyUsedAssistants = if (BackupContentOption.CHARACTERS.name in selected) cleanedSettings.recentlyUsedAssistants else live.recentlyUsedAssistants,
                            providers = resolvedProviders,
                            ttsProviders = resolvedTtsProviders,
                            providerTags = if (BackupContentOption.PROVIDERS.name in selected) cleanedSettings.providerTags else live.providerTags,
                            lorebooks = if (BackupContentOption.LOREBOOKS.name in selected) cleanedSettings.lorebooks else live.lorebooks,
                            skills = if (BackupContentOption.SKILLS.name in selected) cleanedSettings.skills else live.skills,
                            mcpServers = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.mcpServers else live.mcpServers,
                            searchServices = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.searchServices else live.searchServices,
                            searchCommonOptions = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.searchCommonOptions else live.searchCommonOptions,
                            searchServiceSelected = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.searchServiceSelected else live.searchServiceSelected,
                            modes = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.modes else live.modes,
                            dynamicColor = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.dynamicColor else live.dynamicColor,
                            themeId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.themeId else live.themeId,
                            developerMode = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.developerMode else live.developerMode,
                            enableRagLogging = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.enableRagLogging else live.enableRagLogging,
                            displaySetting = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.displaySetting else live.displaySetting,
                            enableWebSearch = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.enableWebSearch else live.enableWebSearch,
                            favoriteModels = if (BackupContentOption.SETTINGS.name in selected || BackupContentOption.PROVIDERS.name in selected) cleanedSettings.favoriteModels else live.favoriteModels,
                            recentModelIds = if (BackupContentOption.SETTINGS.name in selected || BackupContentOption.PROVIDERS.name in selected) cleanedSettings.recentModelIds else live.recentModelIds,
                            chatModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.chatModelId else live.chatModelId,
                            titleModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.titleModelId else live.titleModelId,
                            titleThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.titleThinkingBudget else live.titleThinkingBudget,
                            summarizerModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.summarizerModelId else live.summarizerModelId,
                            summarizerThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.summarizerThinkingBudget else live.summarizerThinkingBudget,
                            subagentModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.subagentModelId else live.subagentModelId,
                            subagentThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.subagentThinkingBudget else live.subagentThinkingBudget,
                            imageGenerationModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.imageGenerationModelId else live.imageGenerationModelId,
                            titlePrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.titlePrompt else live.titlePrompt,
                            translateModeId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.translateModeId else live.translateModeId,
                            translatePrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.translatePrompt else live.translatePrompt,
                            suggestionModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.suggestionModelId else live.suggestionModelId,
                            suggestionThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.suggestionThinkingBudget else live.suggestionThinkingBudget,
                            suggestionPrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.suggestionPrompt else live.suggestionPrompt,
                            learningModePrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.learningModePrompt else live.learningModePrompt,
                            ocrModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.ocrModelId else live.ocrModelId,
                            ocrThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.ocrThinkingBudget else live.ocrThinkingBudget,
                            ocrPrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.ocrPrompt else live.ocrPrompt,
                            embeddingModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.embeddingModelId else live.embeddingModelId,
                            selectedTTSProviderId = if (BackupContentOption.SETTINGS.name in selected || BackupContentOption.PROVIDERS.name in selected) cleanedSettings.selectedTTSProviderId else live.selectedTTSProviderId,
                            selectedTTSVoiceId = if (BackupContentOption.SETTINGS.name in selected || BackupContentOption.PROVIDERS.name in selected) cleanedSettings.selectedTTSVoiceId else live.selectedTTSVoiceId,
                            ttsAutoplayMode = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.ttsAutoplayMode else live.ttsAutoplayMode,
                            sttModelId = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.sttModelId else live.sttModelId,
                            sttThinkingBudget = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.sttThinkingBudget else live.sttThinkingBudget,
                            sttPrompt = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.sttPrompt else live.sttPrompt,
                            webServerEnabled = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.webServerEnabled else live.webServerEnabled,
                            webServerPort = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.webServerPort else live.webServerPort,
                            webServerJwtEnabled = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.webServerJwtEnabled else live.webServerJwtEnabled,
                            webServerAccessPassword = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.webServerAccessPassword else live.webServerAccessPassword,
                            webServerBackgroundSetupShown = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.webServerBackgroundSetupShown else live.webServerBackgroundSetupShown,
                            chatStorage = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.chatStorage else live.chatStorage,
                            textSelectionConfig = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.textSelectionConfig else live.textSelectionConfig,
                            assistantOverlayConfig = if (BackupContentOption.SETTINGS.name in selected) cleanedSettings.assistantOverlayConfig else live.assistantOverlayConfig,
                        )
                    } else {
                        cleanedSettings
                    }
                    settingsStore.update(finalSettings)
                    LogUtil.i(
                        TAG,
                        "restoreFromBackupFile: Settings restored before file commit",
                    )
                }

                // Files last: never wipe live dirs until DB/settings have committed, and
                // skip dirs that were listed in the manifest but missing from the zip.
                val directoriesToRestore = resolveManagedDirsToRestore(
                    manifest = manifest,
                    stagedManagedDirs = stagedManagedDirs,
                    stagedFilesDir = stagedFilesDir,
                )
                restoreManagedFileDirectories(
                    liveFilesDir = context.filesDir,
                    stagedFilesDir = stagedFilesDir,
                    directoriesToRestore = directoriesToRestore,
                    liveBackupDir = File(restoreTempDir, "live_files_bak"),
                )

                LogUtil.i(TAG, "restoreFromBackupFile: Restore completed successfully")

                val totalCleanupResult = settingsCleanupResult.copy(
                    unsupportedZipEntriesBytes = unsupportedZipEntriesBytes,
                )
                RestoreResult(
                    sanitization = sanitizationResult,
                    settingsCleanup = totalCleanupResult,
                    workspacesMetadata = manifest?.workspacesMetadata ?: emptyList(),
                )
            } finally {
                restoreTempDir.deleteRecursively()
            }
        }

    private fun checkpointDatabase() {
        runCatching {
            appDatabase.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        }.onFailure { error ->
            LogUtil.w(TAG, "prepareBackupFile: WAL checkpoint failed", error)
        }
    }

    private fun addDatabaseEntries(zipOut: ZipOutputStream) {
        val dbFile = context.getDatabasePath(BackupArchiveFormat.DB_ENTRY)
        if (dbFile.exists()) {
            addFileToZip(zipOut, dbFile, BackupArchiveFormat.LEGACY_DB_ENTRY)
        }

        val walFile = File(dbFile.parentFile, BackupArchiveFormat.WAL_ENTRY)
        if (walFile.exists()) {
            addFileToZip(zipOut, walFile, BackupArchiveFormat.WAL_ENTRY)
        }

        val shmFile = File(dbFile.parentFile, BackupArchiveFormat.SHM_ENTRY)
        if (shmFile.exists()) {
            addFileToZip(zipOut, shmFile, BackupArchiveFormat.SHM_ENTRY)
        }
    }

    private fun addManagedFileEntries(zipOut: ZipOutputStream, dirs: List<String> = BackupArchiveFormat.MANAGED_FILE_DIRS) {
        dirs.forEach { dirName ->
            val directory = File(context.filesDir, dirName)
            val skip: (String, Boolean) -> Boolean =
                if (dirName == BackupArchiveFormat.WORKSPACES_DIR) {
                    { relative, isDir -> BackupArchiveFormat.isExcludedWorkspacePath(relative, isDir) }
                } else {
                    { _, _ -> false }
                }
            enumerateDirectoryEntries(directory, dirName, skip).forEach { entry ->
                try {
                    if (entry.isDirectory) {
                        addDirectoryToZip(zipOut, entry.entryName)
                    } else {
                        addFileToZip(zipOut, entry.source, entry.entryName)
                    }
                } catch (e: Exception) {
                    LogUtil.w(TAG, "addManagedFileEntries: Failed to zip entry ${entry.entryName} from source ${entry.source.absolutePath}", e)
                }
            }
        }
    }

    private fun addFilteredDatabaseEntries(zipOut: ZipOutputStream, filteredDbFile: File) {
        if (filteredDbFile.exists()) {
            addFileToZip(zipOut, filteredDbFile, BackupArchiveFormat.LEGACY_DB_ENTRY)
        }
        val walFile = File(filteredDbFile.parentFile, filteredDbFile.name + "-wal")
        if (walFile.exists()) {
            addFileToZip(zipOut, walFile, BackupArchiveFormat.WAL_ENTRY)
        }
        val shmFile = File(filteredDbFile.parentFile, filteredDbFile.name + "-shm")
        if (shmFile.exists()) {
            addFileToZip(zipOut, shmFile, BackupArchiveFormat.SHM_ENTRY)
        }
    }


    private fun stageDatabaseEntry(
        zipIn: ZipInputStream,
        stagedDbDir: File,
        entryName: String,
    ) {
        val targetName = when (entryName) {
            BackupArchiveFormat.LEGACY_DB_ENTRY, BackupArchiveFormat.DB_ENTRY -> BackupArchiveFormat.DB_ENTRY
            BackupArchiveFormat.WAL_ENTRY -> BackupArchiveFormat.WAL_ENTRY
            BackupArchiveFormat.SHM_ENTRY -> BackupArchiveFormat.SHM_ENTRY
            else -> error("Unsupported database entry: $entryName")
        }
        val targetFile = File(stagedDbDir, targetName)
        targetFile.parentFile?.mkdirs()
        targetFile.sink().buffer().outputStream().use { outputStream ->
            zipIn.copyTo(outputStream)
        }
    }

    private fun stageManagedFileEntry(
        zipIn: ZipInputStream,
        stagedFilesDir: File,
        entryName: String,
        isDirectory: Boolean,
    ) {
        val targetFile = safeZipDestination(stagedFilesDir, entryName)
        if (isDirectory || entryName.endsWith('/')) {
            targetFile.mkdirs()
            return
        }

        targetFile.parentFile?.mkdirs()
        targetFile.sink().buffer().outputStream().use { outputStream ->
            zipIn.copyTo(outputStream)
        }
    }

    private fun restorePortableSharedPreferences(
        stagedSnapshots: Map<String, SharedPreferencesSnapshot>,
        manifest: BackupManifest?,
    ) {
        val prefStoresToRestore = if (manifest?.formatVersion == BackupArchiveFormat.CURRENT_FORMAT_VERSION) {
            manifest.sharedPrefsStores
                .filter { BackupArchiveFormat.PORTABLE_SHARED_PREF_STORES.contains(it) }
                .distinct()
        } else {
            stagedSnapshots.keys
                .filter { BackupArchiveFormat.PORTABLE_SHARED_PREF_STORES.contains(it) }
                .sorted()
        }

        prefStoresToRestore.forEach { storeName ->
            val prefs = context.applicationContext.getSharedPreferences(storeName, Context.MODE_PRIVATE)
            val snapshot = stagedSnapshots[storeName]
            if (snapshot != null) {
                restoreSharedPreferencesSnapshot(prefs, snapshot)
            }
        }
    }

    private fun restoreLiveDatabase(stagedDbFile: File, manifest: BackupManifest? = null): DatabaseSanitizer.SanitizationResult {
        val (cleanDb, result) = DatabaseSanitizer.sanitize(context, stagedDbFile)

        // If restoring a selective backup (where only a subset of tables were exported),
        // copy any unselected live tables into cleanDb so the user's unselected live data
        // (e.g. workspaces or chats) is NOT wiped when cleanDb replaces the live DB.
        if (manifest != null && manifest.selectedContent.isNotEmpty()) {
            val tablesInBackup = buildSet {
                if (BackupContentOption.CHATS.name in manifest.selectedContent) addAll(DatabaseSanitizer.CHAT_TABLES)
                if (BackupContentOption.WORKSPACES.name in manifest.selectedContent) addAll(DatabaseSanitizer.WORKSPACE_TABLES)
            }
            val liveDbFile = context.getDatabasePath(BackupArchiveFormat.DB_ENTRY)
            if (liveDbFile.exists()) {
                val tablesToPreserve = DatabaseSanitizer.PORTABLE_TABLES.filter { it !in tablesInBackup }
                if (tablesToPreserve.isNotEmpty()) {
                    runCatching {
                        DatabaseSanitizer.copyTablesBetweenDatabases(
                            sourceDbFile = liveDbFile,
                            targetDbFile = cleanDb,
                            tables = tablesToPreserve,
                        )
                    }.onFailure { e ->
                        LogUtil.w(TAG, "Failed to preserve unselected tables during partial restore", e)
                    }
                }
            }
        }

        appDatabase.close()
        context.deleteDatabase(BackupArchiveFormat.DB_ENTRY)

        val finalDbFile = context.getDatabasePath(BackupArchiveFormat.DB_ENTRY)
        finalDbFile.parentFile?.mkdirs()
        cleanDb.copyTo(finalDbFile, overwrite = true)

        val cleanWal = File(cleanDb.path + "-wal")
        val cleanShm = File(cleanDb.path + "-shm")
        val finalWal = File(finalDbFile.path + "-wal")
        val finalShm = File(finalDbFile.path + "-shm")

        if (cleanWal.exists()) {
            cleanWal.copyTo(finalWal, overwrite = true)
        } else {
            finalWal.delete()
        }

        if (cleanShm.exists()) {
            cleanShm.copyTo(finalShm, overwrite = true)
        } else {
            finalShm.delete()
        }

        context.deleteDatabase("rikka_hub_sanitized")
        return result
    }

    private fun restoreDatabase(stagedDbFile: File, manifest: BackupManifest? = null): DatabaseSanitizer.SanitizationResult {
        return restoreLiveDatabase(stagedDbFile, manifest)
    }

    /**
     * The backup zip excludes each workspace's Linux rootfs, so after a restore
     * any workspace row still marked READY/INSTALLING would be a lie. Reset those
     * to DISABLED when the rootfs is absent so the UI offers a fresh install.
     */
    private suspend fun resetWorkspaceShellStatesForMissingRootfs() {
        val workspaces = runCatching { appDatabase.workspaceDao().getAll() }
            .getOrDefault(emptyList())
        if (workspaces.isEmpty()) return
        val workspacesRoot = File(context.filesDir, BackupArchiveFormat.WORKSPACES_DIR)
        val now = System.currentTimeMillis()
        for (ws in workspaces) {
            val status = ws.shellStatus
            if (status != me.lucid.workspace.WorkspaceShellStatus.READY.name &&
                status != me.lucid.workspace.WorkspaceShellStatus.INSTALLING.name
            ) {
                continue
            }
            val linuxDir = File(File(workspacesRoot, ws.root), "linux")
            val present = runCatching { linuxDir.hasUsableRootfs() }.getOrDefault(false)
            if (!present) {
                runCatching {
                    appDatabase.workspaceDao().updateShellStatus(
                        ws.id,
                        me.lucid.workspace.WorkspaceShellStatus.DISABLED.name,
                        now,
                    )
                }.onFailure { error ->
                    LogUtil.w(TAG, "resetWorkspaceShellStates: failed for ${ws.id}: ${error.message}")
                }
            }
        }
    }
}

private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
    file.source().buffer().inputStream().use { inputStream ->
        val zipEntry = ZipEntry(entryName)
        zipOut.putNextEntry(zipEntry)
        inputStream.copyTo(zipOut)
        zipOut.closeEntry()
        LogUtil.d(TAG, "addFileToZip: Added $entryName (${file.length()} bytes) to zip")
    }
}

private fun addDirectoryToZip(zipOut: ZipOutputStream, entryName: String) {
    val normalizedName = if (entryName.endsWith('/')) entryName else "$entryName/"
    val zipEntry = ZipEntry(normalizedName)
    zipOut.putNextEntry(zipEntry)
    zipOut.closeEntry()
}

private fun addVirtualFileToZip(zipOut: ZipOutputStream, name: String, content: String) {
    val zipEntry = ZipEntry(name)
    zipOut.putNextEntry(zipEntry)
    zipOut.write(content.toByteArray())
    zipOut.closeEntry()
    LogUtil.i(TAG, "addVirtualFileToZip: $name (${content.length} bytes)")
}

private suspend fun DavCollection.ensureCollectionExists() = withContext(Dispatchers.IO) {
    try {
        propfind(depth = 0) { response, relation ->
            LogUtil.i(TAG, "ensureCollectionExists: $response $relation")
        }
    } catch (e: NotFoundException) {
        LogUtil.i(TAG, "ensureCollectionExists: ${this@ensureCollectionExists.location}")
        mkCol(null) { response ->
            LogUtil.i(TAG, "ensureCollectionExists: $response")
        }
    }
}

data class WebDavBackupItem(
    val href: String,
    val displayName: String,
    val size: Long,
    val lastModified: Instant,
)
