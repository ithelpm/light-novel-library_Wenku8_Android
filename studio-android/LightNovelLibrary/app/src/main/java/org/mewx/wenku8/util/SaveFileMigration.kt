package org.mewx.wenku8.util

import android.annotation.TargetApi

/**
 * The utility class for migration save files from prior-API-30 to API 30+ (Android R) world.
 */
object SaveFileMigration {
    private val TAG: String? = SaveFileMigration::class.java.getSimpleName()
    private val SIGNAL_FILE_NAME: String? = ".migration_completed"

    // Cached paths.
    private var savedInternalPath: String? = null
    private var savedExternalPath: String? = null

    // This Uri is needed because constructing Uri just from a path is hard. The path looks like: /tree/primary:wenku8.
    private var overrideExternalPathUrl: Uri? = null
    fun markMigrationCompleted() {
        LightCache.saveFile(getInternalSavePath(), SIGNAL_FILE_NAME, "".getBytes(), false)
    }

    fun revertMigrationStatus() {
        LightCache.deleteFile(getInternalSavePath(), SIGNAL_FILE_NAME)
    }

    /**
     * Checks if the external storage contains the wenku8 directory.
     * @return true if eligible; otherwise false
     */
    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    fun migrationEligible(): Boolean {
        return LightCache.testFileExist((Environment.getExternalStorageDirectory() + File.separator).toString() + "wenku8" + File.separator, true)
    }

    fun migrationCompleted(): Boolean {
        val signalFileExists: Boolean = LightCache.testFileExist(getInternalSavePath() + SIGNAL_FILE_NAME, true)
        Log.d(TAG, "migrationCompleted: $signalFileExists")
        return signalFileExists
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    fun generateMigrationPlan(): List<Uri?>? {
        return if (overrideExternalPathUrl != null) {
            LightCache.listAllFilesInDirectory(DocumentFile.fromTreeUri(MyApp.getContext(), overrideExternalPathUrl))
        } else LightCache.listAllFilesInDirectory(File(getExternalStoragePath()))
    }

    /**
     * Given an external file path, copy the file to the internal storage.
     * Although this is slow (because we are not caching the paths), it's acceptable for one-off effort.
     *
     * @param externalFilePath the file Uri in external storage
     * @return the internal absolute file path to the copied file
     */
    @Throws(FileNotFoundException::class)
    fun migrateFile(externalFilePath: Uri?): String? {
        val internalFilePath: String = externalFilePath.getPath().replace(getExternalStoragePath(), getInternalSavePath())
        // The missing parent folders will also be created.
        if (overrideExternalPathUrl != null) {
            LightCache.copyFile(MyApp.getContext().getContentResolver().openInputStream(externalFilePath), internalFilePath, true)
        } else {
            LightCache.copyFile(externalFilePath.getPath(), internalFilePath, true)
        }
        return internalFilePath
    }

    fun getInternalSavePath(): String? {
        if (savedInternalPath == null) {
            savedInternalPath = MyApp.getContext().getFilesDir() + File.separator
        }
        return savedInternalPath
    }

    fun overrideExternalPath(uri: Uri?) {
        overrideExternalPathUrl = uri
    }

    fun getExternalStoragePath(): String? {
        if (overrideExternalPathUrl != null) {
            return overrideExternalPathUrl.getPath()
        }
        if (savedExternalPath == null) {
            savedExternalPath = (Environment.getExternalStorageDirectory() + File.separator).toString() + "wenku8" + File.separator
        }
        return savedExternalPath
    }
}