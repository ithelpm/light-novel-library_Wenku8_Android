package org.mewx.wenku8.activity

import android.content.ContentValues

/**
 * Created by MewX on 2015/6/6.
 * Old reader engine.
 */
class VerticalReaderActivity : AppCompatActivity() {
    // private vars
    private var from: String? = ""
    private var aid = 0
    private var cid = 0
    private var volumeList: VolumeList? = null // for extended function
    private var pDialog: MaterialDialog? = null
    private var svTextListLayout: ScrollViewNoFling? = null
    private var TextListLayout: LinearLayout? = null
    private var nc: List<OldNovelContentParser.NovelContent?>? = null

    // Scroll runnable to last read position
    private val runnableScroll: Runnable? = object : Runnable() {
        @Override
        fun run() {
            this@VerticalReaderActivity.findViewById(R.id.content_scrollview)
                    .scrollTo(0, GlobalConfig.getReadSavesRecord(cid, TextListLayout.getMeasuredHeight()))
        }
    }

    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.layout_vertical_reader_temp)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)
        volumeList = getIntent().getSerializableExtra("volume") as VolumeList
        cid = getIntent().getIntExtra("cid", 1)
        from = getIntent().getStringExtra("from")

        // UIL setting
        if (ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this)
        }

        // get Novel Content
        getNovelContent()

        // get view
        TextListLayout = findViewById(R.id.novel_content_layout)
        svTextListLayout = findViewById(R.id.content_scrollview)
        svTextListLayout.setOnTouchListener(object : OnTouchListener() {
            // Gesture detector
            var gestureDetector: GestureDetector? = GestureDetector(this@VerticalReaderActivity, object : OnGestureListener() {
                @Override
                fun onDown(e: MotionEvent?): Boolean {
                    return false
                }

                @Override
                fun onShowPress(e: MotionEvent?) {
                }

                @Override
                fun onSingleTapUp(e: MotionEvent?): Boolean {
                    val screenHeight: Int = getResources().getDisplayMetrics().heightPixels
                    val y = e.getY() as Int
                    if (y < screenHeight * 5 / 6 && y >= screenHeight / 2) {
                        // move down
                        svTextListLayout.smoothScrollBy(0, screenHeight / 2)
                        return true
                    } else if (y < screenHeight / 2 && y > screenHeight / 6) {
                        // move up
                        svTextListLayout.smoothScrollBy(0, -screenHeight / 2)
                        return true
                    }
                    return false
                }

                @Override
                fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                    return false
                }

                @Override
                fun onLongPress(e: MotionEvent?) {
                }

                @Override
                fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                    return false
                }
            })

            @Override
            fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return gestureDetector.onTouchEvent(event)
            }
        })
        Toast.makeText(this, getString(R.string.notice_volume_to_dark_mode), Toast.LENGTH_SHORT).show()
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // set navigation bar status, remember to disable "setNavigationBarTintEnabled"
        val flags: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
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

    private fun getNovelContent() {
        val cv: ContentValues = Wenku8API.getNovelContent(aid, cid, GlobalConfig.getCurrentLang())
        val ast: asyncNovelContentTask = asyncNovelContentTask()
        ast.execute(cv)
        pDialog = Builder(this)
                .theme(Theme.LIGHT)
                .title(R.string.sorry_old_engine_preprocess)
                .content(R.string.sorry_old_engine_merging)
                .progress(false, 1, true)
                .cancelable(true)
                .cancelListener { dialog ->
                    ast.cancel(true)
                    pDialog.dismiss()
                    pDialog = null
                }
                .titleColorRes(R.color.default_text_color_black)
                .show()
        pDialog.setProgress(0)
        pDialog.setMaxProgress(1)
        pDialog.show()
    }

    internal inner class asyncNovelContentTask : AsyncTask<ContentValues?, Integer?, Integer?>() {
        // fail return -1
        @Override
        protected fun doInBackground(vararg params: ContentValues?): Integer? {
            try {
                val xml: String
                xml = if (from.equals(FromLocal)) GlobalConfig.loadFullFileFromSaveFolder("novel", "$cid.xml") else {
                    val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(
                            Wenku8API.BASE_URL, params[0]) ?: return -100
                    String(tempXml, "UTF-8")
                }
                nc = OldNovelContentParser.parseNovelContent(xml, pDialog)
                if (nc == null || nc.size() === 0) {
                    Log.e("MewX-Main", "getNullFromParser (NovelContentParser.parseNovelContent(xml);)")

                    // network error or parse failed
                    return -100
                }
                return 0
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return -1
        }

        @Override
        protected fun onPostExecute(result: Integer?) {
            if (result == -100) {
                Toast.makeText(this@VerticalReaderActivity,
                        getResources().getString(R.string.system_network_error),
                        Toast.LENGTH_LONG).show()
                if (pDialog != null) pDialog.dismiss()
                return
            }

            // The abandoned way - dynamically adding textview into layout
            for (i in 0 until nc.size()) {
                if (pDialog != null) pDialog.setProgress(i)
                when (nc.get(i).type) {
                    TEXT -> {
                        val tempTV = TextView(this@VerticalReaderActivity)
                        if (i == 0) {
                            tempTV.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                                    GlobalConfig.getShowTextSize() + 6)
                            val shader: Shader = LinearGradient(0, 0, 0,
                                    tempTV.getTextSize(), -0xffcc67, -0x996601,
                                    Shader.TileMode.CLAMP)
                            tempTV.getPaint().setShader(shader)
                        } else {
                            tempTV.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                                    GlobalConfig.getShowTextSize())
                        }
                        tempTV.setText(nc.get(i).content)
                        tempTV.setLineSpacing(GlobalConfig.getShowTextSize() * 1.0f, 1.0f) // set line space
                        tempTV.setTextColor(getResources().getColor(R.color.reader_default_text_dark))
                        tempTV.setPadding(GlobalConfig.getShowTextPaddingLeft(),
                                GlobalConfig.getShowTextPaddingTop(),
                                GlobalConfig.getShowTextPaddingRight(), 0)
                        TextListLayout.addView(tempTV)
                    }

                    IMAGE -> {
                        val tempIV = ImageView(this@VerticalReaderActivity)
                        tempIV.setClickable(true)
                        tempIV.setAdjustViewBounds(true)
                        tempIV.setScaleType(ImageView.ScaleType.FIT_CENTER) // CENTER_INSIDE
                        tempIV.setPadding(0, GlobalConfig.getShowTextPaddingTop(),
                                0, 0)
                        tempIV.setImageResource(R.drawable.ic_empty_image) // default

                        // async loader
                        val imgFileName: String = GlobalConfig.generateImageFileNameByURL(nc.get(i).content)
                        val path: String = GlobalConfig.getAvailableNovelContentImagePath(imgFileName)
                        if (path != null) {
                            ImageLoader.getInstance().displayImage(
                                    "file://$path", tempIV)
                            tempIV.setOnClickListener { v ->
                                val intent = Intent(this@VerticalReaderActivity, ViewImageDetailActivity::class.java)
                                intent.putExtra("path", path)
                                this@VerticalReaderActivity.startActivity(intent)
                                this@VerticalReaderActivity.overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                            }
                        } else {
                            // define another asynctask to load image
                            // need to access local var - tempIV
                            internal class asyncDownloadImage : AsyncTask<String?, Integer?, String?>() {
                                @Override
                                protected fun doInBackground(vararg params: String?): String? {
                                    GlobalConfig.saveNovelContentImage(params[0])
                                    val name: String = GlobalConfig.generateImageFileNameByURL(params[0])
                                    return GlobalConfig.getAvailableNovelContentImagePath(name)
                                }

                                @Override
                                protected fun onPostExecute(result: String?) {
                                    ImageLoader.getInstance().displayImage(
                                            "file://$result", tempIV)
                                    tempIV.setOnClickListener { v ->
                                        val intent = Intent(this@VerticalReaderActivity, ViewImageDetailActivity::class.java)
                                        intent.putExtra("path", result)
                                        this@VerticalReaderActivity.startActivity(intent)
                                        this@VerticalReaderActivity.overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                                    }
                                }
                            }

                            val async = asyncDownloadImage()
                            async.execute(nc.get(i).content)
                        }
                        TextListLayout.addView(tempIV)
                    }
                }
            }

            // end loading dialog
            if (pDialog != null) pDialog.dismiss()

            // show dialog
            if (GlobalConfig.getReadSavesRecord(cid, TextListLayout.getMeasuredHeight()) > 100) {
                // set scroll view
                val handler = Handler()
                handler.postDelayed(runnableScroll, 200)
                Log.d(VerticalReaderActivity::class.java.getSimpleName(), "Scroll to = " + GlobalConfig.getReadSavesRecord(cid, TextListLayout.getMeasuredHeight()))
            }
        }
    }

    @Override
    protected fun onPause() {
        super.onPause()
        saveRecord()
    }

    private fun saveRecord() {
        // cannot get height easily, except sum one by one
        GlobalConfig.addReadSavesRecord(cid, this@VerticalReaderActivity.findViewById(R.id.content_scrollview).getScrollY(),
                TextListLayout.getMeasuredHeight())
    }

    @Override
    protected fun onDestroy() {
        super.onDestroy()
        if (pDialog != null) pDialog.dismiss()
        pDialog = null
    }

    @Override
    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                TextListLayout.setBackgroundColor(getResources().getColor(R.color.reader_default_bg_black))

                // change text color
                var i = 1
                while (i < TextListLayout.getChildCount()) {
                    val view: View = TextListLayout.getChildAt(i)
                    if (view is TextView) (view as TextView).setTextColor(getResources().getColor(R.color.reader_default_text_light))
                    i++
                }
                return true
            }

            KeyEvent.KEYCODE_VOLUME_UP -> {
                TextListLayout.setBackgroundColor(getResources().getColor(R.color.reader_default_bg_yellow))

                // change text color
                var i = 1
                while (i < TextListLayout.getChildCount()) {
                    val view: View = TextListLayout.getChildAt(i)
                    if (view is TextView) (view as TextView).setTextColor(getResources().getColor(R.color.reader_default_text_dark))
                    i++
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @Override
    fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.fade_out)
    }

    companion object {
        // constant
        private val FromLocal: String? = "fav"
    }
}