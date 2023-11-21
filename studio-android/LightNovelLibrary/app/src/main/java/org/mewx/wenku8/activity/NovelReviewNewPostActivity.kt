package org.mewx.wenku8.activity

import android.app.Activity

/**
 * Created by MewX on 2020/7/5.
 * Novel Review New Post Activity.
 */
class NovelReviewNewPostActivity : BaseMaterialActivity() {
    // components
    private var etTitle: EditText? = null
    private var etContent: EditText? = null
    @Override
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMaterialStyle(R.layout.layout_novel_review_new_post)

        // Init Firebase Analytics on GA4.
        FirebaseAnalytics.getInstance(this)

        // fetch values
        aid = getIntent().getIntExtra("aid", 1)

        // get views
        etTitle = findViewById(R.id.input_title)
        etContent = findViewById(R.id.input_content)
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu_review_new_post, menu)
        return true
    }

    private fun noBadWords(text: String?): Boolean {
        val badWord: String = Wenku8API.searchBadWords(text)
        if (badWord != null) {
            Toast.makeText(getApplication(), String.format(getResources().getString(R.string.system_containing_bad_word), badWord), Toast.LENGTH_SHORT).show()
            return false
        } else if (text.length() < Wenku8API.MIN_REPLY_TEXT) {
            Toast.makeText(getApplication(), getResources().getString(R.string.system_review_too_short), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun hideIME() {
        val imm: InputMethodManager? = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        var view: View? = getCurrentFocus()
        if (view == null) view = View(this)
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0)
    }

    @Override
    fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        if (menuItem.getItemId() === android.R.id.home) {
            onBackPressed()
        } else if (menuItem.getItemId() === R.id.action_submit) {
            val title: String = etTitle.getText().toString()
            val content: String = etContent.getText().toString()
            if (noBadWords(title) && noBadWords(content)) {
                hideIME()
                AsyncSubmitNePost(this, title, content).execute()
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    @Override
    fun onBackPressed() {
        // TODO: save draft
        if (etTitle.getText().toString().trim().length() !== 0 ||
                etContent.getText().toString().trim().length() !== 0) {
            Builder(this)
                    .theme(Theme.LIGHT)
                    .title(R.string.system_warning)
                    .content(R.string.system_review_draft_will_be_lost)
                    .positiveText(R.string.dialog_positive_ok)
                    .negativeText(R.string.dialog_negative_preferno)
                    .negativeColorRes(R.color.menu_text_color)
                    .onPositive { dialog, which -> super.onBackPressed() }
                    .show()
        } else {
            super.onBackPressed()
        }
    }

    private class AsyncSubmitNePost internal constructor(activity: NovelReviewNewPostActivity?, title: String?, content: String?) : AsyncTask<Void?, Void?, Integer?>() {
        private val title: String?
        private val content: String?
        private var ran = false // whether the action is done by this instance.
        private val activityWeakReference: WeakReference<NovelReviewNewPostActivity?>?

        init {
            activityWeakReference = WeakReference(activity)
            this.title = title
            this.content = content
        }

        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            if (!isSubmitting.getAndSet(true)) {
                ran = true
                Log.d(NovelReviewNewPostActivity::class.java.getSimpleName(),
                        String.format("start submitting: [%s] %s", title, content))

                // todo: disable icon or change it to an animated icon
            }
        }

        @Override
        protected fun doInBackground(vararg voids: Void?): Integer? {
            if (!ran) return null

            // TODO: adding "Sent from Android client" at the end of content.
            val tempXml: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL,
                    Wenku8API.getCommentNewThreadParams(aid, title, content))
                    ?: return 0
            // network issue
            val xml: String = String(tempXml, Charset.forName("UTF-8")).trim()
            Log.d(NovelReviewNewPostActivity::class.java.getSimpleName(), xml)
            return Integer.valueOf(xml)
        }

        @Override
        protected fun onPostExecute(errorCode: Integer?) {
            super.onPostExecute(errorCode)
            if (!ran) return
            val activity: NovelReviewNewPostActivity = activityWeakReference.get()
            if (errorCode == null || errorCode != 1) {
                // net network or other issue
                if (activity != null) {
                    Toast.makeText(activity, activity.getResources().getString(R.string.system_network_error), Toast.LENGTH_SHORT).show()
                }
                return
            }

            // errorCode is 1 - successful.
            isSubmitting.set(false)
            Log.d(NovelReviewNewPostActivity::class.java.getSimpleName(), "finished submitting")

            // todo: clean saved draft

            // todo: return to previous screen and refresh
            if (activity != null) {
                activity.finish()
            }
        }
    }

    companion object {
        // private vars
        private var aid = 1

        // switcher
        private val isSubmitting: AtomicBoolean? = AtomicBoolean(false)
    }
}