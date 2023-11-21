package org.mewx.wenku8.activity

import android.content.Intent

/**
 * Created by MewX on 2018/7/12.
 * Novel Review Activity.
 */
class NovelReviewListActivity : BaseMaterialActivity(), MyItemClickListener {
    // private vars
    private var aid = 1

    // components
    private var mLayoutManager: LinearLayoutManager? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mLoadingLayout: LinearLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLoadingStatusTextView: TextView? = null
    private var mLoadingButton: TextView? = null

    // switcher
    private var mAdapter: ReviewItemAdapter? = null
    private var reviewList: ReviewList? = ReviewList()
    var pastVisibleItems = 0
    var visibleItemCount = 0
    var totalItemCount = 0
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_novel_review_list)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)

        // get views and set title
        // get views
        mLoadingLayout = findViewById(R.id.list_loading)
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        mRecyclerView = findViewById(R.id.review_item_list)
        mLoadingStatusTextView = findViewById(R.id.list_loading_status)
        mLoadingButton = findViewById(R.id.btn_loading)
        mRecyclerView.setHasFixedSize(false)
        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.setLayoutManager(mLayoutManager)

        // Listener
        mRecyclerView.addOnScrollListener(MyOnScrollListener())

        // set click event for retry and cancel loading
        mLoadingButton.setOnClickListener { v -> AsyncReviewListLoader(this, mSwipeRefreshLayout, aid, reviewList).execute() } // retry loading
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.myAccentColor))
        mSwipeRefreshLayout.setOnRefreshListener { reloadAllReviews() }
    }

    private fun reloadAllReviews() {
        reviewList = ReviewList()
        mAdapter = null
        AsyncReviewListLoader(this, mSwipeRefreshLayout, aid, reviewList).execute()
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_review_list, menu)
        return true
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        } else if (menuItem.getItemId() === R.id.action_new) {
            val intent = Intent(this@NovelReviewListActivity, NovelReviewNewPostActivity::class.java)
            intent.putExtra("aid", aid)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // Load initial content or refresh the list when resumed.
        reloadAllReviews()
    }

    fun getAdapter(): ReviewItemAdapter? {
        return mAdapter
    }

    fun setAdapter(adapter: ReviewItemAdapter?) {
        mAdapter = adapter
    }

    fun getRecyclerView(): RecyclerView? {
        return mRecyclerView
    }

    fun showRetryButton() {
        mLoadingStatusTextView.setText(getResources().getString(R.string.system_parse_failed))
        mLoadingButton.setVisibility(View.VISIBLE)
    }

    fun hideRetryButton() {
        mLoadingStatusTextView.setText(getResources().getString(R.string.list_loading))
        mLoadingButton.setVisibility(View.GONE)
    }

    fun hideListLoading() {
        hideRetryButton()
        mLoadingLayout.setVisibility(View.GONE)
    }

    @Override
    fun onItemClick(view: View?, position: Int) {
        val intent = Intent(this@NovelReviewListActivity, NovelReviewReplyListActivity::class.java)
        intent.putExtra("rid", reviewList.getList().get(position).getRid())
        intent.putExtra("title", reviewList.getList().get(position).getTitle())
        startActivity(intent)
    }

    private inner class MyOnScrollListener : RecyclerView.OnScrollListener() {
        @Override
        fun onScrolled(@NonNull recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            visibleItemCount = mLayoutManager.getChildCount()
            totalItemCount = mLayoutManager.getItemCount()
            pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition()

            // 剩余3个元素的时候就加载
            if (!isLoading.get() && visibleItemCount + pastVisibleItems >= totalItemCount) { // can be +1/2/3 >= total
                // load more thread
                if (reviewList.getCurrentPage() < reviewList.getTotalPage()) {
                    // load more toast
                    Snackbar.make(mRecyclerView, ((getResources().getString(R.string.list_loading)
                            + "(" + (reviewList.getCurrentPage() + 1)).toString() + "/" + reviewList.getTotalPage()).toString() + ")",
                            Snackbar.LENGTH_SHORT).show()
                    AsyncReviewListLoader(this@NovelReviewListActivity, mSwipeRefreshLayout, aid, reviewList).execute()
                }
            }
        }
    }

    private class AsyncReviewListLoader internal constructor(@NonNull novelReviewListActivity: NovelReviewListActivity?, @NonNull swipeRefreshLayout: SwipeRefreshLayout?, aid: Int, @NonNull reviewList: ReviewList?) : AsyncTask<Void?, Void?, Void?>() {
        private val novelReviewListActivityWeakReference: WeakReference<NovelReviewListActivity?>?
        private val swipeRefreshLayoutWeakReference: WeakReference<SwipeRefreshLayout?>?
        private val aid: Int
        private val reviewList: ReviewList?
        private var runOrNot = true // by default, run it
        private var metNetworkIssue = false

        init {
            novelReviewListActivityWeakReference = WeakReference(novelReviewListActivity)
            swipeRefreshLayoutWeakReference = WeakReference(swipeRefreshLayout)
            this.aid = aid
            this.reviewList = reviewList
        }

        @Override
        protected fun onPreExecute() {
            if (isLoading.getAndSet(true)) {
                // is running, do not run again
                runOrNot = false
            } else {
                // not running, so run it
                val tempSwipeLayout: SwipeRefreshLayout = swipeRefreshLayoutWeakReference.get()
                if (tempSwipeLayout != null) tempSwipeLayout.setRefreshing(true)
                val tempActivity: NovelReviewListActivity = novelReviewListActivityWeakReference.get()
                if (tempActivity != null) tempActivity.hideRetryButton()
            }
        }

        @Override
        protected fun doInBackground(vararg v: Void?): Void? {
            if (!runOrNot || reviewList.getCurrentPage() + 1 > reviewList.getTotalPage()) return null

            // load current page + 1
            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getCommentListParams(aid, reviewList.getCurrentPage() + 1))
            if (tempXml == null) {
                metNetworkIssue = true
                return null // network issue
            }
            val xml = String(tempXml, Charset.forName("UTF-8"))
            Log.d(NovelReviewListActivity::class.java.getSimpleName(), xml)
            Wenku8Parser.parseReviewList(reviewList, xml)
            return null
        }

        @Override
        protected fun onPostExecute(v: Void?) {
            // refresh everything when required
            if (!runOrNot) return
            val tempActivity: NovelReviewListActivity = novelReviewListActivityWeakReference.get()
            if (metNetworkIssue) {
                // met net work issue, show retry button
                if (tempActivity != null) tempActivity.showRetryButton()
            } else if (tempActivity != null) {
                // all good, update list
                if (tempActivity.getAdapter() == null) {
                    val reviewItemAdapter = ReviewItemAdapter(reviewList)
                    tempActivity.setAdapter(reviewItemAdapter)
                    reviewItemAdapter.setOnItemClickListener(tempActivity)
                    tempActivity.getRecyclerView().setAdapter(reviewItemAdapter)
                }
                tempActivity.getAdapter().notifyDataSetChanged()
                tempActivity.hideListLoading()
            }

            // stop spinning
            val tempSwipeLayout: SwipeRefreshLayout = swipeRefreshLayoutWeakReference.get()
            if (tempSwipeLayout != null) tempSwipeLayout.setRefreshing(false)

            // reset loading status
            isLoading.set(false)
        }
    }

    companion object {
        private val isLoading: AtomicBoolean? = AtomicBoolean(false)
    }
}