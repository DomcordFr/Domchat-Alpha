package me.lucid.domchat.data.sync

import java.io.File

/**
 * Helpers for restoring managed file directories from a staged backup tree.
 *
 * Live directories are never wiped unless a staged copy actually exists. Swap uses a
 * backup-then-replace sequence so a mid-restore failure can roll live dirs back.
 */
internal fun resolveManagedDirsToRestore(
    manifest: BackupManifest?,
    stagedManagedDirs: Set<String>,
    stagedFilesDir: File,
): List<String> {
    val candidates = if (manifest?.formatVersion == BackupArchiveFormat.CURRENT_FORMAT_VERSION &&
        manifest.includesFiles
    ) {
        manifest.managedFileDirs
            .filter { BackupArchiveFormat.MANAGED_FILE_DIRS.contains(it) }
            .distinct()
    } else {
        stagedManagedDirs
            .filter { BackupArchiveFormat.MANAGED_FILE_DIRS.contains(it) }
            .distinct()
            .sorted()
    }
    // S14: do not wipe a live dir when the archive had no staged copy of it.
    return candidates.filter { dirName -> File(stagedFilesDir, dirName).exists() }
}

internal fun restoreManagedFileDirectories(
    liveFilesDir: File,
    stagedFilesDir: File,
    directoriesToRestore: List<String>,
    liveBackupDir: File,
) {
    if (directoriesToRestore.isEmpty()) {
        return
    }
    liveBackupDir.mkdirs()
    val replaced = mutableListOf<String>()
    try {
        directoriesToRestore.forEach { dirName ->
            val liveDir = File(liveFilesDir, dirName)
            val stagedDir = File(stagedFilesDir, dirName)
            if (!stagedDir.exists()) {
                return@forEach
            }
            if (dirName == BackupArchiveFormat.WORKSPACES_DIR) {
                restoreWorkspacesDir(stagedDir, liveDir, File(liveBackupDir, dirName))
                replaced += dirName
                return@forEach
            }
            backupLiveDirectory(liveDir, File(liveBackupDir, dirName))
            replaced += dirName
            if (liveDir.exists()) {
                liveDir.deleteRecursively()
            }
            liveDir.mkdirs()
            mirrorDirectory(stagedDir, liveDir)
        }
        liveBackupDir.deleteRecursively()
    } catch (error: Exception) {
        replaced.asReversed().forEach { dirName ->
            val liveDir = File(liveFilesDir, dirName)
            val backupDir = File(liveBackupDir, dirName)
            if (liveDir.exists()) {
                liveDir.deleteRecursively()
            }
            if (backupDir.exists()) {
                liveDir.parentFile?.mkdirs()
                if (!backupDir.renameTo(liveDir)) {
                    liveDir.mkdirs()
                    mirrorDirectory(backupDir, liveDir)
                    backupDir.deleteRecursively()
                }
            }
        }
        throw error
    }
}

/**
 * Restores the workspaces directory without disturbing the on-device Linux rootfs.
 *
 * Backups intentionally omit each workspace's reinstallable `linux/` rootfs (and `tmp/`
 * scratch space), so a plain delete-and-mirror would destroy a perfectly good rootfs and
 * force a lengthy reinstall on every restore. Instead we replace only the backed-up
 * subtrees (e.g. `files/`) per workspace and leave the excluded subdirs untouched.
 */
internal fun restoreWorkspacesDir(
    stagedDir: File,
    liveDir: File,
    workspacesBackupDir: File? = null,
) {
    liveDir.mkdirs()
    if (!stagedDir.exists()) return

    val backedUpEntries = mutableListOf<Pair<File, File>>() // (liveFile, backupFile)
    try {
        stagedDir.listFiles()?.forEach { stagedChild ->
            val liveChild = File(liveDir, stagedChild.name)
            if (stagedChild.isDirectory) {
                liveChild.mkdirs()
                stagedChild.listFiles()?.forEach { stagedSub ->
                    if (stagedSub.name in BackupArchiveFormat.WORKSPACE_EXCLUDED_SUBDIRS) {
                        return@forEach
                    }
                    val liveSub = File(liveChild, stagedSub.name)
                    if (liveSub.exists() && workspacesBackupDir != null) {
                        val relPath = liveSub.relativeTo(liveDir).path
                        val backupFile = File(workspacesBackupDir, relPath)
                        backupFile.parentFile?.mkdirs()
                        if (liveSub.isDirectory) {
                            backupFile.mkdirs()
                            mirrorDirectory(liveSub, backupFile)
                        } else {
                            liveSub.copyTo(backupFile, overwrite = true)
                        }
                        backedUpEntries.add(liveSub to backupFile)
                    }
                    if (liveSub.exists()) {
                        liveSub.deleteRecursively()
                    }
                    if (stagedSub.isDirectory) {
                        liveSub.mkdirs()
                        mirrorDirectory(stagedSub, liveSub)
                    } else {
                        liveSub.parentFile?.mkdirs()
                        stagedSub.copyTo(liveSub, overwrite = true)
                    }
                }
            } else {
                if (liveChild.exists() && workspacesBackupDir != null) {
                    val relPath = liveChild.relativeTo(liveDir).path
                    val backupFile = File(workspacesBackupDir, relPath)
                    backupFile.parentFile?.mkdirs()
                    liveChild.copyTo(backupFile, overwrite = true)
                    backedUpEntries.add(liveChild to backupFile)
                }
                if (liveChild.exists()) {
                    liveChild.deleteRecursively()
                }
                liveChild.parentFile?.mkdirs()
                stagedChild.copyTo(liveChild, overwrite = true)
            }
        }
    } catch (error: Exception) {
        backedUpEntries.asReversed().forEach { (liveTarget, backupSource) ->
            if (liveTarget.exists()) {
                liveTarget.deleteRecursively()
            }
            if (backupSource.exists()) {
                liveTarget.parentFile?.mkdirs()
                if (backupSource.isDirectory) {
                    liveTarget.mkdirs()
                    mirrorDirectory(backupSource, liveTarget)
                } else {
                    backupSource.copyTo(liveTarget, overwrite = true)
                }
            }
        }
        throw error
    }
}

internal fun mirrorDirectory(sourceDir: File, targetDir: File) {
    sourceDir.walkTopDown().forEach { source ->
        val relative = source.relativeTo(sourceDir)
        val target = File(targetDir, relative.path)
        if (source.isDirectory) {
            target.mkdirs()
        } else {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
        }
    }
}

private fun backupLiveDirectory(liveDir: File, backupDir: File) {
    if (!liveDir.exists()) {
        return
    }
    if (backupDir.exists()) {
        backupDir.deleteRecursively()
    }
    backupDir.parentFile?.mkdirs()
    if (!liveDir.renameTo(backupDir)) {
        backupDir.mkdirs()
        mirrorDirectory(liveDir, backupDir)
    }
}
