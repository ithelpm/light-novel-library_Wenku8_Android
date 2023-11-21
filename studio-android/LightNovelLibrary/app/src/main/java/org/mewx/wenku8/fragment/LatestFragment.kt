package org.mewx.wenku8.fragment

import android.content.ContentValues

class LatestFragment : Fragment(), MyItemClickListener, MyItemLongClickListener {
    // components
    private var mainActivity: MainActivity? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mTextView: TextView? = null

    // Novel Item info
    private var listNovelItemInfo: List<NovelItemInfoUpdate?>? = ArrayList()
    private var mAdapter: NovelItemAdapter? = null
    private var currentPage = 0
    private var totalPage = 0 // currentP stores next reading page num, TODO: fix wrong number

    // switcher
    private val isLoading: AtomicBoolean? = AtomicBoolean(false)
    var pastVisibleItems = 0
    var visibleItemCount = 0
    var totalItemCount = 0
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listNovelItemInfo = ArrayList()
    }

    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?,
                     savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val rootView: View = inflater.inflate(R.layout.fragment_latest, container, false)

        // Set warning message.
        rootView.findViewById(R.id.relay_warning).setOnClickListener { view ->
            Builder(getContext())
                    .theme(Theme.LIGHT)
                    .backgroundColorRes(R.color.dlgBackgroundColor)
                    .contentColorRes(R.color.dlgContentColor)
                    .positiveColorRes(R.color.dlgPositiveButtonColor)
                    .negativeColorRes(R.color.dlgNegativeButtonColor)
                    .title(getResources().getString(R.string.system_warning))
                    .content(getResources().getString(R.string.relay_warning_full))
                    .positiveText(R.string.dialog_positive_ok)
                    .show()
        }

        // get views
        mRecyclerView = rootView.findViewById(R.id.novel_item_list)
        mTextView = rootView.findViewById(R.id.list_loading_status)
        mRecyclerView.setHasFixedSize(true)
        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(getActivity())
        mRecyclerView.setLayoutManager(mLayoutManager)

        // Listener
        mRecyclerView.addOnScrollListener(MyOnScrollListener())

        // set click event
        rootView.findViewById(R.id.btn_loading).setOnClickListener { v ->
            // To prepare for a loading, need to set the loading status to false.
            // If it's already loading, then do nothing.
            if (!isLoading.compareAndSet(true, false)) {
                // need to reload novel list all
                currentPage = 1
                totalPage = 1
                loadNovelList(currentPage)
            }
        }

        // fetch initial novel list and reset isLoading
        currentPage = 1
        totalPage = 1
        isLoading.set(false)
        loadNovelList(currentPage)
        return rootView
    }

    private fun loadNovelList(page: Int) {
        // In fact, I don't need to know what it really is.
        // I just need to get the NOVELSORTBY
        if (!isLoading.compareAndSet(false, true)) {
            // Is loading already.
            return
        }
        hideRetryButton()

        // fetch list
        val ast: AsyncLoadLatestList = AsyncLoadLatestList()
        ast.execute(Wenku8API.getNovelListWithInfo(Wenku8API.NOVELSORTBY.lastUpdate, page,
                GlobalConfig.getCurrentLang()))
    }

    @Override
    fun onItemClick(view: View?, position: Int) {
        if (position < 0 || position >= listNovelItemInfo.size()) {
            // ArrayIndexOutOfBoundsException
            Toast.makeText(getActivity(), "ArrayIndexOutOfBoundsException: " + position + " in size " + listNovelItemInfo.size(), Toast.LENGTH_SHORT).show()
            return
        }

        // go to detail activity
        val intent = Intent(getActivity(), NovelInfoActivity::class.java)
        intent.putExtra("aid", listNovelItemInfo.get(position).aid)
        intent.putExtra("from", "latest")
        intent.putExtra("title", listNovelItemInfo.get(position).title)
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
    fun onItemLongClick(view: View?, position: Int) {
        // empty
        onItemClick(view, position)
    }

    @Override
    fun onAttach(@NonNull context: Context?) {
        super.onAttach(context)
        mainActivity = getActivity() as MainActivity?
    }

    @Override
    fun onDetach() {
        super.onDetach()
        mainActivity = null
        isLoading.set(false)
    }

    private inner class MyOnScrollListener : RecyclerView.OnScrollListener() {
        @Override
        fun onScrolled(@NonNull recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            visibleItemCount = mLayoutManager.getChildCount()
            totalItemCount = mLayoutManager.getItemCount()
            pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition()

            // 滚动到一半的时候加载，即：剩余3个元素的时候就加载
            if (!isLoading.get() && visibleItemCount + pastVisibleItems + 3 >= totalItemCount) {
                // load more toast
                Snackbar.make(mRecyclerView, getResources().getString(R.string.list_loading)
                        + "(" + currentPage + "/" + totalPage + ")",
                        Snackbar.LENGTH_SHORT).show()

                // load more thread
                if (currentPage <= totalPage) {
                    loadNovelList(currentPage)
                } else {
                    Snackbar.make(mRecyclerView, getResources().getText(R.string.loading_done), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    internal inner class AsyncLoadLatestList : AsyncTask<ContentValues?, Integer?, Integer?>() {
        private var usingWenku8Relay = false
        private var numOfItemsToRefresh = 0

        // fail return -1
        @Override
        protected fun doInBackground(vararg params: ContentValues?): Integer? {
            try {
                // Try requesting from the original website.
                var tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, params[0])
                if (tempXml == null) {
                    // Try requesting from the relay.
                    tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, params[0], false)
                    if (tempXml == null) {
                        // Still failed, return the error code.
                        return -100
                    }
                    usingWenku8Relay = true
                }
                var xml = String(tempXml, "UTF-8")
                totalPage = NovelListWithInfoParser.getNovelListWithInfoPageNum(xml)
                var l: List<NovelListWithInfoParser.NovelListWithInfo?> = NovelListWithInfoParser.getNovelListWithInfo(xml)
                if (l.isEmpty()) {
                    // Try requesting from the relay.
                    tempXml = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, params[0], false)
                    if (tempXml == null) {
                        // Relay network error.
                        return -100
                    }
                    xml = String(tempXml, "UTF-8")
                    totalPage = NovelListWithInfoParser.getNovelListWithInfoPageNum(xml)
                    l = NovelListWithInfoParser.getNovelListWithInfo(xml)
                    if (l.isEmpty()) {
                        // Blocked error.
                        return -100
                    }
                    usingWenku8Relay = true
                }
                for (i in 0 until l.size()) {
                    val nlwi: NovelListWithInfoParser.NovelListWithInfo? = l[i]
                    val ni = NovelItemInfoUpdate(nlwi.aid)
                    ni.title = nlwi.name
                    ni.author = nlwi.hit + "" // hit
                    ni.update = nlwi.push + "" // push
                    ni.intro_short = nlwi.fav + "" // fav
                    listNovelItemInfo.add(ni)
                    numOfItemsToRefresh++
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return 0
        }

        @Override
        protected fun onPostExecute(result: Integer?) {
            if (result == -100) {
                if (!isAdded()) return  // detached
                mTextView.setText(getResources().getString(R.string.system_parse_failed))
                showRetryButton()
                isLoading.set(false)
                return
            }

            // result:
            // add imageView, only here can fetch the layout2 id!!!
            // hide loading layout
            // Note that after switching between fragments, the adapter has a chance to disappear. So, we need to attach it back.
            if (mAdapter == null || mRecyclerView.getAdapter() == null) {
                mAdapter = NovelItemAdapter(listNovelItemInfo)
                mAdapter.setOnItemClickListener(this@LatestFragment)
                mAdapter.setOnItemLongClickListener(this@LatestFragment)
                mRecyclerView.setAdapter(mAdapter)
            }
            val listLoadingView: View = mainActivity.findViewById(R.id.list_loading)
            if (listLoadingView != null) {
                listLoadingView.setVisibility(View.GONE)
            }
            // Incremental changes
            if (numOfItemsToRefresh != 0) {
                mAdapter.notifyItemRangeInserted(listNovelItemInfo.size() - numOfItemsToRefresh, numOfItemsToRefresh)
            }
            currentPage++ // add when loaded
            isLoading.set(false)
            val relayWarningView: View = mainActivity.findViewById(R.id.relay_warning)
            if (relayWarningView != null) {
                relayWarningView.setVisibility(if (usingWenku8Relay) View.VISIBLE else View.GONE)
            }
        }
    }

    @Override
    fun onPause() {
        super.onPause()
        GlobalConfig.LeaveLatest()
    }

    @Override
    fun onResume() {
        super.onResume()
        GlobalConfig.EnterLatest()
    }

    private fun showRetryButton() {
        if (mainActivity.findViewById(R.id.btn_loading) == null || !isAdded()) return
        (mainActivity.findViewById(R.id.btn_loading) as TextView).setText(getResources().getString(R.string.task_retry))
        mainActivity.findViewById(R.id.google_progress).setVisibility(View.GONE)
        mainActivity.findViewById(R.id.btn_loading).setVisibility(View.VISIBLE)
    }

    /**
     * After button pressed, should hide the "retry" button
     */
    private fun hideRetryButton() {
        if (mainActivity.findViewById(R.id.btn_loading) == null) return
        mTextView.setText(getResources().getString(R.string.list_loading))
        mainActivity.findViewById(R.id.google_progress).setVisibility(View.VISIBLE)
        mainActivity.findViewById(R.id.btn_loading).setVisibility(View.GONE)
    }

    companion object {
        private val TAG: String? = "LatestFragment"
    }
}