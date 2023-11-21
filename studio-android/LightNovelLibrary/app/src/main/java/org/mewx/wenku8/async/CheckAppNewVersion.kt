package org.mewx.wenku8.async

import android.content.Context

class CheckAppNewVersion(context: Context?, verbose: Boolean) : AsyncTask<Void?, Void?, Integer?>() {
    private val contextWeakReference: WeakReference<Context?>?
    private val verboseMode: Boolean

    constructor(context: Context?) : this(context, false)

    /**
     * Check whether there's a new version of the app published.
     *
     * @param context the context used for showing dialogs.
     * @param verbose whether to actively show error messages.
     */
    init {
        contextWeakReference = WeakReference(context)
        verboseMode = verbose
    }

    @Override
    protected fun doInBackground(vararg voids: Void?): Integer? {
        // load latest version code
        val codeByte: ByteArray = LightNetwork.LightHttpDownload(GlobalConfig.versionCheckUrl)
                ?: return -1
        // time out

        // parse the version code
        val code: String = String(codeByte).trim()
        Log.d("MewX", "latest version code: $code")
        return if (code.isEmpty() || !TextUtils.isDigitsOnly(code)) -2 // parse error
        else Integer.parseInt(code)
    }

    @Override
    protected fun onPostExecute(code: Integer?) {
        super.onPostExecute(code)
        val ctx: Context = contextWeakReference.get() ?: return
        if (code < 0) {
            // Logging different errors.
            if (code == -1) {
                Log.e("MewX", "unable to fetch latest version")
            } else if (code == -2) {
                Log.e("MewX", "unable to parse version")
            }

            // TODO: show different error messages.
            if (verboseMode) {
                Toast.makeText(ctx, ctx.getResources().getString(R.string.system_update_timeout), Toast.LENGTH_SHORT).show()
            }
        }
        val current: Int = BuildConfig.VERSION_CODE
        if (current >= code) {
            Log.i("MewX", "no newer version")
            if (verboseMode) {
                Toast.makeText(ctx, ctx.getResources().getString(R.string.system_update_latest_version), Toast.LENGTH_SHORT).show()
            }
        } else {
            // update to new version
            Builder(ctx)
                    .theme(Theme.LIGHT)
                    .title(R.string.system_update_found_new)
                    .content(R.string.system_update_jump_to_page)
                    .positiveText(R.string.dialog_positive_sure)
                    .negativeText(R.string.dialog_negative_biao)
                    .negativeColorRes(R.color.menu_text_color)
                    .onPositive { dialog, which ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.blogPageUrl))
                        ctx.startActivity(browserIntent)
                    }
                    .show()
        }
    }
}