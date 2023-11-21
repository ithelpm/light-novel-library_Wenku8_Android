package org.mewx.wenku8.fragment

import android.content.ContentValues

class FavFragment : Fragment(), MyItemClickListener, MyItemLongClickListener, MyOptionClickListener {
    // local vars
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var timecount = 0

    // novel list info
    private var listNovelItemAid: List<Integer?>? = null // aid list
    private var listNovelItemInfo: List<NovelItemInfoUpdate?>? = null // info list
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?,
                     savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_fav, container, false)

        // find view
        mSwipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout)

        // init values
        timecount = 0

        // view setting
        val mLayoutManager = LinearLayoutManager(getActivity())
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL)
        mRecyclerView = rootView.findViewById(R.id.novel_item_list)
        mRecyclerView.setHasFixedSize(false) // set variable size
        mRecyclerView.setItemAnimator(DefaultItemAnimator())
        mRecyclerView.setLayoutManager(mLayoutManager)
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.myAccentColor))
        mSwipeRefreshLayout.setOnRefreshListener { AsyncLoadAllCloud().execute(1) }
        return rootView
    }

    @Override
    fun onItemClick(view: View?, position: Int) {
        // go to detail activity
        val intent = Intent(getActivity(), NovelInfoActivity::class.java)
        intent.putExtra("aid", listNovelItemAid.get(position))
        intent.putExtra("from", "fav")
        intent.putExtra("title", (view.findViewById(R.id.novel_title) as TextView).getText())
        GlobalConfig.accessToLocalBookshelf(listNovelItemAid.get(position)) // sort event
        if (Build.VERSION.SDK_INT < 21) {
            startActivity(intent)
        } else {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(),
                    Pair.create(view.findViewById(R.id.novel_cover), "novel_cover"),
                    Pair.create(view.findViewById(R.id.novel_title), "novel_title"))
            ActivityCompat.startActivity(getActivity(), intent, options.toBundle())
        }
    }

    @Override
    fun onOptionButtonClick(view: View?, position: Int) {
        Builder(getActivity())
                .theme(Theme.LIGHT)
                .title(R.string.dialog_title_choose_delete_option)
                .backgroundColorRes(R.color.dlgBackgroundColor)
                .titleColorRes(R.color.dlgTitleColor)
                .negativeText(R.string.dialog_negative_pass)
                .negativeColorRes(R.color.dlgNegativeButtonColor)
                .itemsGravity(GravityEnum.CENTER)
                .items(R.array.cleanup_option)
                .itemsCallback(object : ListCallback() {
                    @Override
                    fun onSelection(dialog: MaterialDialog?, view: View?, which: Int, text: CharSequence?) {
                        /*
                         * 0 <string name="dialog_clear_cache">清除缓存</string>
                         * 1 <string name="dialog_delete_book">删除这本书</string>
                         */
                        when (which) {
                            0 -> Builder(getActivity())
                                    .callback(object : ButtonCallback() {
                                        @Override
                                        fun onPositive(dialog: MaterialDialog?) {
                                            super.onPositive(dialog)
                                            val aid: Int = listNovelItemAid.get(position)
                                            val listVolume: List<VolumeList?>
                                            val novelFullVolume: String
                                            novelFullVolume = GlobalConfig.loadFullFileFromSaveFolder("intro", "$aid-volume.xml")
                                            if (novelFullVolume.isEmpty()) return
                                            listVolume = Wenku8Parser.getVolumeList(novelFullVolume)
                                            if (listVolume.isEmpty()) return
                                            cleanVolumesCache(listVolume)
                                        }
                                    })
                                    .theme(Theme.LIGHT)
                                    .content(R.string.dialog_sure_to_clear_cache)
                                    .contentGravity(GravityEnum.CENTER)
                                    .positiveText(R.string.dialog_positive_sure)
                                    .negativeText(R.string.dialog_negative_preferno)
                                    .show()

                            1 -> Builder(getActivity())
                                    .callback(object : ButtonCallback() {
                                        @Override
                                        fun onPositive(dialog: MaterialDialog?) {
                                            super.onPositive(dialog)
                                            // delete operation, delete from cloud first, if succeed then delete from local
                                            val arbfc: AsyncRemoveBookFromCloud = AsyncRemoveBookFromCloud()
                                            arbfc.execute(listNovelItemAid.get(position))
                                            listNovelItemAid.remove(position)
                                            refreshList(timecount++)
                                        }
                                    })
                                    .theme(Theme.LIGHT)
                                    .content(R.string.dialog_content_want_to_delete)
                                    .contentGravity(GravityEnum.CENTER)
                                    .positiveText(R.string.dialog_positive_sure)
                                    .negativeText(R.string.dialog_negative_preferno)
                                    .show()
                        }
                    }
                })
                .show()
    }

    @Override
    fun onItemLongClick(view: View?, position: Int) {
    }

    private fun cleanVolumesCache(listVolume: List<VolumeList?>?) {
        // remove from local bookshelf, already in bookshelf
        for (vl in listVolume) {
            vl.cleanLocalCache()
        }
    }

    private fun refreshList(time: Int) {
        if (time == 0) {
            mSwipeRefreshLayout.setRefreshing(true)
            AsyncLoadAllCloud().execute()
        } else {
            loadAllLocal()
        }
    }

    private fun loadAllLocal() {
        var retValue = 0
        var datasetChanged = false

        // init
        listNovelItemAid = GlobalConfig.getLocalBookshelfList()
        if (listNovelItemInfo == null) {
            listNovelItemInfo = ArrayList()
        }

        // load all meta file
        aids@ for (j in 0 until listNovelItemAid.size()) {
            val aid: Integer? = listNovelItemAid.get(j)
            // See if it's in the list already. Expecting the list will not be more than 100.
            for (i in 0 until listNovelItemInfo.size()) {
                val info: NovelItemInfoUpdate? = listNovelItemInfo.get(i)
                if (info.aid === aid) {
                    // Found but in the same place.
                    if (i == j) continue@aids

                    // Found, not in the same place remove and re-insert.
                    listNovelItemInfo.remove(i)
                    listNovelItemInfo.add(j, info)
                    datasetChanged = true
                    continue@aids
                }
            }

            // Not found.
            val xml: String = GlobalConfig.loadFullFileFromSaveFolder("intro", aid.toString() + "-intro.xml")
            var info: NovelItemInfoUpdate?
            if (xml.isEmpty()) {
                // the intro file was deleted
                retValue = -1
                info = NovelItemInfoUpdate(aid)
            } else {
                info = NovelItemInfoUpdate.convertFromMeta(Objects.requireNonNull(Wenku8Parser.parseNovelFullMeta(xml)))
            }
            datasetChanged = true
            listNovelItemInfo.add(j, info)
        }

        // result
        if (retValue != 0) {
            Toast.makeText(getActivity(), getResources().getString(R.string.bookshelf_intro_load_failed), Toast.LENGTH_SHORT).show()
        }

        // Reuse the adapter and datasets.
        if (mRecyclerView.getAdapter() == null) {
            val adapter = NovelItemAdapterUpdate()
            adapter.refreshDataset(listNovelItemInfo)
            adapter.setOnItemClickListener(this@FavFragment)
            adapter.setOnDeleteClickListener(this@FavFragment)
            adapter.setOnItemLongClickListener(this@FavFragment)
            mRecyclerView.setAdapter(adapter)
        }
        if (datasetChanged) {
            mRecyclerView.getAdapter().notifyDataSetChanged()
        }
        mSwipeRefreshLayout.setRefreshing(false)
    }

    private inner class AsyncLoadAllCloud : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        private var md: MaterialDialog? = null
        private var isLoading = false // check in "doInBackground" to make sure to continue or not
        private var forceLoad = false
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            loadAllLocal()
            isLoading = true
            md = Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_content_sync)
                    .progress(false, 1, true)
                    .cancelable(true)
                    .cancelListener { dialog ->
                        isLoading = false
                        md.dismiss()
                    }
                    .show()
            md.setProgress(0)
            md.setMaxProgress(0)
            md.show()
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            // if params.length != 0, force async
            if (params != null && params.size != 0) forceLoad = true

            // ! any network problem will interrupt this procedure
            // load bookshelf list, don't save
            var b: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getBookshelfListAid(GlobalConfig.getCurrentLang()))
                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
            if (LightTool.isInteger(String(b))) {
                if (Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(String(b))) === Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN) {
                    // do log in
                    val temp: Wenku8Error.ErrorCode = LightUserSession.doLoginFromFile()
                    if (temp !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) return temp // return an error code

                    // request again
                    b = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getBookshelfListAid(GlobalConfig.getCurrentLang()))
                    if (b == null) return Wenku8Error.ErrorCode.NETWORK_ERROR
                }
            }

            // purify returned data
            val listResultList: List<Integer?> = ArrayList() // result list
            try {
                Log.d("MewX", String(b, "UTF-8"))
                val p: Pattern = Pattern.compile("aid=\"(.*)\"") // match content between "aid=\"" and "\""
                val m: Matcher = p.matcher(String(b, "UTF-8"))
                while (m.find()) {
                    try {
                        listResultList.add(Integer.valueOf(m.group(1)))
                    } catch (e: NumberFormatException) {
                        Log.e(FavFragment::class.java.getSimpleName(), "Found and skipped broken aid.")
                    }
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            // calc difference
            val listAll: List<Integer?> = ArrayList()
            listAll.addAll(GlobalConfig.getLocalBookshelfList()) // make a copy
            listAll.addAll(listResultList)
            val localOnly: List<Integer?> = ArrayList()
            localOnly.addAll(listAll)
            localOnly.removeAll(listResultList) // local only
            val listDiff: List<Integer?> = ArrayList()
            listDiff.addAll(listAll)
            if (!forceLoad) {
                // cloud only
                listDiff.removeAll(GlobalConfig.getLocalBookshelfList())
            } else {
                // local and cloud together
                val hs: HashSet<Integer?> = HashSet(listDiff)
                listDiff.clear()
                listDiff.addAll(hs)
            }
            if (listDiff.size() === 0 && localOnly.size() === 0) {
                // equal, so exit
                return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
            }

            // load all cloud only book
            var count = 0
            md.setMaxProgress(listDiff.size())
            for (aid in listDiff) {
                if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK

                // download general file
                var volumeXml: String
                var introXml: String
                var vl: List<VolumeList?>
                var ni: NovelItemMeta
                try {
                    // fetch volumes
                    var cv: ContentValues = Wenku8API.getNovelIndex(aid, GlobalConfig.getCurrentLang())
                    val tempVolumeXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                    if (tempVolumeXml == null) return Wenku8Error.ErrorCode.NETWORK_ERROR
                    volumeXml = String(tempVolumeXml, "UTF-8")

                    // fetch intro
                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK

                    // use short intro
                    val tempIntroXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL,
                            Wenku8API.getNovelFullMeta(aid, GlobalConfig.getCurrentLang()))
                            ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                    introXml = String(tempIntroXml, "UTF-8")

                    // parse into structures
                    vl = Wenku8Parser.getVolumeList(volumeXml)
                    ni = Wenku8Parser.parseNovelFullMeta(introXml)
                    if (vl.isEmpty() || ni == null) return Wenku8Error.ErrorCode.XML_PARSE_FAILED
                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                    cv = Wenku8API.getNovelFullIntro(ni.aid, GlobalConfig.getCurrentLang())
                    val tempFullIntro: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                            ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
                    ni.fullIntro = String(tempFullIntro, "UTF-8")

                    // write into saved file, save from volum -> meta -> add2bookshelf
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid.toString() + "-volume.xml", volumeXml)
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid.toString() + "-introfull.xml", ni.fullIntro)
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid.toString() + "-intro.xml", introXml)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // last, add to local
                GlobalConfig.addToLocalBookshelf(aid)
                publishProgress(++count)
            }

            // sync local bookshelf, and set ribbon, sync one, delete one
            val copy: List<Integer?> = ArrayList(localOnly) // make a copy
            for (aid in copy) {
                b = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getAddToBookshelfParams(aid))
                if (b == null) return Wenku8Error.ErrorCode.NETWORK_ERROR
                try {
                    if (LightTool.isInteger(String(b, "UTF-8"))) {
                        val result: Wenku8Error.ErrorCode = Wenku8Error.getSystemDefinedErrorCode(Integer.valueOf(String(b, "UTF-8")))
                        if (result === Wenku8Error.ErrorCode.SYSTEM_6_BOOKSHELF_FULL) {
                            return result
                        } else if (result === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED || result === Wenku8Error.ErrorCode.SYSTEM_5_ALREADY_IN_BOOKSHELF) {
                            localOnly.remove(aid) // remove Obj
                        }
                    }
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onProgressUpdate(vararg values: Integer?) {
            super.onProgressUpdate(values)
            md.setProgress(values[0])
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            isLoading = false
            md.dismiss()
            if (errorCode !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                Toast.makeText(MyApp.getContext(), errorCode.toString(), Toast.LENGTH_SHORT).show()
                refreshList(timecount++)
            } else {
                loadAllLocal()
            }
        }
    }

    internal inner class AsyncRemoveBookFromCloud : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        var md: MaterialDialog? = null
        var aid = 0
        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            md = Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .content(R.string.dialog_content_novel_remove_from_cloud)
                    .contentColorRes(R.color.dlgContentColor)
                    .progress(true, 0)
                    .show()
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            // params: aid
            aid = params[0]
            val bytes: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getDelFromBookshelfParams(aid))
                    ?: return Wenku8Error.ErrorCode.NETWORK_ERROR
            val result: String
            return try {
                result = String(bytes, "UTF-8")
                Log.d("MewX", result)
                if (!LightTool.isInteger(result)) return Wenku8Error.ErrorCode.RETURNED_VALUE_EXCEPTION
                if (Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED && Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN && Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result)) !== Wenku8Error.ErrorCode.SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF) {
                    Wenku8Error.getSystemDefinedErrorCode(Integer.parseInt(result))
                } else {
                    // load volume first
                    // get novel chapter list
                    val listVolume: List<VolumeList?>
                    val novelFullVolume: String
                    novelFullVolume = GlobalConfig.loadFullFileFromSaveFolder("intro", "$aid-volume.xml")
                    if (novelFullVolume.isEmpty()) return Wenku8Error.ErrorCode.ERROR_DEFAULT
                    listVolume = Wenku8Parser.getVolumeList(novelFullVolume)
                    if (listVolume.isEmpty()) return Wenku8Error.ErrorCode.XML_PARSE_FAILED
                    cleanVolumesCache(listVolume)
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
                        //Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_error), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getActivity(), getResources().getString(R.string.bookshelf_removed), Toast.LENGTH_SHORT).show()
            } else Toast.makeText(getActivity(), err.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    @Override
    fun onPause() {
        super.onPause()
        GlobalConfig.LeaveBookshelf()
    }

    @Override
    fun onResume() {
        super.onResume()
        GlobalConfig.EnterBookshelf()

        // refresh list
        refreshList(timecount++)
    }

    companion object {
        fun newInstance(): FavFragment? {
            return FavFragment()
        }
    }
}