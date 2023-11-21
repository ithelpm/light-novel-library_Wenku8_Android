package org.mewx.wenku8.global

import android.content.ContentValues

/**
 * Created by MewX on 2015/1/20.
 *
 * 全局的设置类，负责所有设置的事务，以及全局的变量获取。
 */
@SuppressWarnings(["UnusedDeclaration"])
object GlobalConfig {
    // online arguments
    val blogPageUrl: String? = "https://wenku8.mewx.org/"
    val versionCheckUrl: String? = "https://wenku8.mewx.org/version"
    val noticeCheckSc: String? = "https://wenku8.mewx.org/args/notice_sc"
    val noticeCheckTc: String? = "https://wenku8.mewx.org/args/notice_tc"

    // constants
    val saveFolderName: String? = "saves"
    val imgsSaveFolderName: String? = "imgs"
    val customFolderName: String? = "custom"
    private val saveSearchHistoryFileName: String? = "search_history.wk8"
    private val saveReadSavesFileName: String? = "read_saves.wk8"
    private val saveReadSavesV1FileName: String? = "read_saves_v1.wk8"
    private val saveLocalBookshelfFileName: String? = "bookshelf_local.wk8"
    private val saveSetting: String? = "settings.wk8"
    private val saveUserAccountFileName: String? = "cert.wk8" // certification file
    private val saveUserAvatarFileName: String? = "avatar.jpg"
    private val saveNoticeString: String? = "notice.wk8" // the notice cache from online
    private var maxSearchHistory = 20 // default

    // reserved constants
    val UNKNOWN: String? = "Unknown"

    // vars
    private var lookupInternalStorageOnly = false
    private var isInBookshelf = false
    private var isInLatest = false
    private const val doLoadImage = true
    private var externalStoragePathAvailable = true
    private var currentLang: Wenku8API.LANG? = Wenku8API.LANG.SC
    var pathPickedSave: String? = null // dir picker save path

    // static variables
    private var searchHistory: ArrayList<String?>? = null
    private var readSaves: ArrayList<ReadSaves?>? = null // deprecated
    private var bookshelf: ArrayList<Integer?>? = null
    private var readSavesV1: ArrayList<ReadSavesV1?>? = null // deprecated
    private var allSetting: ContentValues? = null

    // sets and gets
    fun setCurrentLang(l: Wenku8API.LANG?) {
        currentLang = l
        setToAllSetting(SettingItems.language, currentLang.toString())
    }

    fun getCurrentLang(): Wenku8API.LANG? {
        val temp = getFromAllSetting(SettingItems.language)
        if (temp == null) {
            setToAllSetting(SettingItems.language, currentLang.toString())
        } else if (!temp.equals(currentLang.toString())) {
            if (temp.equals(Wenku8API.LANG.SC.toString())) currentLang = Wenku8API.LANG.SC else if (temp.equals(Wenku8API.LANG.TC.toString())) currentLang = Wenku8API.LANG.TC else currentLang = Wenku8API.LANG.SC
        }
        return currentLang
    }

    fun initImageLoader(context: Context?) {
        val localUnlimitedDiscCache = UnlimitedDiscCache(
                File(getDefaultStoragePath().toString() + "cache"),  // FIXME: these imgs folders are actually no in use.
                File((context.getCacheDir() + File.separator).toString() + "imgs"))
        val localDisplayImageOptions: DisplayImageOptions = Builder()
                .resetViewBeforeLoading(true)
                .cacheOnDisk(true)
                .cacheInMemory(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .resetViewBeforeLoading(true)
                .displayer(FadeInBitmapDisplayer(250)).build()
        val localImageLoaderConfiguration: ImageLoaderConfiguration = Builder(context)
                .diskCache(localUnlimitedDiscCache)
                .defaultDisplayImageOptions(localDisplayImageOptions).build()
        ImageLoader.getInstance().init(localImageLoaderConfiguration)
    }

    // settings
    fun getOpensourceLicense(): String? {
        val `is`: InputStream = MyApp.getContext().getResources().openRawResource(R.raw.license)
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }

    /**
     * 设置第一存储路径的合法性（第一路径可以只有设置）
     * @param available true-合法可以使用; false-不能使用，只能只用第二路径
     */
    fun setExternalStoragePathAvailable(available: Boolean) {
        externalStoragePathAvailable = available
    }

    fun getDefaultStoragePath(): String? {
        // The lookupInternalStorageOnly flag has the highest priority.
        return if (lookupInternalStorageOnly || !externalStoragePathAvailable) {
            SaveFileMigration.getInternalSavePath()
        } else SaveFileMigration.getExternalStoragePath()
    }

    // TODO: get rid of this shortcut.
    fun getBackupStoragePath(): String? {
        val internalPath: String = SaveFileMigration.getInternalSavePath()
        return if (getDefaultStoragePath().equals(internalPath)) SaveFileMigration.getExternalStoragePath() else internalPath
    }

    fun doCacheImage(): Boolean {
        // for non-image mode
        return doLoadImage // when cache, cache images
    }

    fun getShowTextSize(): Int {
        return 18 // in "sp"
    }

    fun getShowTextPaddingTop(): Int {
        return 48 // in "dp"
    }

    fun getShowTextPaddingLeft(): Int {
        return 32 // in "dp"
    }

    fun getShowTextPaddingRight(): Int {
        return 32 // in "dp"
    }

    fun getTextLoadWay(): Int {
        // 0 - Always load from online, when WLAN available
        // 1 - Load locally first, then considerate online
        // 2 - In bookshelf do (1), else do (0)
        return 2
    }

    // TODO: get rid of those ugly shortcuts.
    fun getFirstFullSaveFilePath(): String? {
        return getDefaultStoragePath() + saveFolderName + File.separator
    }

    fun getSecondFullSaveFilePath(): String? {
        return getBackupStoragePath() + saveFolderName + File.separator
    }

    fun getFirstFullUserAccountSaveFilePath(): String? {
        return getFirstFullSaveFilePath() + saveUserAccountFileName
    }

    fun getSecondFullUserAccountSaveFilePath(): String? {
        return getSecondFullSaveFilePath() + saveUserAccountFileName
    }

    fun getFirstUserAvatarSaveFilePath(): String? {
        return getFirstFullSaveFilePath() + saveUserAvatarFileName
    }

    fun getSecondUserAvatarSaveFilePath(): String? {
        return getSecondFullSaveFilePath() + saveUserAvatarFileName
    }

    /**
     * Extract image name.
     * @param url http://pic.wenku8.cn/pictures/1/1305/41759/50471.jpg
     * @return pictures113054175950471.jpg
     */
    fun generateImageFileNameByURL(url: String?): String? {
        val s: Array<String?> = url.split("/")
        val result = StringBuilder()
        var canStart = false
        for (temp in s) {
            if (!canStart && temp.contains(".")) canStart = true // judge canStart first
            else if (canStart) result.append(temp)
        }
        return result.toString()
    }

    @NonNull
    private fun loadFullSaveFileContent(@NonNull FileName: String?): String? {
        // get full file in file save path
        var h = ""
        if (LightCache.testFileExist(getDefaultStoragePath() + saveFolderName + File.separator + FileName)) {
            try {
                val b: ByteArray = LightCache.loadFile(getDefaultStoragePath() + saveFolderName + File.separator + FileName)
                        ?: return ""
                h = String(b, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        } else if (LightCache.testFileExist(getBackupStoragePath() + saveFolderName + File.separator + FileName)) {
            try {
                val b: ByteArray = LightCache.loadFile(getBackupStoragePath() + saveFolderName + File.separator + FileName)
                        ?: return ""
                h = String(b, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
        return h
    }

    private fun writeFullSaveFileContent(FileName: String?, @NonNull s: String?): Boolean {
        // process path and filename
        var path: String? = ""
        var fileName = FileName
        if (FileName.contains(File.separator)) {
            path = FileName.substring(0, FileName.lastIndexOf(File.separator))
            fileName = FileName.substring(FileName.lastIndexOf(File.separator)
                    + File.separator.length(), FileName.length())
        }

        // write save file in save path
        return if (!LightCache.saveFile(getDefaultStoragePath() + saveFolderName + File.separator + path, fileName, s.getBytes(), true)) LightCache.saveFile(getBackupStoragePath() + saveFolderName
                + File.separator + path, fileName, s.getBytes(), true) else true
    }

    @NonNull
    fun loadFullFileFromSaveFolder(subFolderName: String?, fileName: String?): String? {
        return loadFullSaveFileContent(subFolderName + File.separator
                + fileName)
    }

    fun writeFullFileIntoSaveFolder(subFolderName: String?, fileName: String?, s: String?): Boolean {
        // input no separator
        return writeFullSaveFileContent(subFolderName + File.separator
                + fileName, s)
    }

    /** Book shelf  */
    fun loadLocalBookShelf() {
        // Format:
        // aid||aid||aid
        // the file just saves the aid list
        bookshelf = ArrayList()
        val h = loadFullSaveFileContent(saveLocalBookshelfFileName)
        val p: Array<String?> = h.split("\\|\\|") // regular expression
        for (t in p) {
            if (t.equals("")) continue
            bookshelf.add(Integer.valueOf(t))
        }
    }

    fun writeLocalBookShelf() {
        if (bookshelf == null) loadLocalBookShelf()
        var s = ""
        for (i in 0 until bookshelf.size()) {
            if (i != 0) s += "||"
            s += bookshelf.get(i)
        }
        writeFullSaveFileContent(saveLocalBookshelfFileName, s)
    }

    fun addToLocalBookshelf(aid: Int) {
        if (bookshelf == null) loadLocalBookShelf()
        if (bookshelf.indexOf(aid) === -1) bookshelf.add(0, aid) // add to the first place
        writeLocalBookShelf()
    }

    fun removeFromLocalBookshelf(aid: Int) {
        if (bookshelf == null) loadLocalBookShelf()
        val i: Int = bookshelf.indexOf(aid)
        if (i != -1) bookshelf.remove(i)
        writeLocalBookShelf()
    }

    fun getLocalBookshelfList(): ArrayList<Integer?>? {
        if (bookshelf == null) loadLocalBookShelf()
        return bookshelf
    }

    fun testInLocalBookshelf(aid: Int): Boolean {
        if (bookshelf == null) loadLocalBookShelf()
        return bookshelf.indexOf(aid) !== -1
    }

    fun accessToLocalBookshelf(aid: Int) {
        val temp: Int = bookshelf.indexOf(aid)
        if (aid == -1) return
        bookshelf.remove(temp)
        bookshelf.add(0, aid)
        writeLocalBookShelf()
    }

    fun testInBookshelf(): Boolean {
        return isInBookshelf
    }

    fun EnterBookshelf() {
        isInBookshelf = true
    }

    fun LeaveBookshelf() {
        isInBookshelf = false
    }

    fun testInLatest(): Boolean {
        return isInLatest
    }

    fun EnterLatest() {
        isInLatest = true
    }

    fun LeaveLatest() {
        isInLatest = false
    }

    /** search history  */
    fun readSearchHistory() {
        // always initial empty
        searchHistory = ArrayList()

        // read history from file, if not exist, create.
        val h = loadFullSaveFileContent(saveSearchHistoryFileName)

        // separate the read string
        var i = 0
        var temp: Int
        while (true) {
            temp = h.indexOf("[", i) // find '['
            if (temp == -1) break
            i = temp + 1
            temp = h.indexOf("]", i) // get ']'
            if (temp == -1) break

            // ok, get a part
            searchHistory.add(h.substring(i, temp))
        }
    }

    fun writeSearchHistory() {
        // [0what][1what]...
        val temp = StringBuilder()
        for (i in 0 until searchHistory.size()) {
            temp.append("[").append(searchHistory.get(i)).append("]")
        }

        // write file
        writeFullSaveFileContent(saveSearchHistoryFileName, temp.toString())
    }

    fun getSearchHistory(): ArrayList<String?>? {
        if (searchHistory == null) readSearchHistory()
        return searchHistory
    }

    fun addSearchHistory(record: String?) {
        // record begins with a number, which represent its type
        if (searchHistory == null) readSearchHistory()
        if (searchHistory.indexOf("[") !== -1) return  // harmful

        // remove same thing
        while (true) {
            val pos: Int = searchHistory.indexOf(record)
            if (pos < 0) break else searchHistory.remove(pos)
        }
        while (searchHistory.size() >= maxSearchHistory) searchHistory.remove(maxSearchHistory - 1) // remove the last
        searchHistory.add(0, record) // add to the first place
        writeSearchHistory() // save history file
    }

    fun deleteSearchHistory(record: String?) {
        // record begins with a number, which represent its type
        if (searchHistory == null) readSearchHistory()
        if (searchHistory.indexOf("[") !== -1) return  // harmful

        // remove same thing
        while (true) {
            val pos: Int = searchHistory.indexOf(record)
            if (pos < 0) break else searchHistory.remove(pos)
        }
        writeSearchHistory() // save history file
    }

    @Deprecated
    fun onSearchClicked(index: Int) {
        if (index >= searchHistory.size()) return
        val temp: String = searchHistory.get(index)
        searchHistory.remove(index)
        searchHistory.add(0, temp)
        writeSearchHistory() // save history file
    }

    fun clearSearchHistory() {
        searchHistory = ArrayList()
        writeSearchHistory() // save history file
    }

    fun getMaxSearchHistory(): Int {
        return maxSearchHistory
    }

    fun setMaxSearchHistory(size: Int) {
        if (size > 0) maxSearchHistory = size
    }

    /** Read Saves (Old)  */
    fun loadReadSaves() {
        // Format:
        // cid,,pos,,height||cid,,pos,,height
        // just use split function
        readSaves = ArrayList()

        // read history from file, if not exist, create.
        val h = loadFullSaveFileContent(saveReadSavesFileName)

        // split string h
        val p: Array<String?> = h.split("\\|\\|") // regular expression
        for (temp in p) {
            Log.v("MewX", temp)
            val parts: Array<String?> = temp.split(",,")
            if (parts.size != 3) continue
            val rs = ReadSaves()
            rs.cid = Integer.valueOf(parts[0])
            rs.pos = Integer.valueOf(parts[1])
            rs.height = Integer.valueOf(parts[2])
            readSaves.add(rs)
        }
    }

    fun writeReadSaves() {
        if (readSaves == null) loadReadSaves()
        val t = StringBuilder()
        for (i in 0 until readSaves.size()) {
            if (i != 0) t.append("||")
            t.append(readSaves.get(i).cid).append(",,")
                    .append(readSaves.get(i).pos).append(",,")
                    .append(readSaves.get(i).height)
        }
        writeFullSaveFileContent(saveReadSavesFileName, t.toString())
    }

    fun addReadSavesRecord(c: Int, p: Int, h: Int) {
        if (p < 100) return  // no necessary to save it
        if (readSaves == null) loadReadSaves()

        // judge if exist, and if legal, update it
        for (i in 0 until readSaves.size()) {
            if (readSaves.get(i).cid === c) {
                // judge if need to update
                readSaves.get(i).pos = p
                readSaves.get(i).height = h
                writeReadSaves()
                return
            }
        }

        // new record
        val rs = ReadSaves()
        rs.cid = c
        rs.pos = p
        rs.height = h
        readSaves.add(rs)
        writeReadSaves()
    }

    fun getReadSavesRecord(c: Int, h: Int): Int {
        if (readSaves == null) loadReadSaves()
        for (i in 0 until readSaves.size()) {
            if (readSaves.get(i).cid === c) {
                // return h * readSaves.get(i).pos / readSaves.get(i).height;
                return readSaves.get(i).pos
            }
        }

        // by default
        return 0
    }

    /** Read Saves (V1)  */
    fun loadReadSavesV1() {
        // Format:
        // cid,,pos,,height||cid,,pos,,height
        // just use split function
        readSavesV1 = ArrayList()

        // read history from file, if not exist, create.
        val h = loadFullSaveFileContent(saveReadSavesV1FileName)

        // split string h
        val p: Array<String?> = h.split("\\|\\|") // regular expression
        OutLoop@ for (temp in p) {
            Log.v("MewX", temp)
            val parts: Array<String?> = temp.split(":") // \\:
            if (parts.size != 5) continue

            // judge legal
            for (str in parts) if (!LightTool.isInteger(str)) continue@OutLoop

            // add to list
            val rs = ReadSavesV1()
            rs.aid = Integer.valueOf(parts[0])
            rs.vid = Integer.valueOf(parts[1])
            rs.cid = Integer.valueOf(parts[2])
            rs.lineId = Integer.valueOf(parts[3])
            rs.wordId = Integer.valueOf(parts[4])
            readSavesV1.add(rs)
        }
    }

    fun writeReadSavesV1() {
        if (readSavesV1 == null) loadReadSavesV1()
        val t = StringBuilder()
        for (i in 0 until readSavesV1.size()) {
            if (i != 0) t.append("||")
            t.append(readSavesV1.get(i).aid).append(":")
                    .append(readSavesV1.get(i).vid).append(":")
                    .append(readSavesV1.get(i).cid).append(":")
                    .append(readSavesV1.get(i).lineId).append(":")
                    .append(readSavesV1.get(i).wordId)
        }
        writeFullSaveFileContent(saveReadSavesV1FileName, t.toString())
    }

    fun addReadSavesRecordV1(aid: Int, vid: Int, cid: Int, lineId: Int, wordId: Int) {
        if (readSavesV1 == null) loadReadSavesV1()

        // judge if exist, and if legal, update it
        for (i in 0 until readSavesV1.size()) {
            if (readSavesV1.get(i).aid === aid) {
                // need to update
                readSavesV1.get(i).vid = vid
                readSavesV1.get(i).cid = cid
                readSavesV1.get(i).lineId = lineId
                readSavesV1.get(i).wordId = wordId
                writeReadSavesV1()
                return
            }
        }

        // new record
        val rs = ReadSavesV1()
        rs.aid = aid
        rs.vid = vid
        rs.cid = cid
        rs.lineId = lineId
        rs.wordId = wordId
        readSavesV1.add(rs)
        writeReadSavesV1()
    }

    fun removeReadSavesRecordV1(aid: Int) {
        if (readSavesV1 == null) loadReadSavesV1()
        var i = 0
        while (i < readSavesV1.size()) {
            if (readSavesV1.get(i).aid === aid) break
            i++
        }
        if (i < readSavesV1.size()) readSavesV1.remove(i)
        writeReadSavesV1()
    }

    @Nullable
    fun getReadSavesRecordV1(aid: Int): ReadSavesV1? {
        if (readSavesV1 == null) loadReadSavesV1()
        for (i in 0 until readSavesV1.size()) {
            if (readSavesV1.get(i).aid === aid) return readSavesV1.get(i)
        }
        return null
    }

    /** All settings  */
    fun loadAllSetting() {
        // Verify which storage source to user.
        lookupInternalStorageOnly = SaveFileMigration.migrationCompleted()

        // Loads all settings.
        allSetting = ContentValues()
        val h = loadFullSaveFileContent(saveSetting)
        val sets: Array<String?> = h.split("\\|\\|\\|\\|")
        for (set in sets) {
            val temp: Array<String?> = set.split("::::")
            if (temp.size != 2 || temp[0] == null || temp[0].length() === 0 || temp[1] == null || temp[1].length() === 0) continue
            allSetting.put(temp[0], temp[1])
        }

        // Updates settings version.
        val version = getFromAllSetting(SettingItems.version)
        if (version == null || version.isEmpty()) {
            setToAllSetting(SettingItems.version, "1")
        }
        // Else, reserved for future settings migration.
    }

    fun saveAllSetting() {
        if (allSetting == null) loadAllSetting()
        val result = StringBuilder()
        for (key in allSetting.keySet()) {
            if (!result.toString().equals("")) result.append("||||")
            result.append(key).append("::::").append(allSetting.getAsString(key))
        }
        writeFullSaveFileContent(saveSetting, result.toString())
    }

    @Nullable
    fun getFromAllSetting(name: SettingItems?): String? {
        if (allSetting == null) loadAllSetting()
        return allSetting.getAsString(name.toString())
    }

    fun setToAllSetting(name: SettingItems?, value: String?) {
        if (allSetting == null) loadAllSetting()
        if (name != null && value != null) {
            allSetting.remove(name.toString())
            allSetting.put(name.toString(), value)
            saveAllSetting()
        }
    }
    /* Novel content */
    /**
     * Async gets image url and download to save folder's image folder.
     *
     * @param url full http url of target image
     * @return if file finally exist, if already exist before saving, still
     * return true; if finally the file does not exist, return false.
     */
    fun saveNovelContentImage(url: String?): Boolean {
        val imgFileName = generateImageFileNameByURL(url)
        val defaultFullPath = getFirstFullSaveFilePath() + imgsSaveFolderName + File.separator + imgFileName
        val fallbackFullPath = getSecondFullSaveFilePath() + imgsSaveFolderName + File.separator + imgFileName
        if (!LightCache.testFileExist(defaultFullPath) && !LightCache.testFileExist(fallbackFullPath)) {
            // neither of the file exist
            val fileContent: ByteArray = LightNetwork.LightHttpDownload(url) ?: return false
            // network error
            return (LightCache.saveFile(defaultFullPath, fileContent, true)
                    || LightCache.saveFile(fallbackFullPath, fileContent, true))
        }
        return true // file exist
    }

    /**
     * Gets available local saving path of target image.
     *
     * @param fileName just need the fileName
     * @return direct fileName or just null
     */
    fun getAvailableNovelContentImagePath(fileName: String?): String? {
        val defaultFullPath = getFirstFullSaveFilePath() + imgsSaveFolderName + File.separator + fileName
        val fallbackFullPath = getSecondFullSaveFilePath() + imgsSaveFolderName + File.separator + fileName
        return if (LightCache.testFileExist(defaultFullPath)) {
            defaultFullPath
        } else if (LightCache.testFileExist(fallbackFullPath)) {
            fallbackFullPath
        } else {
            null
        }
    }

    fun isNetworkAvailable(context: Context?): Boolean {
        val cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm != null) {
            // connected
            val activeNetwork: NetworkInfo = cm.getActiveNetworkInfo()
            return activeNetwork != null
        }
        return false // not connected
    }

    /* notice */
    @NonNull
    fun loadSavedNotice(): String? {
        return loadFullSaveFileContent(saveNoticeString)
    }

    fun writeTheNotice(@NonNull noticeStr: String?) {
        writeFullSaveFileContent(saveNoticeString, noticeStr)
    }

    /** Structures  */
    class ReadSaves {
        // deprecated
        var cid = 0
        var pos = 0 // last time scroll Y pos
        var height = 0 // last time scroll Y height
    }

    class ReadSavesV1 {
        // deprecated
        var aid = 0
        var vid = 0
        var cid = 0
        var lineId = 0
        var wordId = 0
    }

    enum class SettingItems {
        version,  // (int) 1
        language, menu_bg_id,  // (int) 1-5 by system, 0 for user
        menu_bg_path,  // (String) for user custom
        reader_font_path,  // (String) path to ttf, "0" means default
        reader_font_size,  // (int) sp (8 - 32)
        reader_line_distance,  // (int) dp (0 - 32)
        reader_paragraph_distance,  // (int) dp (0 - 48)
        reader_paragraph_edge_distance,  // (int) dp (0 - 32)
        reader_background_path
        // (String) path to an image, day mode only, "0" means default
    }
}