package org.mewx.wenku8.activity

import android.os.Bundle

/**
 * Created by MewX on 2015/7/29.
 * About activity.
 */
class AboutActivity : BaseMaterialActivity() {
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_about)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // get version code
        val tvVersion: TextView = findViewById(R.id.app_version)
        tvVersion.setText(String.format(getResources().getString(R.string.about_version_template), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(menuItem)
    }
}