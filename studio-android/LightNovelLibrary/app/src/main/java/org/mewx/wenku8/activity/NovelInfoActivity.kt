package org.mewx.wenku8.activity

import android.content.ContentValues

/**
 * Created by MewX on 2015/5/13.
 * Novel Info Activity.
 */
class NovelInfoActivity : BaseMaterialActivity() {
    // constant
    private val FromLocal: String? = "fav"

    // private vars
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var aid = 1
    private var from: String? = ""
    private var title: String? = ""
    private var isLoading = true
    private var rlMask: RelativeLayout? = null // mask layout
    private var mLinearLayout: LinearLayout? = null
    private var tvNovelTitle: TextView? = null
    private var tvNovelAuthor: TextView? = null
    private var tvNovelStatus: TextView? = null
    private var tvNovelUpdate: TextView? = null
    private var tvLatestChapter: TextView? = null
    private var tvNovelFullIntro: TextView? = null
    private var pDialog: MaterialDialog? = null
    private var fabFavorite: FloatingActionButton? = null
    private var famMenu: FloatingActionsMenu? = null
    private var spb: SmoothProgressBar? = null
    private var mNovelItemMeta: NovelItemMeta? = null
    private var listVolume: List<VolumeList?>? = ArrayList()
    private var novelFullMeta: String? = null
    private var novelFullIntro: String? = null
    private var novelFullVolume: String? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_novel_info)

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)
        from = getIntent().getStringExtra("from")
        title = getIntent().getStringExtra("title")

        // Analysis.
        val viewItemParams = Bundle()
        viewItemParams.putString(FirebaseAnalytics.Param.ITEM_ID, "" + aid)
        viewItemParams.putString(FirebaseAnalytics.Param.ITEM_NAME, title)
        viewItemParams.putString("from", from)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, viewItemParams)

        // UIL setting
        if (ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this)
        }

        // get views
        rlMask = findViewById(R.id.white_mask)
        mLinearLayout = findViewById(R.id.novel_info_scroll)
        val llCardLayout: LinearLayout = findViewById(R.id.item_card)
        val ivNovelCover: ImageView = findViewById(R.id.novel_cover)
        tvNovelTitle = findViewById(R.id.novel_title)
        tvNovelAuthor = findViewById(R.id.novel_author)
        tvNovelStatus = findViewById(R.id.novel_status)
        tvNovelUpdate = findViewById(R.id.novel_update)
        val tvLatestChapterNameText: TextView = findViewById(R.id.novel_item_text_shortinfo)
        tvLatestChapter = findViewById(R.id.novel_intro)
        tvNovelFullIntro = findViewById(R.id.novel_intro_full)
        val ibNovelOption: ImageButton = findViewById(R.id.novel_option)
        fabFavorite = findViewById(R.id.fab_favorate)
        val fabDownload: FloatingActionButton = findViewById(R.id.fab_download)
        famMenu = findViewById(R.id.multiple_actions)
        spb = findViewById(R.id.spb)

        // hide view and set colors
        tvNovelTitle.setText(title)
        // FIXME: these imgs folders are actually no in use.
        if (LightCache.testFileExist((GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + aid + ".jpg", ivNovelCover) else if (LightCache.testFileExist((GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + aid + ".jpg", ivNovelCover) else ImageLoader.getInstance().displayImage(Wenku8API.getCoverURL(aid), ivNovelCover) // move to onCreateView!
        tvLatestChapterNameText.setText(getResources().getText(R.string.novel_item_latest_chapter))
        ibNovelOption.setVisibility(ImageButton.INVISIBLE)
        fabFavorite.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
        fabDownload.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
        llCardLayout.setBackgroundResource(R.color.menu_transparent)
        if (GlobalConfig.testInLocalBookshelf(aid)) {
            fabFavorite.setIcon(R.drawable.ic_favorate_pressed)
        }

        // fetch all info
        getSupportActionBar().setTitle(R.string.action_novel_info)
        spb.setVisibility(View.INVISIBLE) // wait for runnable
        val handler = Handler()
        handler.postDelayed({
            spb.setVisibility(View.VISIBLE)
            if (from.equals(FromLocal)) refreshInfoFromLocal() else refreshInfoFromCloud()
        }, 500)


        // set on click listeners
        famMenu.setOnFloatingActionsMenuUpdateListener(object : OnFloatingActionsMenuUpdateListener() {
            @Override
            fun onMenuExpanded() {
                rlMask.setVisibility(View.VISIBLE)
            }

            @Override
            fun onMenuCollapsed() {
                rlMask.setVisibility(View.INVISIBLE)
            }
        })
        rlMask.setOnClickListener { v ->
            // Collapse the fam
            if (famMenu.isExpanded()) famMenu.collapse()
        }
        tvNovelTitle.setBackground(getResources().getDrawable(R.drawable.btn_menu_item))
        tvNovelAuthor.setBackground(getResources().getDrawable(R.drawable.btn_menu_item))
        tvLatestChapter.setBackground(getResources().getDrawable(R.drawable.btn_menu_item))
        tvNovelTitle.setOnClickListener { v ->
            if (runLoadingChecker()) return@setOnClickListener

            // show aid: title
            Builder(this@NovelInfoActivity)
                    .theme(Theme.LIGHT)
                    .titleColorRes(R.color.dlgTitleColor)
                    .backgroundColorRes(R.color.dlgBackgroundColor)
                    .contentColorRes(R.color.dlgContentColor)
                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                    .title(R.string.dialog_content_novel_title)
                    .content(aid.toString() + ": " + mNovelItemMeta.title)
                    .contentGravity(GravityEnum.CENTER)
                    .positiveText(R.string.dialog_positive_known)
                    .show()
        }
        tvNovelAuthor.setOnClickListener { v ->
            if (runLoadingChecker()) return@setOnClickListener
            Builder(this@NovelInfoActivity)
                    .theme(Theme.LIGHT)
                    .onPositive { ignored1, ignored2 ->
                        // search author name
                        val intent = Intent(this@NovelInfoActivity, SearchResultActivity::class.java)
                        intent.putExtra("key", mNovelItemMeta.author)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.hold)
                    }
                    .content(R.string.dialog_content_search_author)
                    .positiveText(R.string.dialog_positive_ok)
                    .negativeText(R.string.dialog_negative_biao)
                    .show()
        }
        fabFavorite.setOnClickListener { v ->
            if (runLoadingChecker()) return@setOnClickListener

            // add to favorite
            if (GlobalConfig.testInLocalBookshelf(aid)) {
                Builder(this@NovelInfoActivity)
                        .onPositive { ignored1, ignored2 ->
                            // delete from cloud first, if succeed then delete from local
                            val arbfc: AsyncRemoveBookFromCloud = AsyncRemoveBookFromCloud()
                            arbfc.execute(aid)
                        }
                        .theme(Theme.LIGHT)
                        .backgroundColorRes(R.color.dlgBackgroundColor)
                        .contentColorRes(R.color.dlgContentColor)
                        .positiveColorRes(R.color.dlgPositiveButtonColor)
                        .negativeColorRes(R.color.dlgNegativeButtonColor)
                        .content(R.string.dialog_content_sure_to_unfav)
                        .contentGravity(GravityEnum.CENTER)
                        .positiveText(R.string.dialog_positive_yes)
                        .negativeText(R.string.dialog_negative_preferno)
                        .show()
            } else {
                // not in bookshelf, add it to.
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$aid-intro.xml", novelFullMeta)
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$aid-introfull.xml", novelFullIntro)
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$aid-volume.xml", novelFullVolume)
                GlobalConfig.addToLocalBookshelf(aid)
                if (GlobalConfig.testInLocalBookshelf(aid)) { // in
                    Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.bookshelf_added), Toast.LENGTH_SHORT).show()
                    fabFavorite.setIcon(R.drawable.ic_favorate_pressed)
                } else {
                    Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.bookshelf_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        fabDownload.setOnClickListener { v ->
            if (runLoadingChecker()) return@setOnClickListener
            if (!GlobalConfig.testInLocalBookshelf(aid)) {
                Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.system_fav_it_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // download / update activity or verify downloading action (add to queue)
            // use list dialog to provide more functions
            Builder(this@NovelInfoActivity)
                    .theme(Theme.LIGHT)
                    .title(R.string.dialog_title_choose_download_option)
                    .backgroundColorRes(R.color.dlgBackgroundColor)
                    .titleColorRes(R.color.dlgTitleColor)
                    .negativeText(R.string.dialog_negative_pass)
                    .negativeColorRes(R.color.dlgNegativeButtonColor)
                    .itemsGravity(GravityEnum.CENTER)
                    .items(R.array.download_option)
                    .itemsCallback { dialog, view, which, text ->
                        when (which) {
                            0 -> optionCheckUpdates()
                            1 -> optionDownloadUpdates()
                            2 -> optionDownloadOverride()
                            3 -> optionDownloadSelected()
                        }
                    }
                    .show()
        }
        tvLatestChapter.setOnClickListener { view ->
            if (runLoadingChecker()) return@setOnClickListener

            // no sufficient info
            if (mNovelItemMeta != null && mNovelItemMeta.latestSectionCid !== 0) showDirectJumpToReaderDialog(mNovelItemMeta.latestSectionCid) else Toast.makeText(this, getResources().getText(R.string.reader_msg_please_refresh_and_retry), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * run loading checker
     * @return true if loading; otherwise false
     */
    private fun runLoadingChecker(): Boolean {
        if (isLoading) {
            Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.system_loading_please_wait), Toast.LENGTH_SHORT).show()
        }
        return isLoading
    }

    /**
     * 0 <string name="dialog_option_check_for_update">检查更新</string>
     */
    private fun optionCheckUpdates() {
        // async task
        isLoading = true
        val auct: AsyncUpdateCacheTask = AsyncUpdateCacheTask()
        auct.execute(aid, 0)

        // show progress
        pDialog = Builder(this@NovelInfoActivity)
                .theme(Theme.LIGHT)
                .content(R.string.dialog_content_downloading)
                .progress(false, 1, true)
                .cancelable(true)
                .cancelListener { dialog12 ->
                    isLoading = false
                    auct.cancel(true)
                    pDialog.dismiss()
                    pDialog = null
                }
                .show()
        pDialog.setProgress(0)
        pDialog.setMaxProgress(1)
        pDialog.show()
    }

    /**
     * 1 <string name="dialog_option_update_uncached_volumes">更新下载</string>
     */
    private fun optionDownloadUpdates() {
        // async task
        isLoading = true
        val auct: AsyncUpdateCacheTask = AsyncUpdateCacheTask()
        auct.execute(aid, 1)

        // show progress
        pDialog = Builder(this@NovelInfoActivity)
                .theme(Theme.LIGHT)
                .content(R.string.dialog_content_downloading)
                .progress(false, 1, true)
                .cancelable(true)
                .cancelListener { dialog1 ->
                    isLoading = false
                    auct.cancel(true)
                    pDialog.dismiss()
                    pDialog = null
                }
                .show()
        pDialog.setProgress(0)
        pDialog.setMaxProgress(1)
        pDialog.show()
    }

    /**
     * 2 <string name="dialog_option_force_update_all">覆盖下载</string>
     */
    private fun optionDownloadOverride() {
        Builder(this@NovelInfoActivity)
                .onPositive { ignored1, ignored2 ->
                    // async task
                    isLoading = true
                    val auct: AsyncUpdateCacheTask = AsyncUpdateCacheTask()
                    auct.execute(aid, 2)

                    // show progress
                    pDialog = Builder(this@NovelInfoActivity)
                            .theme(Theme.LIGHT)
                            .content(R.string.dialog_content_downloading)
                            .progress(false, 1, true)
                            .cancelable(true)
                            .cancelListener { dialog13 ->
                                isLoading = false
                                auct.cancel(true)
                                pDialog.dismiss()
                                pDialog = null
                            }
                            .show()
                    pDialog.setProgress(0)
                    pDialog.setMaxProgress(1)
                    pDialog.show()
                }
                .theme(Theme.LIGHT)
                .backgroundColorRes(R.color.dlgBackgroundColor)
                .contentColorRes(R.color.dlgContentColor)
                .positiveColorRes(R.color.dlgPositiveButtonColor)
                .negativeColorRes(R.color.dlgNegativeButtonColor)
                .content(R.string.dialog_content_verify_force_update)
                .contentGravity(GravityEnum.CENTER)
                .positiveText(R.string.dialog_positive_likethis)
                .negativeText(R.string.dialog_negative_preferno)
                .show()
    }

    /**
     * 3 <string name="dialog_option_select_and_update">分卷下载</string>
     */
    private fun optionDownloadSelected() {
        // select volumes
        val volumes = arrayOfNulls<String?>(listVolume.size())
        for (i in 0 until listVolume.size()) volumes[i] = listVolume.get(i).volumeName
        Builder(this@NovelInfoActivity)
                .theme(Theme.LIGHT)
                .title(R.string.dialog_option_select_and_update)
                .items(volumes)
                .itemsCallbackMultiChoice(null) { dialog, which, text ->
                    if (which == null || which.length === 0) return@itemsCallbackMultiChoice true
                    val adv: AsyncDownloadVolumes = AsyncDownloadVolumes()
                    adv.execute(which)
                    true
                }
                .positiveText(R.string.dialog_positive_ok)
                .show()
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(getResources().getString(R.string.action_novel_info))
        getMenuInflater().inflate(R.menu.menu_novel_info, menu)

        // fill the icon to white color
        for (i in 0 until menu.size()) {
            val drawable: Drawable = menu.getItem(i).getIcon()
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
            }
        }
        return true
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            if (Build.VERSION.SDK_INT < 21) finish() else finishAfterTransition() // end directly
        } else if (menuItem.getItemId() === R.id.action_continue_read_progress) {
            if (runLoadingChecker()) return true

            // show dialog, jump to last read position
            val rs: GlobalConfig.ReadSavesV1 = GlobalConfig.getReadSavesRecordV1(aid)
            if (rs != null) {
                showDirectJumpToReaderDialog(rs.cid)
                return true
            }
            // not found
            Toast.makeText(this, getResources().getText(R.string.reader_msg_no_saved_reading_progress), Toast.LENGTH_SHORT).show()
        } else if (menuItem.getItemId() === R.id.action_go_to_forum) {
            val intent = Intent(this@NovelInfoActivity, NovelReviewListActivity::class.java)
            intent.putExtra("aid", aid)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun showDirectJumpToReaderDialog(cid: Int) {
        // find volumeList
        var savedVolumeList: VolumeList? = null
        var chapterInfo: ChapterInfo? = null
        for (vl in listVolume) {
            for (ci in vl.chapterList) {
                if (ci.cid === cid) {
                    chapterInfo = ci
                    savedVolumeList = vl
                    break
                }
            }
        }
        // no sufficient info
        if (savedVolumeList == null) {
            Toast.makeText(this, getResources().getText(R.string.reader_msg_no_available_chapter), Toast.LENGTH_SHORT).show()
            return
        }
        val volumeList_bak: VolumeList? = savedVolumeList
        Builder(this)
                .onPositive { ignored1, ignored2 ->
                    // jump to reader activity
                    val intent = Intent(this@NovelInfoActivity, Wenku8ReaderActivityV1::class.java)
                    intent.putExtra("aid", aid)
                    intent.putExtra("volume", volumeList_bak)
                    intent.putExtra("cid", cid)

                    // test does file exist
                    if (from.equals(FromLocal)
                            && !LightCache.testFileExist(((GlobalConfig.getDefaultStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + cid).toString() + ".xml")
                            && !LightCache.testFileExist(((GlobalConfig.getBackupStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + cid).toString() + ".xml")) {
                        intent.putExtra("from", "cloud") // from cloud
                    } else {
                        intent.putExtra("from", from) // from "fav"
                    }
                    intent.putExtra("forcejump", "yes")
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                }
                .theme(Theme.LIGHT)
                .titleColorRes(R.color.default_text_color_black)
                .backgroundColorRes(R.color.dlgBackgroundColor)
                .contentColorRes(R.color.dlgContentColor)
                .positiveColorRes(R.color.dlgPositiveButtonColor)
                .negativeColorRes(R.color.dlgNegativeButtonColor)
                .title(R.string.reader_v1_notice)
                .content((getResources().getString(R.string.reader_jump_last) + "\n" + title + "\n" + savedVolumeList.volumeName).toString() + "\n" + chapterInfo.chapterName)
                .contentGravity(GravityEnum.CENTER)
                .positiveText(R.string.dialog_positive_sure)
                .negativeText(R.string.dialog_negative_biao)
                .show()
    }

    @Override
    fun onBackPressed() {
        // end famMenu first
        if (famMenu.isExpanded()) {
            famMenu.collapse()
            return
        }

        // normal exit
        if (Build.VERSION.SDK_INT < 21) finish() else finishAfterTransition() // end directly
    }

    private inner class FetchInfoAsyncTask : AsyncTask<Integer?, Integer?, Integer?>() {
        var fromLocal = false
        @Override
        protected fun doInBackground(vararg params: Integer?): Integer? {
            // transfer '1' to this task represent loading from local
            if (params != null && params.size == 1 && params[0] == 1) fromLocal = true

            // get novel full meta
            try {
                if (fromLocal) {
                    novelFullMeta = GlobalConfig.loadFullFileFromSaveFolder("intro", "$aid-intro.xml")
                    if (novelFullMeta.isEmpty()) return -9
                } else {
                    val cv: ContentValues = Wenku8API.getNovelFullMeta(aid, GlobalConfig.getCurrentLang())
                    val byteNovelFullMeta: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                            ?: return -1
                    novelFullMeta = String(byteNovelFullMeta, "UTF-8") // save
                }
                mNovelItemMeta = Wenku8Parser.parseNovelFullMeta(novelFullMeta)
                if (mNovelItemMeta == null) return -1
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return -2
            }
            publishProgress(1) // procedure 1/3

            // get novel full intro
            try {
                if (fromLocal) {
                    novelFullIntro = GlobalConfig.loadFullFileFromSaveFolder("intro", "$aid-introfull.xml")
                    if (novelFullIntro.isEmpty()) return -9
                } else {
                    val cvFullIntroRequest: ContentValues = Wenku8API.getNovelFullIntro(aid, GlobalConfig.getCurrentLang())
                    val byteNovelFullInfo: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cvFullIntroRequest)
                            ?: return -1
                    novelFullIntro = String(byteNovelFullInfo, "UTF-8") // save
                }
                mNovelItemMeta.fullIntro = novelFullIntro
                if (mNovelItemMeta.fullIntro.length() === 0) return -1
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return -2
            }
            publishProgress(2)

            // get novel chapter list
            try {
                if (fromLocal) {
                    novelFullVolume = GlobalConfig.loadFullFileFromSaveFolder("intro", "$aid-volume.xml")
                    if (novelFullVolume.isEmpty()) return -9
                } else {
                    val cv: ContentValues = Wenku8API.getNovelIndex(aid, GlobalConfig.getCurrentLang())
                    val byteNovelChapterList: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                            ?: return -1
                    novelFullVolume = String(byteNovelChapterList, "UTF-8") // save
                }

                // update the volume list
                listVolume = Wenku8Parser.getVolumeList(novelFullVolume)
                if (listVolume.isEmpty()) return -1
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return -2
            }
            publishProgress(3) // procedure 3/3

            // Check local volume files exists, express in another color
            for (vl in listVolume) {
                for (ci in vl.chapterList) {
                    if (!LightCache.testFileExist((GlobalConfig.getFirstFullSaveFilePath() + "novel" + File.separator + ci.cid).toString() + ".xml")
                            && !LightCache.testFileExist((GlobalConfig.getSecondFullSaveFilePath() + "novel" + File.separator + ci.cid).toString() + ".xml")) break
                    if (vl.chapterList.indexOf(ci) === vl.chapterList.size() - 1) vl.inLocal = true
                }
            }
            return 0
        }

        @Override
        protected fun onProgressUpdate(vararg values: Integer?) {
            super.onProgressUpdate(values)
            when (values[0]) {
                1 -> {
                    // update general info
                    tvNovelAuthor.setPaintFlags(tvNovelAuthor.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG) // with hyperlink
                    tvLatestChapter.setPaintFlags(tvLatestChapter.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG) // with hyperlink
                    tvNovelTitle.setText(mNovelItemMeta.title)
                    tvNovelAuthor.setText(mNovelItemMeta.author)
                    tvNovelStatus.setText(mNovelItemMeta.bookStatus)
                    tvNovelUpdate.setText(mNovelItemMeta.lastUpdate)
                    tvLatestChapter.setText(mNovelItemMeta.latestSectionName)
                    if (this@NovelInfoActivity.getSupportActionBar() != null) this@NovelInfoActivity.getSupportActionBar().setTitle(mNovelItemMeta.title) // set action bar title
                }

                2 ->                     //update novel info full
                    tvNovelFullIntro.setText(mNovelItemMeta.fullIntro)

                3 -> {}
            }
        }

        @Override
        protected fun onPostExecute(integer: Integer?) {
            isLoading = false
            spb.progressiveStop()
            super.onPostExecute(integer)
            if (integer == -1) {
                Toast.makeText(this@NovelInfoActivity, "FetchInfoAsyncTask:onPostExecute network error", Toast.LENGTH_SHORT).show()
                return
            } else if (integer == -9) {
                Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.bookshelf_intro_load_failed), Toast.LENGTH_SHORT).show()
                // TODO: a better fix with optionCheckUpdates(), but need to avoid recursive calls.
                return
            } else if (integer < 0) return  // ignore other exceptions
            buildVolumeList()
        }
    }

    private fun buildVolumeList() {
        // remove all TextView(in CardView, in RelativeView)
        if (mLinearLayout.getChildCount() >= 3) mLinearLayout.removeViews(2, mLinearLayout.getChildCount() - 2)
        val rs: GlobalConfig.ReadSavesV1 = GlobalConfig.getReadSavesRecordV1(aid)
        for (vl in listVolume) {
            // get view
            val rl: RelativeLayout = LayoutInflater.from(this@NovelInfoActivity).inflate(R.layout.view_novel_chapter_item, null) as RelativeLayout
            // set text and listeners
            val tv: TextView = rl.findViewById(R.id.chapter_title)
            tv.setText(vl.volumeName)
            if (vl.inLocal) (rl.findViewById(R.id.chapter_status) as TextView).setText(getResources().getString(R.string.bookshelf_inlocal))
            val btn: RelativeLayout = rl.findViewById(R.id.chapter_btn)
            // added indicator for last read volume
            if (rs != null && rs.vid === vl.vid) {
                btn.setBackgroundColor(Color.LTGRAY)
            }
            btn.setOnLongClickListener { v ->
                Builder(this@NovelInfoActivity)
                        .theme(Theme.LIGHT)
                        .onPositive { ignored1, ignored2 ->
                            vl.cleanLocalCache()
                            (rl.findViewById(R.id.chapter_status) as TextView).setText("")
                        }
                        .content(R.string.dialog_sure_to_clear_cache)
                        .positiveText(R.string.dialog_positive_want)
                        .negativeText(R.string.dialog_negative_biao)
                        .show()
                true
            }
            btn.setOnClickListener { v ->
                // jump to chapter select activity
                val intent = Intent(this@NovelInfoActivity, NovelChapterActivity::class.java)
                intent.putExtra("aid", aid)
                intent.putExtra("volume", vl)
                intent.putExtra("from", from)
                startActivity(intent)
            }

            // add to scroll view
            mLinearLayout.addView(rl)
        }
    }

    internal inner class AsyncUpdateCacheTask : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        // in: Aid, OperationType
        // out: current loading
        var volumeXml: String? = null
        var introXml: String? = null
        var vl: List<VolumeList?>? = ArrayList()
        private var ni: NovelItemMeta? = null
        var size_a = 0
        var current = 0
        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            if (params == null || params.size < 2) return Wenku8Error.ErrorCode.PARAM_COUNT_NOT_MATCHED
            val taskAid: Int = params[0]
            val operationType: Int = params[1] // type = 0, 1, 2, 3

            // get full range online, always
            try {
                // fetch intro
                if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK // cancel
                var cv: ContentValues = Wenku8API.getNovelIndex(taskAid, GlobalConfig.getCurrentLang())
                val tempVolumeXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                        ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                // network error
                volumeXml = String(tempVolumeXml, "UTF-8")
                if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK // cancel
                cv = Wenku8API.getNovelFullMeta(taskAid, GlobalConfig.getCurrentLang())
                val tempIntroXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                        ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                // network error
                introXml = String(tempIntroXml, "UTF-8")

                // parse into structures
                vl = Wenku8Parser.getVolumeList(volumeXml)
                ni = Wenku8Parser.parseNovelFullMeta(introXml)
                if (vl.isEmpty() || ni == null) return Wenku8Error.ErrorCode.XML_PARSE_FAILED // parse failed
                if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK // calcel
                cv = Wenku8API.getNovelFullIntro(ni.aid, GlobalConfig.getCurrentLang())
                val tempFullIntro: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                        ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                // network error
                ni.fullIntro = String(tempFullIntro, "UTF-8")

                // write into saved file
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$taskAid-intro.xml", introXml)
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$taskAid-introfull.xml", ni.fullIntro)
                GlobalConfig.writeFullFileIntoSaveFolder("intro", "$taskAid-volume.xml", volumeXml)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                return Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING
            }
            if (operationType == 0) return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED // update info

            // calc size
            for (tempVl in vl) {
                size_a += tempVl.chapterList.size()
            }
            pDialog.setMaxProgress(size_a)

            // cache each cid to save the whole book
            // and will need to download all the images
            for (tempVl in vl) {
                for (tempCi in tempVl.chapterList) {
                    try {
                        val cv: ContentValues = Wenku8API.getNovelContent(ni.aid, tempCi.cid, GlobalConfig.getCurrentLang())

                        // load from local first
                        if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK // calcel
                        var xml: String = GlobalConfig.loadFullFileFromSaveFolder("novel", tempCi.cid + ".xml") // prevent empty file
                        if (xml.length() === 0 || operationType == 2) {
                            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                            // network error
                            xml = String(tempXml, "UTF-8")
                            if (xml.trim().length() === 0) return Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING
                            GlobalConfig.writeFullFileIntoSaveFolder("novel", tempCi.cid + ".xml", xml)
                        }

                        // cache image
                        if (GlobalConfig.doCacheImage()) {
                            val nc: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(xml)
                            for (i in 0 until nc.size()) {
                                if (nc[i].type === NovelContentType.IMAGE) {
                                    pDialog.setMaxProgress(++size_a)

                                    // save this images, judge exist first
                                    val imgFileName: String = GlobalConfig
                                            .generateImageFileNameByURL(nc[i].content)
                                    if (!LightCache.testFileExist(GlobalConfig.getFirstFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                                            && !LightCache.testFileExist(GlobalConfig.getSecondFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                                            || operationType == 2) {
                                        // neither of the file exist
                                        val fileContent: ByteArray = LightNetwork.LightHttpDownload(nc[i].content)
                                                ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                                        // network error
                                        if (!LightCache.saveFile(GlobalConfig.getFirstFullSaveFilePath()
                                                        + GlobalConfig.imgsSaveFolderName + File.separator,
                                                        imgFileName, fileContent, true)) // fail
                                        // to first path
                                            LightCache.saveFile(GlobalConfig.getSecondFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator,
                                                    imgFileName, fileContent, true)
                                    }
                                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                                    publishProgress(++current) // update
                                    // progress
                                }
                            }
                        }
                        publishProgress(++current) // update progress
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            }
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onProgressUpdate(vararg values: Integer?) {
            if (pDialog != null) pDialog.setProgress(values[0])
        }

        protected fun onPostExecute(result: Wenku8Error.ErrorCode?) {
            if (result === Wenku8Error.ErrorCode.USER_CANCELLED_TASK) {
                // user cancelled
                Toast.makeText(this@NovelInfoActivity, R.string.system_manually_cancelled, Toast.LENGTH_LONG).show()
                if (pDialog != null) pDialog.dismiss()
                onResume()
                isLoading = false
                return
            } else if (result === Wenku8Error.ErrorCode.NETWORK_ERROR) {
                Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.system_network_error), Toast.LENGTH_LONG).show()
                if (pDialog != null) pDialog.dismiss()
                onResume()
                isLoading = false
                return
            } else if (result === Wenku8Error.ErrorCode.XML_PARSE_FAILED
                    || result === Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING) {
                Toast.makeText(this@NovelInfoActivity, "Server returned strange data! (copyright reason?)", Toast.LENGTH_LONG).show()
                if (pDialog != null) pDialog.dismiss()
                onResume()
                isLoading = false
                return
            }

            // cache successfully
            Toast.makeText(this@NovelInfoActivity, "OK", Toast.LENGTH_LONG).show()
            isLoading = false
            if (pDialog != null) pDialog.dismiss()
            refreshInfoFromLocal()
        }
    }

    internal inner class AsyncRemoveBookFromCloud : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        var md: MaterialDialog? = null
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            md = Builder(this@NovelInfoActivity)
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_content_novel_remove_from_cloud)
                    .contentColorRes(R.color.dlgContentColor)
                    .progress(true, 0)
                    .show()
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            // params: aid
            val bytes: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getDelFromBookshelfParams(params[0]))
                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
            val result: String
            return try {
                result = String(bytes, "UTF-8")
                Log.d("MewX", result)
                if (!LightTool.isInteger(result)) return Wenku8Error.ErrorCode.RETURNED_VALUE_EXCEPTION
                if (Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED && Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN && Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF) {
                    Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result))
                } else {
                    // remove from local bookshelf
                    // already in bookshelf
                    for (tempVl in listVolume) {
                        for (tempCi in tempVl.chapterList) {
                            LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml")
                            LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml")
                        }
                    }

                    // delete files
                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-intro.xml")
                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-introfull.xml")
                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-volume.xml")
                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-intro.xml")
                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-introfull.xml")
                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-volume.xml")

                    // remove from bookshelf
                    GlobalConfig.removeFromLocalBookshelf(aid)
                    if (!GlobalConfig.testInLocalBookshelf(aid)) { // not in
                        Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
                    } else {
                        Wenku8Error.ErrorCode.LOCAL_BOOK_REMOVE_FAILED
                    }
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Wenku8Error.ErrorCode.BYTE_TO_STRING_EXCEPTION
            }
        }

        @Override
        protected fun onPostExecute(err: Wenku8Error.ErrorCode?) {
            super.onPostExecute(err)
            md.dismiss()
            if (err === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.bookshelf_removed), Toast.LENGTH_SHORT).show()
                if (fabFavorite != null) fabFavorite.setIcon(R.drawable.ic_favorate)
            } else Toast.makeText(this@NovelInfoActivity, err.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private inner class AsyncDownloadVolumes : AsyncTask<Array<Integer?>?, Integer?, Wenku8Error.ErrorCode?>() {
        private var md: MaterialDialog? = null
        private var loading = false
        private var size_a = 0
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            loading = true
            md = Builder(this@NovelInfoActivity)
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_content_downloading)
                    .progress(false, 1, true)
                    .cancelable(true)
                    .cancelListener { dialog -> loading = false }
                    .show()
            md.setProgress(0)
            md.setMaxProgress(1)
            size_a = 0
        }

        @Override
        protected fun doInBackground(vararg params: Array<Integer?>?): Wenku8Error.ErrorCode? {
            // params[0] is the index list
            var current = 0
            for (idxVolume in params[0]) {
                // calc size
                size_a += listVolume.get(idxVolume).chapterList.size()
                for (tempCi in listVolume.get(idxVolume).chapterList) {
                    try {
                        val cv: ContentValues = Wenku8API.getNovelContent(aid, tempCi.cid, GlobalConfig.getCurrentLang())

                        // load from local first
                        if (!loading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK // cancel
                        var xml: String = GlobalConfig.loadFullFileFromSaveFolder("novel", tempCi.cid + ".xml") // prevent empty file
                        if (xml.length() === 0) {
                            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                            // network error
                            xml = String(tempXml, "UTF-8")
                            if (xml.trim().length() === 0) return Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING
                            GlobalConfig.writeFullFileIntoSaveFolder("novel", tempCi.cid + ".xml", xml)
                        }

                        // cache image
                        if (GlobalConfig.doCacheImage()) {
                            val nc: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(xml)
                            for (i in 0 until nc.size()) {
                                if (nc[i].type === NovelContentType.IMAGE) {
                                    size_a++

                                    // save this images, judge exist first
                                    val imgFileName: String = GlobalConfig
                                            .generateImageFileNameByURL(nc[i].content)
                                    if (!LightCache.testFileExist(GlobalConfig.getFirstFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                                            && !LightCache.testFileExist(GlobalConfig.getSecondFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)) {
                                        // neither of the file exist
                                        val fileContent: ByteArray = LightNetwork.LightHttpDownload(nc[i].content)
                                                ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                                        // network error
                                        if (!LightCache.saveFile(GlobalConfig.getFirstFullSaveFilePath()
                                                        + GlobalConfig.imgsSaveFolderName + File.separator,
                                                        imgFileName, fileContent, true)) // fail
                                        // to first path
                                            LightCache.saveFile(GlobalConfig.getSecondFullSaveFilePath()
                                                    + GlobalConfig.imgsSaveFolderName + File.separator,
                                                    imgFileName, fileContent, true)
                                    }
                                    if (!loading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                                    publishProgress(++current) // update
                                    // progress
                                }
                            }
                        }
                        publishProgress(++current) // update progress
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            }
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onProgressUpdate(vararg values: Integer?) {
            super.onProgressUpdate(values)
            md.setMaxProgress(size_a)
            md.setProgress(values[0])
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            if (errorCode === Wenku8Error.ErrorCode.USER_CANCELLED_TASK) {
                // user cancelled
                Toast.makeText(this@NovelInfoActivity, R.string.system_manually_cancelled, Toast.LENGTH_LONG).show()
                if (md != null) md.dismiss()
                onResume()
                loading = false
                return
            } else if (errorCode === Wenku8Error.ErrorCode.NETWORK_ERROR) {
                Toast.makeText(this@NovelInfoActivity, getResources().getString(R.string.system_network_error), Toast.LENGTH_LONG).show()
                if (md != null) md.dismiss()
                onResume()
                loading = false
                return
            } else if (errorCode === Wenku8Error.ErrorCode.XML_PARSE_FAILED
                    || errorCode === Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING) {
                Toast.makeText(this@NovelInfoActivity, "Server returned strange data! (copyright reason?)", Toast.LENGTH_LONG).show()
                if (md != null) md.dismiss()
                onResume()
                loading = false
                return
            }

            // cache successfully
            Toast.makeText(this@NovelInfoActivity, "OK", Toast.LENGTH_LONG).show()
            loading = false
            if (md != null) md.dismiss()
            refreshInfoFromLocal()
        }
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // return from search activity
        val upArrow: Drawable = getResources().getDrawable(R.drawable.ic_svg_back)
        if (getSupportActionBar() != null && upArrow != null) {
            getSupportActionBar().setHomeButtonEnabled(true)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            upArrow.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
            getSupportActionBar().setHomeAsUpIndicator(upArrow)
        }

        // refresh when back from reader activity
        buildVolumeList()
    }

    private fun refreshInfoFromLocal() {
        isLoading = true
        spb.progressiveStart()
        val fetchInfoAsyncTask: FetchInfoAsyncTask = FetchInfoAsyncTask()
        fetchInfoAsyncTask.execute(1) // load from local
    }

    private fun refreshInfoFromCloud() {
        isLoading = true
        spb.progressiveStart()
        val fetchInfoAsyncTask: FetchInfoAsyncTask = FetchInfoAsyncTask()
        fetchInfoAsyncTask.execute()
    }
}