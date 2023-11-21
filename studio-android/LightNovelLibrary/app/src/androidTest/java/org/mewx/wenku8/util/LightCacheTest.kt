package org.mewx.wenku8.util

import org.junit.Assert.assertArrayEquals

@SmallTest
class LightCacheTest {
    @Before
    fun setUp() {
        val TEMP_FILE_PATH = "test/path"
        val TEMP_FILE_FULL_NAME_PATH = TEMP_FILE_PATH + File.separator + TEMP_FILE_NAME
        val instrumentationCtx: Context = InstrumentationRegistry.getInstrumentation().getTargetContext()
        BASE = instrumentationCtx.getFilesDir().getAbsolutePath() + File.separator
        BASE_TEMP_FILE_PATH = BASE + TEMP_FILE_PATH
        BASE_TEMP_FILE_NAME = BASE + TEMP_FILE_NAME
        BASE_TEMP_FILE_FULL_NAME_PATH = BASE + TEMP_FILE_FULL_NAME_PATH
    }

    @After
    fun cleanUp() {
        // reset test environment
        LightCache.deleteFile(BASE_TEMP_FILE_NAME) // single file
        LightCache.deleteFile(BASE_TEMP_FILE_FULL_NAME_PATH) // file with path
        LightCache.deleteFile(BASE_TEMP_FILE_PATH)
        LightCache.deleteFile(BASE.toString() + "test")
    }

    /**
     * when file exists, return true
     */
    @Test
    fun testFileExist() {
        assertFalse(LightCache.testFileExist(BASE_TEMP_FILE_NAME))

        // create file
        LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte()), false)
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_NAME))
    }

    /**
     * when file exists but the file is empty, return false
     */
    @Test
    fun testFileExistEmpty() {
        assertFalse(LightCache.testFileExist(BASE_TEMP_FILE_NAME))

        // create file
        LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf(), false)
        assertFalse(LightCache.testFileExist(BASE_TEMP_FILE_NAME))
    }

    @Test
    fun loadFileNoFile() {
        assertNull(LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun loadFileEmptyFile() {
        LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf(), false)
        assertArrayEquals(ByteArray(0), LightCache.loadFile(BASE_TEMP_FILE_NAME))
    }

    @Test
    fun loadFileNormalFile() {
        LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false)
        assertArrayEquals(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_NAME))
    }

    @Test
    fun saveFilePathAndFileName() {
        LightCache.saveFile(BASE_TEMP_FILE_PATH, TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false)
        assertArrayEquals(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun saveFileFullPath() {
        LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false)
        assertArrayEquals(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun saveFileNoUpdate() {
        LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false)
        LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), false)
        assertArrayEquals(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun saveFileForceUpdate() {
        LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false)
        LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), true)
        assertArrayEquals(byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    /**
     * Target file is actually is a folder
     */
    @Test
    fun saveFileExistingFolder() {
        assertTrue(File(BASE_TEMP_FILE_FULL_NAME_PATH).mkdirs()) // create as folder
        assertFalse(LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), true))
    }

    @Test
    fun deleteFileNoFile() {
        assertFalse(LightCache.deleteFile(BASE_TEMP_FILE_PATH, TEMP_FILE_NAME))
    }

    @Test
    fun deleteFileNormal() {
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false))
        assertTrue(LightCache.deleteFile(BASE_TEMP_FILE_PATH, TEMP_FILE_NAME))
    }

    @Test
    fun deleteFolder() {
        assertTrue(File(BASE_TEMP_FILE_FULL_NAME_PATH).mkdirs())
        assertTrue(LightCache.deleteFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun deleteFolderNotEmpty() {
        assertTrue(File(BASE_TEMP_FILE_FULL_NAME_PATH).mkdirs())
        assertFalse(LightCache.deleteFile(BASE_TEMP_FILE_PATH))
    }

    @Test
    fun copyFileNoSourceFile() {
        LightCache.copyFile(BASE_TEMP_FILE_NAME, BASE_TEMP_FILE_FULL_NAME_PATH, false)
        assertFalse(LightCache.testFileExist(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun copyFileNoTargetFileParentFolder() {
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false))
        LightCache.copyFile(BASE_TEMP_FILE_NAME, BASE_TEMP_FILE_FULL_NAME_PATH, false)
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_FULL_NAME_PATH))
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_NAME)) // original file still exists
    }

    @Test
    fun copyFileNormal() {
        assertTrue(File(BASE_TEMP_FILE_PATH).mkdirs())
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false))
        LightCache.copyFile(BASE_TEMP_FILE_NAME, BASE_TEMP_FILE_FULL_NAME_PATH, false)
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_FULL_NAME_PATH))
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_NAME)) // original file still exists
    }

    @Test
    fun copyFileExistingNoForce() {
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false))
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), false))
        LightCache.copyFile(BASE_TEMP_FILE_NAME, BASE_TEMP_FILE_FULL_NAME_PATH, false)
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_FULL_NAME_PATH))
        assertArrayEquals(byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun copyFileExistingForce() {
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_NAME, byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), false))
        assertTrue(LightCache.saveFile(BASE_TEMP_FILE_FULL_NAME_PATH, byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()), false))
        LightCache.copyFile(BASE_TEMP_FILE_NAME, BASE_TEMP_FILE_FULL_NAME_PATH, true)
        assertTrue(LightCache.testFileExist(BASE_TEMP_FILE_FULL_NAME_PATH))
        assertArrayEquals(byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte()), LightCache.loadFile(BASE_TEMP_FILE_FULL_NAME_PATH))
    }

    @Test
    fun listAllFilesInDirectory_withFilePathAsInput() {
        val fileName1 = BASE.toString() + "file1"
        assertTrue(LightCache.saveFile(fileName1, byteArrayOf('1'.code.toByte()), false))
        assertTrue(LightCache.listAllFilesInDirectory(File(fileName1)).isEmpty())
    }

    @Test
    fun listAllFilesInDirectory() {
        val fileName1 = BASE.toString() + "file1"
        val fileName2 = BASE.toString() + "dir1/file2"
        assertTrue(LightCache.saveFile(fileName1, byteArrayOf('1'.code.toByte()), false))
        assertTrue(LightCache.saveFile(fileName2, byteArrayOf('2'.code.toByte()), false))
        assertTrue(LightCache.listAllFilesInDirectory(File(BASE.toString() + "dir1")).contains(Uri.fromFile(File(fileName2))))
        assertTrue(LightCache.listAllFilesInDirectory(File(BASE))
                .containsAll(Arrays.asList(Uri.fromFile(File(fileName1)), Uri.fromFile(File(fileName2)))))
    }

    companion object {
        private val TEMP_FILE_NAME: String? = "test.temp"
        private var BASE: String? = ""
        private var BASE_TEMP_FILE_PATH: String? = null
        private var BASE_TEMP_FILE_NAME: String? = null
        private var BASE_TEMP_FILE_FULL_NAME_PATH: String? = null
    }
}