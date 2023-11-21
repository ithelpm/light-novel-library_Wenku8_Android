package org.mewx.wenku8.fragment

import android.content.ContentValues

class NovelItemListFragment : Fragment(), MyItemClickListener, MyItemLongClickListener {
    private var listType: String? = ""
    private var searchKey: String? = ""
    private val isLoading: AtomicBoolean? = AtomicBoolean(false)

    // members
    private var actionBar: ActionBar? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var spb: SmoothProgressBar? = null

    // novel list info
    private var listNovelItemAid: List<Integer?>? = ArrayList() // aid list
    private var listNovelItemInfo: List<NovelItemInfoUpdate?>? = ArrayList() // novel info list
    private var mAdapter: NovelItemAdapterUpdate? = null

    // page info
    private var currentPage = 1 // default 1
    private var totalPage = 0 // default 0
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listType = getArguments().getString("type")
        // judge if is 'search'
        searchKey = if (listType.equals(SEARCH_TYPE)) getArguments().getString("key") else ""
        actionBar = (getActivity() as AppCompatActivity?).getSupportActionBar()
    }

    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView: View = inflater.inflate(R.layout.fragment_novel_item_list, container, false)
        rootView.setTag(listType) // set TAG

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

        // init values
        listNovelItemAid = ArrayList()
        listNovelItemInfo = ArrayList()
        currentPage = 1 // default 1
        totalPage = 0 // default 0
        mLayoutManager = LinearLayoutManager(getActivity())
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL)
        mRecyclerView = rootView.findViewById(R.id.novel_item_list)
        mRecyclerView.setHasFixedSize(false) // set variable size
        mRecyclerView.setItemAnimator(DefaultItemAnimator())
        mRecyclerView.setLayoutManager(mLayoutManager)

        // List request
        if (listType.equals(SEARCH_TYPE)) {
            // update UI
            spb = getActivity().findViewById(R.id.spb)
            spb.progressiveStart()

            // execute task
            Toast.makeText(getActivity(), "search", Toast.LENGTH_SHORT).show()
            val asyncGetSearchResultList: AsyncGetSearchResultList = AsyncGetSearchResultList()
            asyncGetSearchResultList.execute(searchKey)
        } else {
            // Listener
            mRecyclerView.addOnScrollListener(MyOnScrollListener())
            mRecyclerView.addOnScrollListener(OnHidingScrollListener())
            val asyncGetNovelItemList: AsyncGetNovelItemList = AsyncGetNovelItemList()
            asyncGetNovelItemList.execute(currentPage)
        }
        return rootView
    }

    @Override
    fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    @Override
    fun onItemClick(view: View?, position: Int) {
        //Toast.makeText(getActivity(),"item click detected", Toast.LENGTH_SHORT).show();
        if (position < 0 || position >= listNovelItemAid.size()) {
            // ArrayIndexOutOfBoundsException
            Toast.makeText(getActivity(), "ArrayIndexOutOfBoundsException: " + position + " in size " + listNovelItemAid.size(), Toast.LENGTH_SHORT).show()
            return
        }

        // go to detail activity
        val intent = Intent(getActivity(), NovelInfoActivity::class.java)
        intent.putExtra("aid", listNovelItemAid.get(position))
        intent.putExtra("from", "list")
        intent.putExtra("title", (view.findViewById(R.id.novel_title) as TextView).getText())
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
    }

    private inner class OnHidingScrollListener : RecyclerView.OnScrollListener() {
        var toolbarMarginOffset = 0
        @Override
        fun onScrolled(@NonNull recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            toolbarMarginOffset += dy
            if (toolbarMarginOffset > actionBar.getHeight()) actionBar.hide()
            if (toolbarMarginOffset == 0) actionBar.show()
        }
    }

    private fun refreshPartialIdList(newNovelItemAids: List<Integer?>?) {
        // Some sanity checks.
        if (newNovelItemAids == null || newNovelItemAids.isEmpty()) {
            return
        }

        // add to total list
        listNovelItemAid.addAll(newNovelItemAids)

        // Just append new updates.
        val startIndex: Int = listNovelItemInfo.size()

        // set empty
        for (aid in newNovelItemAids) {
            listNovelItemInfo.add(NovelItemInfoUpdate(aid))
        }
        if (mAdapter == null) {
            mAdapter = NovelItemAdapterUpdate()
            mAdapter.setOnItemClickListener(this)
            mAdapter.setOnItemLongClickListener(this)
        }
        mAdapter.refreshDataset(listNovelItemInfo)
        if (currentPage == 1 && mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter)
        } else {
            mAdapter.notifyItemRangeInserted(startIndex, newNovelItemAids.size())
        }
    }

    /**
     * Refresh all the list with Integer array.
     * If empty, create;
     */
    private fun refreshEntireIdList() {
        // Not creating new list for incremental data update.
        listNovelItemInfo.clear()

        // set empty
        for (temp in listNovelItemAid) {
            listNovelItemInfo.add(NovelItemInfoUpdate(temp))
        }
        if (mAdapter == null) {
            mAdapter = NovelItemAdapterUpdate()
            mAdapter.setOnItemClickListener(this)
            mAdapter.setOnItemLongClickListener(this)
        }
        mAdapter.refreshDataset(listNovelItemInfo)
        if (currentPage == 1 && mRecyclerView != null) {
            mRecyclerView.setAdapter(mAdapter)
        } else mAdapter.notifyDataSetChanged()
    }

    private inner class MyOnScrollListener : RecyclerView.OnScrollListener() {
        @Override
        fun onScrolled(@NonNull recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val pastVisiblesItems: Int
            val visibleItemCount: Int
            val totalItemCount: Int
            visibleItemCount = mLayoutManager.getChildCount()
            totalItemCount = mLayoutManager.getItemCount()
            pastVisiblesItems = mLayoutManager.findFirstVisibleItemPosition()
            if (!isLoading.get()) {
                // 滚动到一半的时候加载，即：剩余2个元素的时候就加载
                if (visibleItemCount + pastVisiblesItems + 2 >= totalItemCount && (totalPage == 0 || currentPage < totalPage)) {
                    // load more toast
                    Snackbar.make(mRecyclerView, getResources().getString(R.string.list_loading)
                            + "(" + (currentPage + 1) + "/" + totalPage + ")",
                            Snackbar.LENGTH_SHORT).show()

                    // load more thread
                    AsyncGetNovelItemList().execute(currentPage + 1)
                }
            }
        }
    }

    private inner class AsyncGetNovelItemList internal constructor() : AsyncTask<Integer?, Integer?, Integer?>() {
        private var usingWenku8Relay = false
        private var tempNovelList: List<Integer?>? = ArrayList()
        private val raceCondition: Boolean

        init {
            raceCondition = !isLoading.compareAndSet(false, true)
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Integer? {
            // Check if another loading happening.
            if (raceCondition) {
                Log.d("MewX", "doInBackground: blocking change")
                return -1
            }

            // Update the current page to the new page.
            currentPage = params[0]

            // params[0] is current page number
            val cv: ContentValues = Wenku8API.getNovelList(Wenku8API.getNOVELSORTBY(listType), currentPage)
            var temp: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
            if (temp == null) {
                // Try requesting from the relay.
                temp = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, cv, false)
                if (temp == null) {
                    // Still failed, return the error code.
                    return -1
                }
                usingWenku8Relay = true
            }
            try {
                Log.d("MewX", "doInBackground: loading page $currentPage")
                tempNovelList = Wenku8Parser.parseNovelItemList(String(temp, "UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            // judge result
            if (tempNovelList.isEmpty()) {
                Log.d("MewX", "in AsyncGetNovelItemList: doInBackground: tempNovelList == null || tempNovelList.size() == 0")
                // Try requesting from the relay.
                temp = LightNetwork.LightHttpPostConnection(Wenku8API.RELAY_URL, cv, false)
                if (temp == null) {
                    // Still failed, returns no error code.
                    return 0
                }
                try {
                    tempNovelList = Wenku8Parser.parseNovelItemList(String(temp, "UTF-8"))
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
                if (tempNovelList.isEmpty()) {
                    // Still failed.
                    return 0
                }
                usingWenku8Relay = true
            }
            totalPage = tempNovelList.get(0)
            tempNovelList.remove(0)
            return 0
        }

        @Override
        protected fun onPostExecute(integer: Integer?) {
            if (integer == -1) {
                // network error
                return
            }
            if (tempNovelList.isEmpty()) {
                Log.d("MewX", "in AsyncGetNovelItemList: onPostExecute: tempNovelList == null || tempNovelList.size() == 0")
                return
            }
            refreshPartialIdList(tempNovelList)
            isLoading.set(false)
            if (getActivity() != null) {
                val relayWarningView: View = getActivity().findViewById(R.id.relay_warning)
                if (relayWarningView != null) {
                    relayWarningView.setVisibility(if (usingWenku8Relay) View.VISIBLE else View.GONE)
                }
            }
        }
    }

    private inner class AsyncGetSearchResultList : AsyncTask<String?, Integer?, Integer?>() {
        @Override
        protected fun doInBackground(vararg params: String?): Integer? {

            // get search result by novel title
            var cv: ContentValues = Wenku8API.searchNovelByNovelName(params[0], GlobalConfig.getCurrentLang())
            val tempListTitle: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                    ?: return -1

            // purify returned data
            val listResultList: List<Integer?> = ArrayList() // result list
            try {
                Log.d("MewX", String(tempListTitle, "UTF-8"))
                val p: Pattern = Pattern.compile("aid=\'(.*)\'") // match content between "aid=\'" and "\'"
                val m: Matcher = p.matcher(String(tempListTitle, "UTF-8"))
                while (m.find()) listResultList.add(Integer.valueOf(m.group(1)))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            // get search result by author name
            cv = Wenku8API.searchNovelByAuthorName(params[0], GlobalConfig.getCurrentLang())
            val tempListName: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, cv)
                    ?: return -1

            // purify returned data
            val listResultList2: List<Integer?> = ArrayList() // result list
            try {
                Log.d("MewX", String(tempListName, "UTF-8"))
                val p: Pattern = Pattern.compile("aid=\'(.*)\'") // match content between "aid=\'" and "\'"
                val m: Matcher = p.matcher(String(tempListName, "UTF-8"))
                while (m.find()) {
                    listResultList2.add(Integer.valueOf(m.group(1)))
                    Log.d("MewX", listResultList2[listResultList2.size() - 1].toString())
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }

            // set migrate
            listNovelItemAid = ArrayList()
            listNovelItemAid.addAll(listResultList)
            listNovelItemAid.removeAll(listResultList2)
            listNovelItemAid.addAll(listResultList2)
            return 0
        }

        @Override
        protected fun onPostExecute(integer: Integer?) {
            super.onPostExecute(integer)
            spb.progressiveStop()
            if (integer == -1) {
                Toast.makeText(getActivity(), getResources().getString(R.string.system_network_error), Toast.LENGTH_LONG).show()
                return
            }
            if (listNovelItemAid.isEmpty()) {
                Toast.makeText(getActivity(), getResources().getString(R.string.task_null), Toast.LENGTH_LONG).show()
                return
            }
            // show all items
            refreshEntireIdList()
        }
    }

    @Override
    fun onDetach() {
        super.onDetach()
        if (actionBar != null) actionBar.show()
    }

    companion object {
        private val SEARCH_TYPE: String? = "search"
        fun newInstance(args: Bundle?): NovelItemListFragment? {
            val fragment = NovelItemListFragment()
            fragment.setArguments(args)
            return fragment
        }
    }
}