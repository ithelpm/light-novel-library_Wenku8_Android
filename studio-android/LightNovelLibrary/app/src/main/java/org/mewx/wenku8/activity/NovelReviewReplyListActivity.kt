package org.mewx.wenku8.activity

import android.app.Activity

/**
 * Created by MewX on 2018/7/12.
 * Novel Review Activity.
 */
class NovelReviewReplyListActivity : BaseMaterialActivity(), MyItemLongClickListener {
    // private vars
    private var rid = 1
    private var reviewTitle: String? = ""

    // components
    private var mLayoutManager: LinearLayoutManager? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mLoadingLayout: LinearLayout? = null
    private var mRecyclerView: RecyclerView? = null
    private var mLoadingStatusTextView: TextView? = null
    private var mLoadingButton: TextView? = null
    private var etReplyText: EditText? = null

    // switcher
    private var mAdapter: ReviewReplyItemAdapter? = null
    private var reviewReplyList: ReviewReplyList? = ReviewReplyList()
    var pastVisibleItems = 0
    var visibleItemCount = 0
    var totalItemCount = 0
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_novel_review_reply_list)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch values
        rid = getIntent().getIntExtra("rid", 1)
        reviewTitle = getIntent().getStringExtra("title")

        // get views and set title
        // get views
        mLoadingLayout = findViewById(R.id.list_loading)
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        mRecyclerView = findViewById(R.id.review_item_list)
        mLoadingStatusTextView = findViewById(R.id.list_loading_status)
        mLoadingButton = findViewById(R.id.btn_loading)
        etReplyText = findViewById(R.id.review_reply_edit_text)
        val llReplyButton: LinearLayout = findViewById(R.id.review_reply_send)
        mRecyclerView.setHasFixedSize(false)
        val horizontalDecoration = DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL)
        val horizontalDivider: Drawable = ContextCompat.getDrawable(getApplication(), R.drawable.divider_horizontal)
        if (horizontalDivider != null) {
            horizontalDecoration.setDrawable(horizontalDivider)
            mRecyclerView.addItemDecoration(horizontalDecoration)
        }
        // use a linear layout manager
        mLayoutManager = LinearLayoutManager(this)
        mRecyclerView.setLayoutManager(mLayoutManager)

        // Listener
        mRecyclerView.addOnScrollListener(MyOnScrollListener())

        // set click event for retry and cancel loading
        mLoadingButton.setOnClickListener { v -> AsyncReviewReplyListLoader(this, mSwipeRefreshLayout, rid, reviewReplyList).execute() } // retry loading
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.myAccentColor))
        mSwipeRefreshLayout.setOnRefreshListener { refreshReviewReplyList() }
        llReplyButton.setOnClickListener { ignored ->
            val temp: String = etReplyText.getText().toString()
            val badWord: String = Wenku8API.searchBadWords(temp)
            if (badWord != null) {
                Toast.makeText(getApplication(), String.format(getResources().getString(R.string.system_containing_bad_word), badWord), Toast.LENGTH_SHORT).show()
            } else if (temp.length() < Wenku8API.MIN_REPLY_TEXT) {
                Toast.makeText(getApplication(), getResources().getString(R.string.system_review_too_short), Toast.LENGTH_SHORT).show()
            } else {
                // hide ime
                val imm: InputMethodManager? = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
                var view: View? = getCurrentFocus()
                if (view == null) view = View(this)
                if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0)

                // submit
                AsyncPublishReply(etReplyText, this, rid, temp).execute()
            }
        }

        // load initial content
        AsyncReviewReplyListLoader(this, mSwipeRefreshLayout, rid, reviewReplyList).execute()
    }

    fun refreshReviewReplyList() {
        // reload all
        reviewReplyList = ReviewReplyList()
        mAdapter = null
        AsyncReviewReplyListLoader(this, mSwipeRefreshLayout, rid, reviewReplyList).execute()
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (getSupportActionBar() != null && reviewTitle != null && !reviewTitle.isEmpty()) getSupportActionBar().setTitle(reviewTitle)
        return true
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    fun onBackPressed() {
        val temp: String = etReplyText.getText().toString().trim()
        if (!temp.isEmpty()) {
            // TODO: new window to verify exit or not
        } else {
            super.onBackPressed()
        }
    }

    fun getAdapter(): ReviewReplyItemAdapter? {
        return mAdapter
    }

    fun setAdapter(adapter: ReviewReplyItemAdapter?) {
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
    fun onItemLongClick(view: View?, position: Int) {
        val content: String = reviewReplyList.getList().get(position).getContent()
        val clipboard: ClipboardManager? = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip: ClipData = ClipData.newPlainText(getResources().getString(R.string.app_name), content)
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this@NovelReviewReplyListActivity,
                    String.format(getResources().getString(R.string.system_copied_to_clipboard), content),
                    Toast.LENGTH_SHORT).show()
        }
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
                if (reviewReplyList.getCurrentPage() < reviewReplyList.getTotalPage()) {
                    // load more toast
                    Snackbar.make(mRecyclerView, ((getResources().getString(R.string.list_loading)
                            + "(" + Integer.toString(reviewReplyList.getCurrentPage() + 1)).toString() + "/" + reviewReplyList.getTotalPage()).toString() + ")",
                            Snackbar.LENGTH_SHORT).show()
                    AsyncReviewReplyListLoader(this@NovelReviewReplyListActivity, mSwipeRefreshLayout, rid, reviewReplyList).execute()
                }
            }
        }
    }

    private class AsyncReviewReplyListLoader internal constructor(@NonNull novelReviewListActivity: NovelReviewReplyListActivity?, @NonNull swipeRefreshLayout: SwipeRefreshLayout?, rid: Int, @NonNull reviewReplyList: ReviewReplyList?) : AsyncTask<Void?, Void?, Void?>() {
        private val novelReviewListActivityWeakReference: WeakReference<NovelReviewReplyListActivity?>?
        private val swipeRefreshLayoutWeakReference: WeakReference<SwipeRefreshLayout?>?
        private val rid: Int
        private val reviewReplyList: ReviewReplyList?
        private var runOrNot = true // by default, run it
        private var metNetworkIssue = false

        init {
            novelReviewListActivityWeakReference = WeakReference(novelReviewListActivity)
            swipeRefreshLayoutWeakReference = WeakReference(swipeRefreshLayout)
            this.rid = rid
            this.reviewReplyList = reviewReplyList
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
                val tempActivity: NovelReviewReplyListActivity = novelReviewListActivityWeakReference.get()
                if (tempActivity != null) tempActivity.hideRetryButton()
            }
        }

        @Override
        protected fun doInBackground(vararg v: Void?): Void? {
            if (!runOrNot || reviewReplyList.getCurrentPage() + 1 > reviewReplyList.getTotalPage()) return null

            // load current page + 1
            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getCommentContentParams(rid, reviewReplyList.getCurrentPage() + 1))
            if (tempXml == null) {
                metNetworkIssue = true
                return null // network issue
            }
            val xml = String(tempXml, Charset.forName("UTF-8"))
            Log.d(NovelReviewReplyListActivity::class.java.getSimpleName(), xml)
            Wenku8Parser.parseReviewReplyList(reviewReplyList, xml)
            return null
        }

        @Override
        protected fun onPostExecute(v: Void?) {
            // refresh everything when required
            if (!runOrNot) return
            val tempActivity: NovelReviewReplyListActivity = novelReviewListActivityWeakReference.get()
            if (metNetworkIssue) {
                // met net work issue, show retry button
                if (tempActivity != null) tempActivity.showRetryButton()
            } else {
                // all good, update list
                if (tempActivity.getAdapter() == null) {
                    val reviewReplyItemAdapter = ReviewReplyItemAdapter(reviewReplyList)
                    tempActivity.setAdapter(reviewReplyItemAdapter)
                    reviewReplyItemAdapter.setOnItemLongClickListener(tempActivity)
                    tempActivity.getRecyclerView().setAdapter(reviewReplyItemAdapter)
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

    private class AsyncPublishReply internal constructor(@NonNull editText: EditText?, @NonNull activity: NovelReviewReplyListActivity?, rid: Int, @NonNull content: String?) : AsyncTask<String?, Void?, Integer?>() {
        private val editTextWeakReference: WeakReference<EditText?>?
        private val activityWeakReference: WeakReference<NovelReviewReplyListActivity?>?
        private val rid: Int
        private val content: String?
        private var runOrNot = true // true - run

        init {
            editTextWeakReference = WeakReference(editText)
            activityWeakReference = WeakReference(activity)
            this.rid = rid
            this.content = content
        }

        @Override
        protected fun onPreExecute() {
            if (isLoading.getAndSet(true)) {
                runOrNot = false // do not run
            } else {
                // disable text and submit
                val editText: EditText = editTextWeakReference.get()
                if (editText != null) {
                    editText.setEnabled(false)
                }
            }
        }

        @Override
        protected fun doInBackground(vararg strings: String?): Integer? {
            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL, Wenku8API.getCommentReplyParams(rid, content))
                    ?: return 0
            // network issue
            val xml: String = String(tempXml, Charset.forName("UTF-8")).trim()
            Log.d(NovelReviewReplyListActivity::class.java.getSimpleName(), xml)
            return Integer.valueOf(xml)
        }

        @Override
        protected fun onPostExecute(i: Integer?) {
            if (runOrNot) {
                val editText: EditText = editTextWeakReference.get()
                val activity: NovelReviewReplyListActivity = activityWeakReference.get()
                when (i) {
                    1 -> {
                        // successful -> clear and enable edit text
                        if (editText != null) {
                            editText.setText("")
                        }

                        // refresh page
                        if (activity != null) {
                            activity.refreshReviewReplyList()
                        }
                    }

                    11 -> if (activity != null) {
                        Toast.makeText(activity, activity.getResources().getString(R.string.system_post_locked), Toast.LENGTH_SHORT).show()
                    }

                    else ->                         // met network or other issue
                        if (activity != null) {
                            Toast.makeText(activity, activity.getResources().getString(R.string.system_network_error), Toast.LENGTH_SHORT).show()
                        }
                }

                // enable edit text
                if (editText != null) {
                    editText.setEnabled(true)
                }
                isLoading.set(false)
            }
        }

        companion object {
            private val isLoading: AtomicBoolean? = AtomicBoolean(false)
        }
    }

    companion object {
        private val isLoading: AtomicBoolean? = AtomicBoolean(false)
    }
}