package com.peanut.ledger.data

import android.content.Context
import android.net.Uri
import com.peanut.ledger.BuildConfig
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class BackupReason(val key: String, val label: String) {
    AUTO("auto", "自动备份"),
    MANUAL("manual", "手动备份"),
    BEFORE_RESTORE("before_restore", "恢复前安全备份");

    companion object {
        fun fromKey(key: String?): BackupReason =
            entries.firstOrNull { it.key == key } ?: AUTO
    }
}

data class BackupInfo(
    val file: File,
    val createdAt: Long,
    val reason: BackupReason,
    val appVersionName: String,
    val appVersionCode: Int,
    val schemaVersion: Int,
    val formatVersion: Int,
    val sizeBytes: Long,
    val issue: String? = null
) {
    val blocked: Boolean get() = issue != null

    val versionLabel: String
        get() = if (appVersionCode > 0 && appVersionName.isNotBlank()) {
            "v$appVersionName"
        } else {
            "旧版备份"
        }

    val timeLabel: String
        get() = Instant.ofEpochMilli(createdAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy/M/d HH:mm", Locale.US))

    val sizeLabel: String
        get() = when {
            sizeBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", sizeBytes / 1024.0 / 1024.0)
            sizeBytes >= 1024 -> String.format(Locale.US, "%.0f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

sealed interface RestoreCheck {
    data class Ready(
        val info: BackupInfo,
        val data: LedgerData,
        val migrated: Boolean
    ) : RestoreCheck

    data class Invalid(val message: String) : RestoreCheck

    data class Blocked(val message: String, val info: BackupInfo?) : RestoreCheck
}

/**
 * Versioned local backups.
 *
 * Backup files are JSON with a small metadata envelope around the exact
 * repository payload. The envelope carries the APP version, schema version and
 * format version so a restore can be checked before any local data is touched.
 */
class BackupManager(private val context: Context) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val CURRENT_SCHEMA_VERSION = 1
        const val DEFAULT_FREQUENCY_DAYS = 1
        const val DEFAULT_RETENTION_DAYS = 30
        const val FREQUENCY_OFF = 0
        private const val FORMAT_NAME = "peanut-ledger-backup"
        private const val PREFS = "peanut_backup_prefs"
        private const val KEY_FREQUENCY = "frequency_days"
        private const val KEY_RETENTION = "retention_days"
        private const val KEY_LAST_SUCCESS = "last_success_at"
        private const val DAY_MS = 24L * 60L * 60L * 1000L

        fun currentAppVersionName(): String = BuildConfig.VERSION_NAME
        fun currentAppVersionCode(): Int = BuildConfig.VERSION_CODE
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val backupDir: File =
        (context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups")).apply {
            if (!exists()) mkdirs()
        }

    var frequencyDays: Int
        get() = prefs.getInt(KEY_FREQUENCY, DEFAULT_FREQUENCY_DAYS).coerceIn(0, 365)
        set(value) {
            prefs.edit().putInt(KEY_FREQUENCY, value.coerceIn(0, 365)).apply()
        }

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION, DEFAULT_RETENTION_DAYS).coerceIn(1, 3650)
        set(value) {
            prefs.edit().putInt(KEY_RETENTION, value.coerceIn(1, 3650)).apply()
            enforceRetention()
        }

    val lastSuccessAt: Long
        get() = prefs.getLong(KEY_LAST_SUCCESS, 0L)

    fun listBackups(): List<BackupInfo> {
        enforceRetention()
        return backupDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            ?.mapNotNull { file -> readInfo(file) }
            ?.sortedByDescending { it.createdAt }
            .orEmpty()
    }

    fun createBackup(data: LedgerData, reason: BackupReason): BackupInfo? = synchronized(this) {
        runCatching {
            if (!backupDir.exists()) backupDir.mkdirs()
            val createdAt = System.currentTimeMillis()
            val root = JSONObject()
                .put("format", FORMAT_NAME)
                .put("formatVersion", CURRENT_FORMAT_VERSION)
                .put("schemaVersion", CURRENT_SCHEMA_VERSION)
                .put("appVersionName", currentAppVersionName())
                .put("appVersionCode", currentAppVersionCode())
                .put("createdAt", createdAt)
                .put("reason", reason.key)
                .put("data", LedgerRepository.encode(data))
            val stamp = Instant.ofEpochMilli(createdAt)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US))
            var file = File(
                backupDir,
                "peanut-$stamp-${reason.key}-v${currentAppVersionCode()}.json"
            )
            var suffix = 1
            while (file.exists()) {
                file = File(
                    backupDir,
                    "peanut-$stamp-${reason.key}-v${currentAppVersionCode()}-$suffix.json"
                )
                suffix++
            }
            val temp = File(backupDir, "${file.name}.tmp")
            temp.writeText(root.toString(2), Charsets.UTF_8)
            if (!temp.renameTo(file)) {
                file.writeText(root.toString(2), Charsets.UTF_8)
                temp.delete()
            }
            prefs.edit().putLong(KEY_LAST_SUCCESS, createdAt).apply()
            enforceRetention()
            readInfo(file)
        }.getOrNull()
    }

    /**
     * Called on APP start and when the APP goes to the background. A backup is
     * made at most once per configured calendar-day interval. Backgrounding is
     * the safest moment because edits are already persisted and the user is done.
     */
    fun maybeAutoBackup(data: LedgerData): BackupInfo? = synchronized(this) {
        val frequency = frequencyDays
        if (frequency <= 0) return null
        val today = LocalDate.now()
        val lastDate = if (lastSuccessAt > 0L) {
            Instant.ofEpochMilli(lastSuccessAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            listBackups().firstOrNull()?.let {
                Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        if (lastDate != null) {
            val elapsed = today.toEpochDay() - lastDate.toEpochDay()
            if (elapsed >= 0 && elapsed < frequency) return null
            // A clock rollback should not create a backup storm.
            if (elapsed < 0) return null
        }
        createBackup(data, BackupReason.AUTO)
    }

    fun copyBackupTo(file: File, uri: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        } != null
    }.getOrDefault(false)

    fun importBackup(uri: Uri): File? {
        if (!backupDir.exists()) backupDir.mkdirs()
        var file = File(backupDir, "peanut-import-${System.currentTimeMillis()}.json")
        var suffix = 1
        while (file.exists()) {
            file = File(backupDir, "peanut-import-${System.currentTimeMillis()}-$suffix.json")
            suffix++
        }
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            file.delete()
            return null
        }
        return file
    }

    fun deleteBackup(file: File): Boolean = synchronized(this) {
        val deleted = file.isFile && file.delete()
        if (deleted) {
            backupRevisionCleanup()
        }
        deleted
    }

    fun deleteAllBackups(): Int = synchronized(this) {
        var count = 0
        backupDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension.equals("json", ignoreCase = true) && file.delete()) {
                count++
            }
        }
        prefs.edit().remove(KEY_LAST_SUCCESS).apply()
        count
    }

    private fun backupRevisionCleanup() {
        val hasBackups = backupDir.listFiles()?.any {
            it.isFile && it.extension.equals("json", ignoreCase = true)
        } == true
        if (!hasBackups) {
            prefs.edit().remove(KEY_LAST_SUCCESS).apply()
        }
    }

    fun checkRestore(file: File): RestoreCheck {
        if (!file.isFile) return RestoreCheck.Invalid("备份文件不存在")
        val root = runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrElse {
            return RestoreCheck.Invalid("备份文件无法解析，可能已损坏")
        }
        if (root.optString("format") != FORMAT_NAME) {
            return RestoreCheck.Invalid("这不是花生账本备份文件")
        }
        val formatVersion = root.optInt("formatVersion", 1)
        val schemaVersion = root.optInt("schemaVersion", 1)
        val appVersionCode = root.optInt("appVersionCode", 0)
        val appVersionName = root.optString("appVersionName", "").ifBlank { "未知版本" }
        val info = readInfo(file, root)
            ?: return RestoreCheck.Invalid("备份文件无法读取")
        if (formatVersion > CURRENT_FORMAT_VERSION) {
            return RestoreCheck.Blocked(
                "备份格式版本为 v$formatVersion，当前 APP 仅支持 v$CURRENT_FORMAT_VERSION。\n\n" +
                    "备份数据版本：v$appVersionName（code $appVersionCode）\n" +
                    "当前 APP 版本：v${currentAppVersionName()}（code ${currentAppVersionCode()}）",
                info
            )
        }
        if (appVersionCode > currentAppVersionCode()) {
            return RestoreCheck.Blocked(
                "备份数据版本：v$appVersionName（code $appVersionCode）\n" +
                    "当前 APP 版本：v${currentAppVersionName()}（code ${currentAppVersionCode()}）\n\n" +
                    "该备份由更新版本的 APP 创建，当前版本无法还原。请先升级 APP 后再试。",
                info
            )
        }
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            return RestoreCheck.Blocked(
                "备份数据结构版本为 v$schemaVersion，当前 APP 仅支持 v$CURRENT_SCHEMA_VERSION。\n\n" +
                    "备份数据版本：v$appVersionName（code $appVersionCode）\n" +
                    "当前 APP 版本：v${currentAppVersionName()}（code ${currentAppVersionCode()}）",
                info
            )
        }
        val dataRoot = root.optJSONObject("data")
            ?: if (root.has("days") || root.has("settings")) root else null
            ?: return RestoreCheck.Invalid("备份文件缺少账本数据")
        val data = runCatching {
            LedgerRepository.decode(migrate(dataRoot, schemaVersion))
        }.getOrElse {
            return RestoreCheck.Invalid("备份数据读取失败，可能已损坏")
        }
        return RestoreCheck.Ready(
            info = info,
            data = data,
            migrated = schemaVersion < CURRENT_SCHEMA_VERSION || appVersionCode < currentAppVersionCode()
        )
    }

    private fun readInfo(file: File, root: JSONObject? = null): BackupInfo? {
        val json = root ?: runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() ?: return null
        val formatVersion = json.optInt("formatVersion", 1)
        val schemaVersion = json.optInt("schemaVersion", 1)
        val appVersionCode = json.optInt("appVersionCode", 0)
        val appVersionName = json.optString("appVersionName", "").ifBlank { "未知版本" }
        val issue = when {
            formatVersion > CURRENT_FORMAT_VERSION -> "备份格式版本过高，无法还原"
            appVersionCode > currentAppVersionCode() -> "备份数据版本高于当前 APP"
            schemaVersion > CURRENT_SCHEMA_VERSION -> "备份数据结构版本过高，无法还原"
            else -> null
        }
        return BackupInfo(
            file = file,
            createdAt = json.optLong("createdAt", file.lastModified()),
            reason = BackupReason.fromKey(json.optString("reason", "")),
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            schemaVersion = schemaVersion,
            formatVersion = formatVersion,
            sizeBytes = file.length(),
            issue = issue
        )
    }

    /** Place for future schema migrations; old payloads are parsed with defaults. */
    private fun migrate(root: JSONObject, fromSchema: Int): JSONObject {
        var current = root
        // v0/unknown payloads are already handled by LedgerRepository's optional fields.
        if (fromSchema <= 0) {
            current = root
        }
        return current
    }

    private fun enforceRetention() {
        val keepDays = retentionDays.coerceAtLeast(1)
        val cutoff = System.currentTimeMillis() - keepDays * DAY_MS
        backupDir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            if (file.name.endsWith(".tmp")) {
                file.delete()
                return@forEach
            }
            val createdAt = runCatching {
                JSONObject(file.readText(Charsets.UTF_8)).optLong("createdAt", file.lastModified())
            }.getOrDefault(file.lastModified())
            if (createdAt < cutoff) file.delete()
        }
    }
}
