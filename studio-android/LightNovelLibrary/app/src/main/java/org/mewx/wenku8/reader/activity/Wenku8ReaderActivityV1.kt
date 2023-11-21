package org.mewx.wenku8.reader.activity

import android.app.Activity

/**
 * Created by MewX on 2015/7/10.
 * Novel Reader Engine V1.
 */
class Wenku8ReaderActivityV1 : BaseMaterialActivity() {
    // vars
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    private var from: String? = ""
    private var aid = 0
    private var cid = 0
    private var forcejump: String? = null
    private var volumeList: VolumeList? = null
    private var nc: List<OldNovelContentParser.NovelContent?>? = ArrayList()
    private var mSliderHolder: RelativeLayout? = null
    private var sl: SlidingLayout? = null

    //    private int tempNavBarHeight;
    // components
    private var mSlidingPageAdapter: SlidingPageAdapter? = null
    private var loader: WenkuReaderLoader? = null
    private var setting: WenkuReaderSettingV1? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_reader_swipe_temp, BaseMaterialActivity.StatusBarColor.DARK)

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)
        volumeList = getIntent().getSerializableExtra("volume") as VolumeList
        cid = getIntent().getIntExtra("cid", 1)
        from = getIntent().getStringExtra("from")
        forcejump = getIntent().getStringExtra("forcejump")
        if (forcejump == null || forcejump.length() === 0) forcejump = "no"
        //        tempNavBarHeight = LightTool.getNavigationBarSize(this).y;

        // Analysis.
        val readerParams = Bundle()
        readerParams.putString(FirebaseAnalytics.Param.ITEM_ID, "" + aid)
        readerParams.putString("chapter_id", "" + cid)
        readerParams.putString("from", from)
        readerParams.putString("jump_to_saved_page", forcejump)
        mFirebaseAnalytics.logEvent("reader_v1", readerParams)
        getTintManager().setTintAlpha(0.0f)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(volumeList.volumeName)
        }

        // find views
        mSliderHolder = findViewById(R.id.slider_holder)

        // UIL setting
        if (ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this)
        }

        // async tasks
        val cv: ContentValues = Wenku8API.getNovelContent(aid, cid, GlobalConfig.getCurrentLang())
        val ast: AsyncNovelContentTask = AsyncNovelContentTask()
        ast.execute(cv)
    }

    @Override
    protected fun onResume() {
        super.onResume()
        if (findViewById(R.id.reader_bot).getVisibility() !== View.VISIBLE) hideNavigationBar() else showNavigationBar()
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_reader_v1, menu)
        val drawable: Drawable = menu.getItem(0).getIcon()
        if (drawable != null) {
            drawable.mutate()
            drawable.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
        }
        return true
    }

    @Override
    fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Update display cutout area.
        if (Build.VERSION.SDK_INT >= 28) {
            val cutout: DisplayCutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout()
            if (cutout != null) {
                LightTool.setDisplayCutout(
                        Rect(cutout.getSafeInsetLeft(),
                                cutout.getSafeInsetTop(),
                                cutout.getSafeInsetRight(),
                                cutout.getSafeInsetBottom()))
            }
        }
    }

    private fun hideNavigationBar() {
        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
            val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            getWindow().getDecorView().setSystemUiVisibility(flags)

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView: View = getWindow().getDecorView()
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN === 0) {
                    decorView.setSystemUiVisibility(flags)
                }
            }
        }
    }

    private fun showNavigationBar() {
        // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
        val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        // This work only for android 4.4+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags)

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            val decorView: View = getWindow().getDecorView()
            decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN === 0) {
                    decorView.setSystemUiVisibility(flags)
                }
            }
        }
    }

    @Override
    protected fun onPause() {
        super.onPause()

        // save record
        if (mSlidingPageAdapter != null && loader != null) {
            loader.setCurrentIndex(mSlidingPageAdapter.getCurrentLastLineIndex())
            if (volumeList.chapterList.size() > 1 && volumeList.chapterList.get(volumeList.chapterList.size() - 1).cid === cid && mSlidingPageAdapter.getCurrentLastWordIndex() == loader.getCurrentStringLength() - 1) GlobalConfig.removeReadSavesRecordV1(aid) else GlobalConfig.addReadSavesRecordV1(aid, volumeList.vid, cid, mSlidingPageAdapter.getCurrentFirstLineIndex(), mSlidingPageAdapter.getCurrentFirstWordIndex())
        }
    }

    @Override
    fun dispatchKeyEvent(@NonNull event: KeyEvent?): Boolean {
        when (event.getKeyCode()) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                gotoNextPage()
                return true
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                gotoPreviousPage()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    internal inner class SlidingPageAdapter(begLineIndex: Int, begWordIndex: Int) : SlidingAdapter<WenkuReaderPageView?>() {
        var firstLineIndex = 0 // line index of first index of this page
        var firstWordIndex = 0 // first index of this page
        var lastLineIndex = 0 // line index of last index of this page
        var lastWordIndex = 0 // last index of this page
        var nextPage: WenkuReaderPageView? = null
        var previousPage: WenkuReaderPageView? = null
        var isLoadingNext = false
        var isLoadingPrevious = false

        init {

            // init values
            firstLineIndex = begLineIndex
            firstWordIndex = begWordIndex

            // check valid first
            if (firstLineIndex + 1 >= loader.getElementCount()) firstLineIndex = loader.getElementCount() - 1 // to last one
            loader.setCurrentIndex(firstLineIndex)
            if (firstWordIndex + 1 >= loader.getCurrentStringLength()) {
                firstLineIndex--
                firstWordIndex = 0
                if (firstLineIndex < 0) firstLineIndex = 0
            }
        }

        @Override
        fun getView(contentView: View?, pageView: WenkuReaderPageView?): View? {
            var contentView: View? = contentView
            Log.d("MewX", "-- slider getView")
            if (contentView == null) contentView = getLayoutInflater().inflate(R.layout.layout_reader_swipe_page, null)

            // prevent memory leak
            val rl: RelativeLayout = contentView.findViewById(R.id.page_holder)
            rl.removeAllViews()
            val lp: ViewGroup.LayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            rl.addView(pageView, lp)
            return contentView
        }

        fun getCurrentFirstLineIndex(): Int {
            return firstLineIndex
        }

        fun getCurrentFirstWordIndex(): Int {
            return firstWordIndex
        }

        fun getCurrentLastLineIndex(): Int {
            return lastLineIndex
        }

        fun getCurrentLastWordIndex(): Int {
            return lastWordIndex
        }

        fun setCurrentIndex(lineIndex: Int, wordIndex: Int) {
            firstLineIndex = if (lineIndex + 1 >= loader.getElementCount()) loader.getElementCount() - 1 else lineIndex
            loader.setCurrentIndex(firstLineIndex)
            firstWordIndex = if (wordIndex + 1 >= loader.getCurrentStringLength()) loader.getCurrentStringLength() - 1 else wordIndex
            val temp = WenkuReaderPageView(this@Wenku8ReaderActivityV1, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.CURRENT)
            firstLineIndex = temp.getFirstLineIndex()
            firstWordIndex = temp.getFirstWordIndex()
            lastLineIndex = temp.getLastLineIndex()
            lastWordIndex = temp.getLastWordIndex()
        }

        @Override
        operator fun hasNext(): Boolean {
            Log.d("MewX", "-- slider hasNext")
            loader.setCurrentIndex(lastLineIndex)
            return !isLoadingNext && loader.hasNext(lastWordIndex)
        }

        @Override
        protected fun computeNext() {
            Log.d("MewX", "-- slider computeNext")
            // vars change to next
            //if(nextPage == null) return;
            nextPage = WenkuReaderPageView(this@Wenku8ReaderActivityV1, lastLineIndex, lastWordIndex, WenkuReaderPageView.LOADING_DIRECTION.FORWARDS)
            firstLineIndex = nextPage.getFirstLineIndex()
            firstWordIndex = nextPage.getFirstWordIndex()
            lastLineIndex = nextPage.getLastLineIndex()
            lastWordIndex = nextPage.getLastWordIndex()
            printLog()
        }

        @Override
        protected fun computePrevious() {
            Log.d("MewX", "-- slider computePrevious")
            // vars change to previous
//            if(previousPage == null) return;
//            loader.setCurrentIndex(firstLineIndex);
            val previousPage = WenkuReaderPageView(this@Wenku8ReaderActivityV1, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.BACKWARDS)
            firstLineIndex = previousPage.getFirstLineIndex()
            firstWordIndex = previousPage.getFirstWordIndex()
            lastLineIndex = previousPage.getLastLineIndex()
            lastWordIndex = previousPage.getLastWordIndex()

            // reset first page
//            if(firstLineIndex == 0 && firstWordIndex == 0)
//                notifyDataSetChanged();
            printLog()
        }

        @Override
        fun getNext(): WenkuReaderPageView? {
            Log.d("MewX", "-- slider getNext")
            nextPage = WenkuReaderPageView(this@Wenku8ReaderActivityV1, lastLineIndex, lastWordIndex, WenkuReaderPageView.LOADING_DIRECTION.FORWARDS)
            return nextPage
        }

        @Override
        fun hasPrevious(): Boolean {
            Log.d("MewX", "-- slider hasPrevious")
            loader.setCurrentIndex(firstLineIndex)
            return !isLoadingPrevious && loader.hasPrevious(firstWordIndex)
        }

        @Override
        fun getPrevious(): WenkuReaderPageView? {
            Log.d("MewX", "-- slider getPrevious")
            previousPage = WenkuReaderPageView(this@Wenku8ReaderActivityV1, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.BACKWARDS)
            return previousPage
        }

        @Override
        fun getCurrent(): WenkuReaderPageView? {
            Log.d("MewX", "-- slider getCurrent")
            val temp = WenkuReaderPageView(this@Wenku8ReaderActivityV1, firstLineIndex, firstWordIndex, WenkuReaderPageView.LOADING_DIRECTION.CURRENT)
            firstLineIndex = temp.getFirstLineIndex()
            firstWordIndex = temp.getFirstWordIndex()
            lastLineIndex = temp.getLastLineIndex()
            lastWordIndex = temp.getLastWordIndex()
            printLog()
            return temp
        }

        private fun printLog() {
            Log.d("MewX", "saved index: " + firstLineIndex + "(" + firstWordIndex + ") -> " + lastLineIndex + "(" + lastWordIndex + ") | Total: " + loader.getCurrentIndex() + " of " + (loader.getElementCount() - 1))
        }
    }

    internal inner class AsyncNovelContentTask : AsyncTask<ContentValues?, Integer?, Wenku8Error.ErrorCode?>() {
        private var md: MaterialDialog? = null
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            md = Builder(this@Wenku8ReaderActivityV1)
                    .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                    .title(R.string.reader_please_wait)
                    .content(R.string.reader_engine_v1_parsing)
                    .progress(true, 0)
                    .cancelable(false)
                    .show()
        }

        @Override
        protected fun doInBackground(vararg params: ContentValues?): Wenku8Error.ErrorCode? {
            return try {
                val xml: String
                xml = if (from.equals(FromLocal)) // or exist
                    GlobalConfig.loadFullFileFromSaveFolder("novel", "$cid.xml") else {
                    val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, params[0])
                            ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                    String(tempXml, "UTF-8")
                }
                nc = OldNovelContentParser.parseNovelContent(xml, null)
                if (nc.size() === 0) if (xml.length() === 0) Wenku8Error.ErrorCode.SERVER_RETURN_NOTHING else Wenku8Error.ErrorCode.XML_PARSE_FAILED else Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Wenku8Error.ErrorCode.STRING_CONVERSION_ERROR
            }
        }

        @Override
        protected fun onPostExecute(result: Wenku8Error.ErrorCode?) {
            if (result !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                Toast.makeText(this@Wenku8ReaderActivityV1, result.toString(), Toast.LENGTH_LONG).show()
                if (md != null) md.dismiss()
                this@Wenku8ReaderActivityV1.finish() // return friendly
                return
            }
            Log.d("MewX", "-- 小说获取完成")

            // init components
            loader = WenkuReaderLoaderXML(nc)
            setting = WenkuReaderSettingV1()
            loader.setCurrentIndex(0)
            for (ci in volumeList.chapterList) {
                // get chapter name
                if (ci.cid === cid) {
                    loader.setChapterName(ci.chapterName)
                    break
                }
            }

            // config sliding layout
            mSlidingPageAdapter = SlidingPageAdapter(0, 0)
            WenkuReaderPageView.setViewComponents(loader, setting, false)
            Log.d("MewX", "-- loader, setting 初始化完成")
            sl = SlidingLayout(this@Wenku8ReaderActivityV1)
            val lp: ViewGroup.LayoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            sl.setAdapter(mSlidingPageAdapter)
            sl.setSlider(OverlappedSlider())
            sl.setOnTapListener(object : OnTapListener() {
                var barStatus = false
                var isSet = false
                @Override
                fun onSingleTap(event: MotionEvent?) {
                    val screenWidth: Int = getResources().getDisplayMetrics().widthPixels
                    val screenHeight: Int = getResources().getDisplayMetrics().heightPixels
                    val x = event.getX() as Int
                    val y = event.getY() as Int
                    if (x > screenWidth / 3 && x < screenWidth * 2 / 3 && y > screenHeight / 3 && y < screenHeight * 2 / 3) {
                        // first init
                        if (!barStatus) {
                            showNavigationBar()
                            findViewById(R.id.reader_top).setVisibility(View.VISIBLE)
                            findViewById(R.id.reader_bot).setVisibility(View.VISIBLE)
                            getTintManager().setStatusBarAlpha(0.90f)
                            getTintManager().setNavigationBarAlpha(0.80f) // TODO: fix bug
                            barStatus = true
                            if (!isSet) {
                                // add action to each
                                findViewById(R.id.btn_daylight).setOnClickListener { v ->
                                    // switch day/night mode
                                    WenkuReaderPageView.switchDayMode()
                                    WenkuReaderPageView.resetTextColor()
                                    mSlidingPageAdapter.restoreState(null, null)
                                    mSlidingPageAdapter.notifyDataSetChanged()
                                }
                                findViewById(R.id.btn_daylight).setOnLongClickListener { v ->
                                    Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_daynight), Toast.LENGTH_SHORT).show()
                                    true
                                }
                                findViewById(R.id.btn_jump).setOnClickListener(object : OnClickListener() {
                                    var isOpen = false
                                    @Override
                                    fun onClick(v: View?) {
                                        // show jump dialog
                                        if (findViewById(R.id.reader_bot_settings).getVisibility() === View.VISIBLE
                                                || findViewById(R.id.reader_bot_seeker).getVisibility() === View.INVISIBLE) {
                                            isOpen = false
                                            findViewById(R.id.reader_bot_settings).setVisibility(View.INVISIBLE)
                                        }
                                        if (!isOpen) findViewById(R.id.reader_bot_seeker).setVisibility(View.VISIBLE) else findViewById(R.id.reader_bot_seeker).setVisibility(View.INVISIBLE)
                                        isOpen = !isOpen
                                        val seeker: DiscreteSeekBar = findViewById(R.id.reader_seekbar)
                                        seeker.setMin(1)
                                        seeker.setProgress(mSlidingPageAdapter.getCurrentFirstLineIndex() + 1) // bug here
                                        seeker.setMax(loader.getElementCount())
                                        seeker.setOnProgressChangeListener(object : OnProgressChangeListener() {
                                            @Override
                                            fun onProgressChanged(discreteSeekBar: DiscreteSeekBar?, i: Int, b: Boolean) {
                                            }

                                            @Override
                                            fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                            }

                                            @Override
                                            fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                                mSlidingPageAdapter.setCurrentIndex(discreteSeekBar.getProgress() - 1, 0)
                                                mSlidingPageAdapter.restoreState(null, null)
                                                mSlidingPageAdapter.notifyDataSetChanged()
                                            }
                                        })
                                    }
                                })
                                findViewById(R.id.btn_jump).setOnLongClickListener { v ->
                                    Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_jump), Toast.LENGTH_SHORT).show()
                                    true
                                }
                                findViewById(R.id.btn_find).setOnClickListener { v ->
                                    // show label page
                                    Toast.makeText(this@Wenku8ReaderActivityV1, "查找功能尚未就绪", Toast.LENGTH_SHORT).show()
                                }
                                findViewById(R.id.btn_find).setOnLongClickListener { v ->
                                    Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_find), Toast.LENGTH_SHORT).show()
                                    true
                                }
                                findViewById(R.id.btn_config).setOnClickListener(object : OnClickListener() {
                                    private var isOpen = false
                                    @Override
                                    fun onClick(v: View?) {
                                        // show jump dialog
                                        if (findViewById(R.id.reader_bot_seeker).getVisibility() === View.VISIBLE
                                                || findViewById(R.id.reader_bot_settings).getVisibility() === View.INVISIBLE) {
                                            isOpen = false
                                            findViewById(R.id.reader_bot_seeker).setVisibility(View.INVISIBLE)
                                        }
                                        if (!isOpen) findViewById(R.id.reader_bot_settings).setVisibility(View.VISIBLE) else findViewById(R.id.reader_bot_settings).setVisibility(View.INVISIBLE)
                                        isOpen = !isOpen

                                        // set all listeners
                                        val seekerFontSize: DiscreteSeekBar = findViewById(R.id.reader_font_size_seeker)
                                        val seekerLineDistance: DiscreteSeekBar = findViewById(R.id.reader_line_distance_seeker)
                                        val seekerParagraphDistance: DiscreteSeekBar = findViewById(R.id.reader_paragraph_distance_seeker)
                                        val seekerParagraphEdgeDistance: DiscreteSeekBar = findViewById(R.id.reader_paragraph_edge_distance_seeker)
                                        seekerFontSize.setProgress(setting.getFontSize())
                                        seekerFontSize.setOnProgressChangeListener(object : OnProgressChangeListener() {
                                            @Override
                                            fun onProgressChanged(discreteSeekBar: DiscreteSeekBar?, i: Int, b: Boolean) {
                                            }

                                            @Override
                                            fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                            }

                                            @Override
                                            fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                                setting.setFontSize(discreteSeekBar.getProgress())
                                                WenkuReaderPageView.setViewComponents(loader, setting, false)
                                                mSlidingPageAdapter.restoreState(null, null)
                                                mSlidingPageAdapter.notifyDataSetChanged()
                                            }
                                        })
                                        seekerLineDistance.setProgress(setting.getLineDistance())
                                        seekerLineDistance.setOnProgressChangeListener(object : OnProgressChangeListener() {
                                            @Override
                                            fun onProgressChanged(discreteSeekBar: DiscreteSeekBar?, i: Int, b: Boolean) {
                                            }

                                            @Override
                                            fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                            }

                                            @Override
                                            fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                                setting.setLineDistance(discreteSeekBar.getProgress())
                                                WenkuReaderPageView.setViewComponents(loader, setting, false)
                                                mSlidingPageAdapter.restoreState(null, null)
                                                mSlidingPageAdapter.notifyDataSetChanged()
                                            }
                                        })
                                        seekerParagraphDistance.setProgress(setting.getParagraphDistance())
                                        seekerParagraphDistance.setOnProgressChangeListener(object : OnProgressChangeListener() {
                                            @Override
                                            fun onProgressChanged(discreteSeekBar: DiscreteSeekBar?, i: Int, b: Boolean) {
                                            }

                                            @Override
                                            fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                            }

                                            @Override
                                            fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                                setting.setParagraphDistance(discreteSeekBar.getProgress())
                                                WenkuReaderPageView.setViewComponents(loader, setting, false)
                                                mSlidingPageAdapter.restoreState(null, null)
                                                mSlidingPageAdapter.notifyDataSetChanged()
                                            }
                                        })
                                        seekerParagraphEdgeDistance.setProgress(setting.getPageEdgeDistance())
                                        seekerParagraphEdgeDistance.setOnProgressChangeListener(object : OnProgressChangeListener() {
                                            @Override
                                            fun onProgressChanged(discreteSeekBar: DiscreteSeekBar?, i: Int, b: Boolean) {
                                            }

                                            @Override
                                            fun onStartTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                            }

                                            @Override
                                            fun onStopTrackingTouch(discreteSeekBar: DiscreteSeekBar?) {
                                                setting.setPageEdgeDistance(discreteSeekBar.getProgress())
                                                WenkuReaderPageView.setViewComponents(loader, setting, false)
                                                mSlidingPageAdapter.restoreState(null, null)
                                                mSlidingPageAdapter.notifyDataSetChanged()
                                            }
                                        })
                                        findViewById(R.id.btn_custom_font).setOnClickListener { v1 ->
                                            Builder(this@Wenku8ReaderActivityV1)
                                                    .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                                    .title(R.string.reader_custom_font)
                                                    .items(R.array.reader_font_option)
                                                    .itemsCallback { dialog, view, which, text ->
                                                        when (which) {
                                                            0 -> {
                                                                // system default
                                                                setting.setUseCustomFont(false)
                                                                WenkuReaderPageView.setViewComponents(loader, setting, false)
                                                                mSlidingPageAdapter.restoreState(null, null)
                                                                mSlidingPageAdapter.notifyDataSetChanged()
                                                            }

                                                            1 ->                                                             // choose a ttf/otf file
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                                    val intent = Intent()
                                                                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT)
                                                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                                                    intent.setType("font/*")
                                                                    startActivityForResult(intent, REQUEST_FONT_PICKER)
                                                                } else {
                                                                    val i = Intent(this@Wenku8ReaderActivityV1, FilePickerActivity::class.java)
                                                                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                                                                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                                                                    i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE)
                                                                    i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                                                            if (GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() === 0) Environment.getExternalStorageDirectory().getPath() else GlobalConfig.pathPickedSave)
                                                                    startActivityForResult(i, REQUEST_FONT_PICKER_LEGACY)
                                                                }
                                                        }
                                                    }
                                                    .show()
                                        }
                                        findViewById(R.id.btn_custom_background).setOnClickListener { v12 ->
                                            Builder(this@Wenku8ReaderActivityV1)
                                                    .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                                    .title(R.string.reader_custom_background)
                                                    .items(R.array.reader_background_option)
                                                    .itemsCallback { dialog, view, which, text ->
                                                        when (which) {
                                                            0 -> {
                                                                // system default
                                                                setting.setPageBackgroundType(WenkuReaderSettingV1.PAGE_BACKGROUND_TYPE.SYSTEM_DEFAULT)
                                                                WenkuReaderPageView.setViewComponents(loader, setting, true)
                                                                mSlidingPageAdapter.restoreState(null, null)
                                                                mSlidingPageAdapter.notifyDataSetChanged()
                                                            }

                                                            1 ->                                                             // choose a image file
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                                    val intent = Intent()
                                                                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT)
                                                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                                                    intent.setType("image/*")
                                                                    startActivityForResult(intent, REQUEST_IMAGE_PICKER)
                                                                } else {
                                                                    val i = Intent(this@Wenku8ReaderActivityV1, FilePickerActivity::class.java)
                                                                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                                                                    i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                                                                    i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE)
                                                                    i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                                                            if (GlobalConfig.pathPickedSave == null || GlobalConfig.pathPickedSave.length() === 0) Environment.getExternalStorageDirectory().getPath() else GlobalConfig.pathPickedSave)
                                                                    startActivityForResult(i, REQUEST_IMAGE_PICKER_LEGACY)
                                                                }
                                                        }
                                                    }
                                                    .show()
                                        }
                                    }
                                })
                                findViewById(R.id.btn_config).setOnLongClickListener { v ->
                                    Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_config), Toast.LENGTH_SHORT).show()
                                    true
                                }
                                findViewById(R.id.text_previous).setOnClickListener { v ->
                                    // goto previous chapter
                                    for (i in 0 until volumeList.chapterList.size()) {
                                        if (cid == volumeList.chapterList.get(i).cid) {
                                            // found self
                                            if (i == 0) {
                                                // no more previous
                                                Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_already_first_chapter), Toast.LENGTH_SHORT).show()
                                            } else {
                                                // jump to previous
                                                val i_bak: Int = i
                                                Builder(this@Wenku8ReaderActivityV1)
                                                        .onPositive { dialog, which ->
                                                            val intent = Intent(this@Wenku8ReaderActivityV1, Wenku8ReaderActivityV1::class.java)
                                                            intent.putExtra("aid", aid)
                                                            intent.putExtra("volume", volumeList)
                                                            intent.putExtra("cid", volumeList.chapterList.get(i_bak - 1).cid)
                                                            intent.putExtra("from", from) // from cloud
                                                            startActivity(intent)
                                                            overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                                                            this@Wenku8ReaderActivityV1.finish()
                                                        }
                                                        .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                                        .title(R.string.dialog_sure_to_jump_chapter)
                                                        .content(volumeList.chapterList.get(i_bak - 1).chapterName)
                                                        .contentGravity(GravityEnum.CENTER)
                                                        .positiveText(R.string.dialog_positive_yes)
                                                        .negativeText(R.string.dialog_negative_no)
                                                        .show()
                                            }
                                            break
                                        }
                                    }
                                }
                                findViewById(R.id.text_next).setOnClickListener { v ->
                                    // goto next chapter
                                    for (i in 0 until volumeList.chapterList.size()) {
                                        if (cid == volumeList.chapterList.get(i).cid) {
                                            // found self
                                            if (i + 1 >= volumeList.chapterList.size()) {
                                                // no more previous
                                                Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_already_last_chapter), Toast.LENGTH_SHORT).show()
                                            } else {
                                                // jump to previous
                                                val i_bak: Int = i
                                                Builder(this@Wenku8ReaderActivityV1)
                                                        .onPositive { dialog, which ->
                                                            val intent = Intent(this@Wenku8ReaderActivityV1, Wenku8ReaderActivityV1::class.java)
                                                            intent.putExtra("aid", aid)
                                                            intent.putExtra("volume", volumeList)
                                                            intent.putExtra("cid", volumeList.chapterList.get(i_bak + 1).cid)
                                                            intent.putExtra("from", from) // from cloud
                                                            startActivity(intent)
                                                            overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                                                            this@Wenku8ReaderActivityV1.finish()
                                                        }
                                                        .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                                        .title(R.string.dialog_sure_to_jump_chapter)
                                                        .content(volumeList.chapterList.get(i_bak + 1).chapterName)
                                                        .contentGravity(GravityEnum.CENTER)
                                                        .positiveText(R.string.dialog_positive_yes)
                                                        .negativeText(R.string.dialog_negative_no)
                                                        .show()
                                            }
                                            break
                                        }
                                    }
                                }
                            }
                        } else {
                            // show menu
                            hideNavigationBar()
                            findViewById(R.id.reader_top).setVisibility(View.INVISIBLE)
                            findViewById(R.id.reader_bot).setVisibility(View.INVISIBLE)
                            findViewById(R.id.reader_bot_seeker).setVisibility(View.INVISIBLE)
                            findViewById(R.id.reader_bot_settings).setVisibility(View.INVISIBLE)
                            getTintManager().setStatusBarAlpha(0.0f)
                            getTintManager().setNavigationBarAlpha(0.0f)
                            barStatus = false
                        }
                        return
                    }
                    if (x > screenWidth / 2) {
                        gotoNextPage()
                    } else if (x <= screenWidth / 2) {
                        gotoPreviousPage()
                    }
                }
            })
            mSliderHolder.addView(sl, 0, lp)
            Log.d("MewX", "-- slider创建完毕")

            // end loading dialog
            if (md != null) md.dismiss()

            // show dialog, jump to last read position
            if (GlobalConfig.getReadSavesRecordV1(aid) != null) {
                val rs: GlobalConfig.ReadSavesV1 = GlobalConfig.getReadSavesRecordV1(aid)
                if (rs != null && rs.vid === volumeList.vid && rs.cid === cid) {
                    if (forcejump.equals("yes")) {
                        mSlidingPageAdapter.setCurrentIndex(rs.lineId, rs.wordId)
                        mSlidingPageAdapter.restoreState(null, null)
                        mSlidingPageAdapter.notifyDataSetChanged()
                    } else if (mSlidingPageAdapter.getCurrentFirstLineIndex() != rs.lineId ||
                            mSlidingPageAdapter.getCurrentFirstWordIndex() != rs.wordId) {
                        // Popping up jump dialog only when the user didn't exist at the first page.
                        Builder(this@Wenku8ReaderActivityV1)
                                .onPositive { dialog, which ->
                                    mSlidingPageAdapter.setCurrentIndex(rs.lineId, rs.wordId)
                                    mSlidingPageAdapter.restoreState(null, null)
                                    mSlidingPageAdapter.notifyDataSetChanged()
                                }
                                .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                .title(R.string.reader_v1_notice)
                                .content(R.string.reader_jump_last)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_sure)
                                .negativeText(R.string.dialog_negative_biao)
                                .show()
                    }
                }
            }
        }
    }

    private fun gotoNextPage() {
        if (mSlidingPageAdapter != null && !mSlidingPageAdapter.hasNext()) {
            // goto next chapter
            for (i in 0 until volumeList.chapterList.size()) {
                if (cid == volumeList.chapterList.get(i).cid) {
                    // found self
                    if (i + 1 >= volumeList.chapterList.size()) {
                        // no more previous
                        Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_already_last_chapter), Toast.LENGTH_SHORT).show()
                    } else {
                        // jump to previous
                        val i_bak: Int = i
                        Builder(this@Wenku8ReaderActivityV1)
                                .onPositive { dialog, which ->
                                    val intent = Intent(this@Wenku8ReaderActivityV1, Wenku8ReaderActivityV1::class.java)
                                    intent.putExtra("aid", aid)
                                    intent.putExtra("volume", volumeList)
                                    intent.putExtra("cid", volumeList.chapterList.get(i_bak + 1).cid)
                                    intent.putExtra("from", from) // from cloud
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                                    this@Wenku8ReaderActivityV1.finish()
                                }
                                .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                .title(R.string.dialog_sure_to_jump_chapter)
                                .content(volumeList.chapterList.get(i_bak + 1).chapterName)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_yes)
                                .negativeText(R.string.dialog_negative_no)
                                .show()
                    }
                    break
                }
            }
        } else {
            if (sl != null) sl.slideNext()
        }
    }

    private fun gotoPreviousPage() {
        if (mSlidingPageAdapter != null && !mSlidingPageAdapter.hasPrevious()) {
            // goto previous chapter
            for (i in 0 until volumeList.chapterList.size()) {
                if (cid == volumeList.chapterList.get(i).cid) {
                    // found self
                    if (i == 0) {
                        // no more previous
                        Toast.makeText(this@Wenku8ReaderActivityV1, getResources().getString(R.string.reader_already_first_chapter), Toast.LENGTH_SHORT).show()
                    } else {
                        // jump to previous
                        val i_bak: Int = i
                        Builder(this@Wenku8ReaderActivityV1)
                                .onPositive { dialog, which ->
                                    val intent = Intent(this@Wenku8ReaderActivityV1, Wenku8ReaderActivityV1::class.java)
                                    intent.putExtra("aid", aid)
                                    intent.putExtra("volume", volumeList)
                                    intent.putExtra("cid", volumeList.chapterList.get(i_bak - 1).cid)
                                    intent.putExtra("from", from) // from cloud
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                                    this@Wenku8ReaderActivityV1.finish()
                                }
                                .theme(if (WenkuReaderPageView.getInDayMode()) Theme.LIGHT else Theme.DARK)
                                .title(R.string.dialog_sure_to_jump_chapter)
                                .content(volumeList.chapterList.get(i_bak - 1).chapterName)
                                .contentGravity(GravityEnum.CENTER)
                                .positiveText(R.string.dialog_positive_yes)
                                .negativeText(R.string.dialog_negative_no)
                                .show()
                    }
                    break
                }
            }
        } else {
            if (sl != null) sl.slidePrevious()
        }
    }

    @Override
    protected fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FONT_PICKER_LEGACY && resultCode == Activity.RESULT_OK) {
            // get ttf path
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                val clip: ClipData = data.getClipData()
                if (clip != null) {
                    for (i in 0 until clip.getItemCount()) {
                        val uri: Uri = clip.getItemAt(i).getUri()
                        // Do something with the URI
                        runSaveCustomFontPath(uri.toString().replaceAll("file://", ""))
                    }
                }
            } else {
                val uri: Uri = data.getData()
                // Do something with the URI
                runSaveCustomFontPath(uri.toString().replaceAll("file://", ""))
            }
        } else if (requestCode == REQUEST_IMAGE_PICKER_LEGACY && resultCode == Activity.RESULT_OK) {
            // get image path
            if (data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                val clip: ClipData = data.getClipData()
                if (clip != null) {
                    for (i in 0 until clip.getItemCount()) {
                        val uri: Uri = clip.getItemAt(i).getUri()
                        // Do something with the URI
                        runSaveCustomBackgroundPath(uri.toString().replaceAll("file://", ""))
                    }
                }
            } else {
                val uri: Uri = data.getData()
                // Do something with the URI
                runSaveCustomBackgroundPath(uri.toString().replaceAll("file://", ""))
            }
        } else if (requestCode == REQUEST_FONT_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val fontUri: Uri = data.getData()
            val copiedFilePath: String = (GlobalConfig.getDefaultStoragePath() + GlobalConfig.customFolderName + File.separator).toString() + "reader_font"
            try {
                LightCache.copyFile(getApplicationContext().getContentResolver().openInputStream(fontUri), copiedFilePath, true)
                runSaveCustomFontPath(copiedFilePath.replaceAll("file://", ""))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Exception: $e", Toast.LENGTH_SHORT).show()
                // Failed to copy. Just ignore it.
            }
        } else if (requestCode == REQUEST_IMAGE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            val mediaUri: Uri = data.getData()
            val copiedFilePath: String = (GlobalConfig.getDefaultStoragePath() + GlobalConfig.customFolderName + File.separator).toString() + "reader_background"
            try {
                LightCache.copyFile(getApplicationContext().getContentResolver().openInputStream(mediaUri), copiedFilePath, true)
                runSaveCustomBackgroundPath(copiedFilePath.replaceAll("file://", ""))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Exception: $e", Toast.LENGTH_SHORT).show()
                // Failed to copy. Just ignore it.
            }
        }
    }

    private fun runSaveCustomFontPath(path: String?) {
        setting.setCustomFontPath(path)
        WenkuReaderPageView.setViewComponents(loader, setting, false)
        mSlidingPageAdapter.restoreState(null, null)
        mSlidingPageAdapter.notifyDataSetChanged()
    }

    private fun runSaveCustomBackgroundPath(path: String?) {
        try {
            BitmapFactory.decodeFile(path)
        } catch (oome: OutOfMemoryError) {
            try {
                val options: BitmapFactory.Options = Options()
                options.inSampleSize = 2
                val bitmap: Bitmap = BitmapFactory.decodeFile(path, options)
                        ?: throw Exception("PictureDecodeFailedException")
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Exception: $e\n可能的原因有：图片不在内置SD卡；图片格式不正确；图片像素尺寸太大，请使用小一点的图，谢谢，此功能为试验性功能；", Toast.LENGTH_LONG).show()
                return
            }
        }
        setting.setPageBackgroundCustomPath(path)
        WenkuReaderPageView.setViewComponents(loader, setting, true)
        mSlidingPageAdapter.restoreState(null, null)
        mSlidingPageAdapter.notifyDataSetChanged()
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        when (menuItem.getItemId()) {
            android.R.id.home -> onBackPressed()
            R.id.action_watch_image -> if (sl != null && sl.getAdapter().getCurrentView() != null && (sl.getAdapter().getCurrentView() as RelativeLayout).getChildAt(0) is WenkuReaderPageView) ((sl.getAdapter().getCurrentView() as RelativeLayout).getChildAt(0) as WenkuReaderPageView).watchImageDetailed(this)
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }

    companion object {
        // constant
        private val FromLocal: String? = "fav"
        private const val REQUEST_FONT_PICKER_LEGACY = 0
        private const val REQUEST_IMAGE_PICKER_LEGACY = 1
        private const val REQUEST_FONT_PICKER = 100
        private const val REQUEST_IMAGE_PICKER = 101
    }
}