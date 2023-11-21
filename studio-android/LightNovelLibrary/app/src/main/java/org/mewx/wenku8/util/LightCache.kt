package org.mewx.wenku8.util

import android.content.ContentUris

/**
 * Light Cache
 * *
 * This class provide straight file operation functions.
 * Easy save file, read file and delete file.
 */
object LightCache {
    private val TAG: String? = LightCache::class.java.getSimpleName()

    /**
     * Test whether file exists
     *
     * @param path the full file path
     * @return true if file exist and not empty;
     * otherwise false, and if the file exists but it's empty, it will get removed
     */
    fun testFileExist(path: String?): Boolean {
        return testFileExist(path, false)
    }

    fun testFileExist(path: String?, allowEmptyFile: Boolean): Boolean {
        val file = File(path)
        if (file.exists()) {
            if (!allowEmptyFile && file.length() === 0) deleteFile(path) // delete empty file and return false
            else return true
        }
        return false
    }

    /**
     * load file content
     *
     * @param path full file path (can be relative)
     * @return null if the file does not exist; otherwise the file content string, can be empty
     */
    fun loadFile(path: String?): ByteArray? {
        // if file not exist, then return null
        val file = File(path)
        if (file.exists() && file.isFile()) {
            // load existing file
            try {
                return loadStream(FileInputStream(file))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun loadStream(inputStream: InputStream?): ByteArray? {
        try {
            // Hopefully to get the file size.
            val fileSize: Int = inputStream.available()
            val dis = DataInputStream(inputStream)

            // read all
            val bs = ByteArray(fileSize)
            if (dis.read(bs, 0, fileSize) === -1) return null
            dis.close()
            inputStream.close()
            return bs
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun saveFile(path: String?, fileName: String?, bs: ByteArray?, forceUpdate: Boolean): Boolean {
        val fullPath = path.toString() + (if (path.charAt(path.length() - 1) !== File.separatorChar) File.separator else "") + fileName
        return saveFile(fullPath, bs, forceUpdate)
    }

    fun saveFile(filepath: String?, bs: ByteArray?, forceUpdate: Boolean): Boolean {
        // create parent folder first when applicable
        val file = File(filepath)
        if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) Log.d(TAG, "Failed to create dir: $filepath")

        // if forceUpdate == true then update the file
        Log.d(TAG, "Path: $filepath")
        if (!file.exists() || forceUpdate) {
            if (file.exists() && !file.isFile()) {
                Log.d(TAG, "Failed to write, which may caused by file is not a file")
                return false // is not a file
            }
            try {
                // create file
                if (!file.createNewFile()) Log.d(TAG, "File existed or failed to create file: $filepath")
                val out = FileOutputStream(file) // truncate
                val dos = DataOutputStream(out)

                // write all
                dos.write(bs)
                dos.close()
                out.close()
                Log.d(TAG, "Write successfully")
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }
        return true // say it successful
    }

    fun deleteFile(path: String?, fileName: String?): Boolean {
        val fullPath = path.toString() + (if (path.charAt(path.length() - 1) !== File.separatorChar) File.separator else "") + fileName
        return deleteFile(fullPath)
    }

    fun deleteFile(filepath: String?): Boolean {
        Log.d(TAG, "Deleting: $filepath")
        return File(filepath).delete()
    }

    /**
     * Copy file from one place to another place,
     * if target parent path does not exist, then create them
     *
     * @param from       full path
     * @param to         full path
     * @param forceWrite true if wanting to override
     */
    fun copyFile(from: String?, to: String?, forceWrite: Boolean?) {
        val fromFile = File(from)
        if (!fromFile.exists() || !fromFile.isFile() || !fromFile.canRead()) {
            return
        }
        try {
            val fosFrom: java.io.FileInputStream = FileInputStream(fromFile)
            copyFile(fosFrom, to, forceWrite)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun copyFile(from: InputStream?, to: String?, forceWrite: Boolean?) {
        val toFile = File(to)
        if (toFile.exists() && !forceWrite) return
        if (!toFile.getParentFile().exists() && !toFile.getParentFile().mkdirs()) Log.d(TAG, "Failed to create parent dirs for target file: $to")
        if (toFile.exists() && forceWrite && !toFile.delete()) Log.d(TAG, "Failed to create or delete target file: $to")
        try {
            val fosTo: java.io.FileOutputStream = FileOutputStream(toFile)
            val bt = ByteArray(1024)
            var c: Int
            while (from.read(bt).also { c = it } > 0) fosTo.write(bt, 0, c)
            from.close()
            fosTo.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    // Copied from https://stackoverflow.com/a/36714242/4206925
    fun getFilePath(context: Context?, uri: Uri?): String? {
        var uri: Uri? = uri
        var selection: String? = null
        var selectionArgs: Array<String?>? = null
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> = docId.split(":")
                return Environment.getExternalStorageDirectory() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                val id: String = DocumentsContract.getDocumentId(uri)
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id))
            } else if (isMediaDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split: Array<String?> = docId.split(":")
                val type = split[0]
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                selection = "_id=?"
                selectionArgs = arrayOf(
                        split[1]
                )
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment()
            }
            val projection = arrayOf<String?>(
                    MediaStore.Images.Media.DATA
            )
            val cursor: Cursor
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null)
                val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath()
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri?): Boolean {
        return "com.android.externalstorage.documents".equals(uri.getAuthority())
    }

    fun isDownloadsDocument(uri: Uri?): Boolean {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority())
    }

    fun isMediaDocument(uri: Uri?): Boolean {
        return "com.android.providers.media.documents".equals(uri.getAuthority())
    }

    fun isGooglePhotosUri(uri: Uri?): Boolean {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority())
    }

    /**
     * Lists all files in the given directory recursively.
     * @param fullDirectoryPath the directory to look up
     * @return the list of absolute paths for all files inside
     */
    fun listAllFilesInDirectory(fullDirectoryPath: File?): List<Uri?>? {
        val paths: ArrayList<Uri?> = ArrayList()
        val directoryQueue: Queue<File?> = LinkedList()
        if (fullDirectoryPath.isDirectory()) {
            directoryQueue.add(fullDirectoryPath)
        }

        // BFS getting all file Uris.
        while (!directoryQueue.isEmpty()) {
            val currentDir: File = directoryQueue.remove()
            val fileList: Array<File?> = currentDir.listFiles() ?: continue
            for (file in fileList) {
                if (file.isDirectory()) {
                    directoryQueue.add(file)
                } else if (file.isFile()) {
                    paths.add(Uri.fromFile(file))
                }
            }
        }
        return paths
    }

    fun listAllFilesInDirectory(fullDirectoryPath: DocumentFile?): List<Uri?>? {
        val paths: ArrayList<Uri?> = ArrayList()
        val directoryQueue: Queue<DocumentFile?> = LinkedList()
        if (fullDirectoryPath.isDirectory()) {
            directoryQueue.add(fullDirectoryPath)
        }

        // BFS getting all file Uris.
        while (!directoryQueue.isEmpty()) {
            val currentDir: DocumentFile = directoryQueue.remove()
            for (file in currentDir.listFiles()) {
                if (file.isDirectory()) {
                    directoryQueue.add(file)
                } else if (file.isFile()) {
                    paths.add(file.getUri())
                }
            }
        }
        return paths
    }
}