package org.mewx.wenku8.activity

import android.graphics.PorterDuff

/**
 * Created by MewX on 2015/5/11.
 * Search Result Activity.
 */
class SearchResultActivity : BaseMaterialActivity() {
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_search_result, StatusBarColor.WHITE)

        // Init Firebase Analytics on GA4.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // get arguments
        val searchKey: String = getIntent().getStringExtra("key")

        // Analysis.
        val searchParams = Bundle()
        searchParams.putString(FirebaseAnalytics.Param.SEARCH_TERM, searchKey)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, searchParams)

        // set action bat title
        val mTextView: TextView? = findViewById(R.id.search_result_title) as TextView?
        if (mTextView != null) mTextView.setText(getResources().getString(R.string.title_search) + searchKey)

        // init values
        val bundle = Bundle()
        bundle.putString("type", "search")
        bundle.putString("key", searchKey)

        // UIL setting
        if (ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this)
        }

        // This code will produce more than one activity in stack, so I jump to new SearchActivity to escape it.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.result_fragment, NovelItemListFragment.newInstance(bundle), "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_NONE)
                .commit()
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