package org.mewx.wenku8.activity

import android.graphics.PorterDuff

/**
 * The base activity that handles Material Design style status bar or so.
 */
class BaseMaterialActivity : AppCompatActivity() {
    protected enum class HomeIndicatorStyle {
        NONE,  // TODO: implement when using this style.
        HAMBURGER, ARROW
    }

    protected enum class StatusBarColor {
        PRIMARY, WHITE, DARK
    }

    private var tintManager: SystemBarTintManager? = null
    private var toolbar: Toolbar? = null
    protected fun getTintManager(): SystemBarTintManager? {
        if (tintManager == null) {
            tintManager = SystemBarTintManager(this)
        }
        return tintManager
    }

    protected fun getToolbar(): Toolbar? {
        if (toolbar == null) {
            toolbar = findViewById(R.id.toolbar_actionbar)
        }
        return toolbar
    }

    protected fun initMaterialStyle(layoutId: Int, indicatorStyle: HomeIndicatorStyle? = HomeIndicatorStyle.ARROW) {
        initMaterialStyle(layoutId, StatusBarColor.PRIMARY, indicatorStyle)
    }

    protected fun initMaterialStyle(layoutId: Int, statusBarColor: StatusBarColor?, indicatorStyle: HomeIndicatorStyle? = HomeIndicatorStyle.ARROW) {
        setContentView(layoutId)

        // set indicator enable
        if (getToolbar() != null) {
            setSupportActionBar(getToolbar())
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setHomeButtonEnabled(true)

            // Default indicator is hamburger.
            if (indicatorStyle == HomeIndicatorStyle.ARROW) {
                val upArrow: Drawable = getResources().getDrawable(R.drawable.ic_svg_back)
                if (upArrow != null) {
                    upArrow.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP)
                }
                getSupportActionBar().setHomeAsUpIndicator(upArrow)
            }
        }

        // change status bar color tint, and this require SDK16
        // Android API 22 has more effects on status bar, so ignore
        // create our manager instance after the content view is set
        tintManager = getTintManager()
        tintManager.setStatusBarTintEnabled(true)
        tintManager.setNavigationBarTintEnabled(true)
        tintManager.setTintAlpha(if (statusBarColor == StatusBarColor.DARK) 0.9f else 0.15f)
        tintManager.setNavigationBarAlpha(if (statusBarColor == StatusBarColor.DARK) 0.8f else 0.0f)
        // set all color
        tintManager.setTintColor(getResources().getColor(android.R.color.black))

        // set Navigation bar color
        if (Build.VERSION.SDK_INT >= 21 && statusBarColor != StatusBarColor.DARK) {
            val statusBarColorId: Int = if (statusBarColor == StatusBarColor.PRIMARY) R.color.myNavigationColor else R.color.myNavigationColorWhite
            getWindow().setNavigationBarColor(getResources().getColor(statusBarColorId))
        }
    }
}