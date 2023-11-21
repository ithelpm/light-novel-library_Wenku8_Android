package org.mewx.wenku8.activity

import android.content.Intent

/**
 * Created by MewX on 2015/5/14.
 * Novel Chapter Activity.
 */
class NovelChapterActivity : BaseMaterialActivity() {
    // constant
    private val FromLocal: String? = "fav"

    // private vars
    private var aid = 1
    private var from: String? = ""
    private var volumeList: VolumeList? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_novel_chapter)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)
        from = getIntent().getStringExtra("from")
        volumeList = getIntent().getSerializableExtra("volume") as VolumeList

        // set title
        getSupportActionBar().setTitle(volumeList.volumeName)
        buildChapterList()
    }

    private fun buildChapterList() {
        // get views
        val mLinearLayout: LinearLayout = findViewById(R.id.novel_chapter_scroll)
        mLinearLayout.removeAllViews()
        val rs: GlobalConfig.ReadSavesV1 = GlobalConfig.getReadSavesRecordV1(aid)
        for (ci in volumeList.chapterList) {
            // get view
            val rl: RelativeLayout = LayoutInflater.from(this@NovelChapterActivity).inflate(R.layout.view_novel_chapter_item, null) as RelativeLayout
            val tv: TextView = rl.findViewById(R.id.chapter_title)
            tv.setText(ci.chapterName)
            val btn: RelativeLayout = rl.findViewById(R.id.chapter_btn)
            // added indicator for last read chapter
            if (rs != null && rs.cid === ci.cid) {
                btn.setBackgroundColor(Color.LTGRAY)
            }
            btn.setOnClickListener { ignored ->
                // jump to reader activity
                val intent = Intent(this@NovelChapterActivity, Wenku8ReaderActivityV1::class.java)
                intent.putExtra("aid", aid)
                intent.putExtra("volume", volumeList)
                intent.putExtra("cid", ci.cid)

                // test does file exist
                if (from.equals(FromLocal)
                        && !LightCache.testFileExist(((GlobalConfig.getDefaultStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + ci.cid).toString() + ".xml")
                        && !LightCache.testFileExist(((GlobalConfig.getBackupStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + ci.cid).toString() + ".xml")) {
                    intent.putExtra("from", "cloud") // from cloud
                } else {
                    intent.putExtra("from", from) // from "fav"
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
            }
            btn.setOnLongClickListener { ignored ->
                Builder(this@NovelChapterActivity)
                        .theme(Theme.LIGHT)
                        .title(R.string.system_choose_reader_engine)
                        .items(R.array.reader_engine_option)
                        .itemsCallback { ignored1, ignored2, which, ignored3 ->
                            var readerClass: Class = Wenku8ReaderActivityV1::class.java
                            when (which) {
                                0 ->                                 // V1
                                    readerClass = Wenku8ReaderActivityV1::class.java

                                1 ->                                 // old
                                    readerClass = VerticalReaderActivity::class.java
                            }
                            val intent = Intent(this@NovelChapterActivity, readerClass)
                            intent.putExtra("aid", aid)
                            intent.putExtra("volume", volumeList)
                            intent.putExtra("cid", ci.cid)

                            // test does file exist
                            if (from.equals(FromLocal)
                                    && !LightCache.testFileExist(((GlobalConfig.getDefaultStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + ci.cid).toString() + ".xml")
                                    && !LightCache.testFileExist(((GlobalConfig.getBackupStoragePath() + GlobalConfig.saveFolderName + File.separator).toString() + "novel" + File.separator + ci.cid).toString() + ".xml")) {
                                // jump to reader activity
                                intent.putExtra("from", "cloud") // from cloud
                            } else {
                                intent.putExtra("from", from) // from "fav"
                            }
                            startActivity(intent)
                            overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                        }
                        .show()
                true
            }

            // add to scroll view
            mLinearLayout.addView(rl)
        }
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // refresh when back from reader activity
        buildChapterList()
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}