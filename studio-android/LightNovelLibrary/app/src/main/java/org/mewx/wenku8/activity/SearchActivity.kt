package org.mewx.wenku8.activity

import android.content.Intent

/**
 * Created by MewX on 2015/5/7.
 * Search Activity.
 */
class SearchActivity : BaseMaterialActivity(), MyItemClickListener, MyItemLongClickListener {
    // private vars
    private var toolbarSearchView: EditText? = null
    private var adapter: SearchHistoryAdapter? = null
    private var historyList: List<String?>? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_search, StatusBarColor.WHITE)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // bind views
        toolbarSearchView = findViewById(R.id.search_view)
        val searchClearButton: View = findViewById(R.id.search_clear)

        // Clear search text when clear button is tapped
        searchClearButton.setOnClickListener { v -> toolbarSearchView.setText("") }

        // set search clear icon color
        val searchClearIcon: ImageView = findViewById(R.id.search_clear_icon)
        searchClearIcon.setColorFilter(getResources().getColor(R.color.mySearchToggleColor), PorterDuff.Mode.SRC_ATOP)

        // set history list
        val mLayoutManager = LinearLayoutManager(this)
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL)
        val mRecyclerView: RecyclerView = this.findViewById(R.id.search_history_list)
        mRecyclerView.setHasFixedSize(true) // set variable size
        mRecyclerView.setItemAnimator(DefaultItemAnimator())
        mRecyclerView.setLayoutManager(mLayoutManager)
        historyList = GlobalConfig.getSearchHistory()
        adapter = SearchHistoryAdapter(historyList)
        adapter.setOnItemClickListener(this) // add item click listener
        adapter.setOnItemLongClickListener(this) // add item click listener
        mRecyclerView.setAdapter(adapter)

        // set search action
        toolbarSearchView.setOnEditorActionListener { v, actionId, event ->
            // purify
            val temp: String = toolbarSearchView.getText().toString().trim()
            if (temp.length() === 0) return@setOnEditorActionListener false

            // real action
            //Toast.makeText(MyApp.getContext(), temp, Toast.LENGTH_SHORT).show();
            GlobalConfig.addSearchHistory(temp)
            refreshHistoryList()

            // jump to search
            val intent = Intent(this@SearchActivity, SearchResultActivity::class.java)
            intent.putExtra("key", temp)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // long-press will cause repetitions
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.hold)
            false
        }
    }

    @Override
    protected fun onResume() {
        super.onResume()

        // set back arrow icon
        val upArrow: Drawable = getResources().getDrawable(R.drawable.ic_svg_back)
        if (upArrow != null && getSupportActionBar() != null) {
            upArrow.setColorFilter(getResources().getColor(R.color.mySearchToggleColor), PorterDuff.Mode.SRC_ATOP)
            getSupportActionBar().setHomeAsUpIndicator(upArrow)
        }
        refreshHistoryList()
    }

    private fun refreshHistoryList() {
        historyList = GlobalConfig.getSearchHistory()
        if (adapter != null) {
            adapter.notifyDataSetChanged() // may be back from search list
        }
    }

    @Override
    fun onItemClick(view: View?, position: Int) {
        if (position < 0 || position >= historyList.size()) {
            // ArrayIndexOutOfBoundsException
            Toast.makeText(this, "ArrayIndexOutOfBoundsException: " + position + " in size " + historyList.size(), Toast.LENGTH_SHORT).show()
            return
        }
        toolbarSearchView.setText(historyList.get(position)) // copy text to editText
        toolbarSearchView.setSelection(historyList.get(position).length()) // move cursor
    }

    @Override
    fun onItemLongClick(view: View?, position: Int) {
        Builder(this)
                .onPositive { ignored1, ignored2 ->
                    GlobalConfig.deleteSearchHistory(historyList.get(position))
                    refreshHistoryList()
                }
                .theme(Theme.LIGHT)
                .backgroundColorRes(R.color.dlgBackgroundColor)
                .contentColorRes(R.color.dlgContentColor)
                .positiveColorRes(R.color.dlgPositiveButtonColor)
                .negativeColorRes(R.color.dlgNegativeButtonColor)
                .title(getResources().getString(R.string.dialog_content_delete_one_search_record))
                .content(historyList.get(position))
                .contentGravity(GravityEnum.CENTER)
                .positiveText(R.string.dialog_positive_likethis)
                .negativeText(R.string.dialog_negative_preferno)
                .show()
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
        super.onBackPressed()

        // leave animation: fade out
        overridePendingTransition(0, R.anim.fade_out)
    }
}