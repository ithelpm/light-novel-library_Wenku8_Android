package org.mewx.wenku8.fragment

import android.content.Context

class ConfigFragment : Fragment() {
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Override
    fun onCreateView(@NonNull inflater: LayoutInflater?, container: ViewGroup?,
                     savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    @Override
    fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // get views
        val tvNotice: TextView = Objects.requireNonNull(getActivity()).findViewById(R.id.notice)
        if (Wenku8API.NoticeString.equals("")) getActivity().findViewById(R.id.notice_layout).setVisibility(View.GONE) else {
            var sequence: CharSequence = Html.fromHtml(Wenku8API.NoticeString.trim())
            // remove trailing spaces
            var i: Int = sequence.length() - 1
            while (i >= 0 && Character.isWhitespace(sequence.charAt(i))) i--
            sequence = sequence.subSequence(0, i + 1)

            // parse html
            val strBuilder = SpannableStringBuilder(sequence)
            val urls: Array<URLSpan?> = strBuilder.getSpans(0, sequence.length(), URLSpan::class.java)
            for (span in urls) {
                val start: Int = strBuilder.getSpanStart(span)
                val end: Int = strBuilder.getSpanEnd(span)
                val flags: Int = strBuilder.getSpanFlags(span)
                val clickable: ClickableSpan = object : ClickableSpan() {
                    fun onClick(view: View?) {
                        // Do something with span.getURL() to handle the link click...
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GlobalConfig.blogPageUrl))
                        Objects.requireNonNull(getContext()).startActivity(browserIntent)
                    }
                }
                strBuilder.setSpan(clickable, start, end, flags)
                strBuilder.removeSpan(span)
            }
            tvNotice.setText(strBuilder)
            tvNotice.setMovementMethod(LinkMovementMethod.getInstance())
        }
        getActivity().findViewById(R.id.btn_choose_language).setOnClickListener { v ->
            Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title(R.string.config_choose_language)
                    .content(R.string.dialog_content_language_tip)
                    .items(R.array.choose_language_option)
                    .itemsCallback { dialog, view, which, text ->
                        // 0 means Simplified Chinese; 1 means Traditional Chinese.
                        val selected: Wenku8API.LANG = if (which === 0) Wenku8API.LANG.SC else Wenku8API.LANG.TC
                        if (selected === GlobalConfig.getCurrentLang()) {
                            Toast.makeText(getActivity(), "Already in.", Toast.LENGTH_SHORT).show()
                            return@itemsCallback
                        }

                        // Selected a different languages.
                        GlobalConfig.setCurrentLang(selected)
                        val intent = Intent()
                        intent.setClass(getActivity(), MainActivity::class.java)
                        startActivity(intent)
                        getActivity().overridePendingTransition(R.anim.fade_in, R.anim.hold) // fade in animation
                        getActivity().finish() // destroy itself
                    }
                    .show()
        }
        getActivity().findViewById(R.id.btn_clear_cache).setOnClickListener { v ->
            Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title(R.string.config_clear_cache)
                    .items(R.array.wipe_cache_option)
                    .itemsCallback { dialog, view, which, text ->
                        if (which === 0) {
                            AsyncDeleteFast(getActivity()).execute()
                        } else if (which === 1) {
                            AsyncDeleteSlow(getActivity()).execute()
                        }
                    }
                    .show()
        }
        getActivity().findViewById(R.id.btn_navigation_drawer_wallpaper).setOnClickListener { v ->
            val intent = Intent(getActivity(), MenuBackgroundSelectorActivity::class.java)
            startActivity(intent)
        }
        getActivity().findViewById(R.id.btn_check_update).setOnClickListener { v -> CheckAppNewVersion(getActivity(), true).execute() }
        getActivity().findViewById(R.id.btn_about).setOnClickListener { v ->
            val intent = Intent(getActivity(), AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private class AsyncDeleteFast internal constructor(context: Context?) : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        private val contextWeakReference: WeakReference<Context?>?
        private var md: MaterialDialog? = null

        init {
            contextWeakReference = WeakReference(context)
        }

        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            val ctx: Context = contextWeakReference.get()
            if (ctx != null) {
                md = Builder(ctx)
                        .theme(Theme.LIGHT)
                        .title(R.string.config_clear_cache)
                        .content(R.string.dialog_content_wipe_cache_fast)
                        .progress(true, 0)
                        .cancelable(false)
                        .show()
            }
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            // covers
            var dir: File? = File(GlobalConfig.getDefaultStoragePath() + "imgs")
            if (!dir.exists()) dir = File(GlobalConfig.getBackupStoragePath() + "imgs")
            var childFile: Array<File?> = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    val temp: Array<String?> = f.getAbsolutePath().split("/")
                    if (temp.size != 0) {
                        val id: String = temp[temp.size - 1].split("\\.").get(0)
                        if (LightTool.isInteger(id) && !GlobalConfig.testInLocalBookshelf(Integer.parseInt(id)) && !f.delete()) Log.d(ConfigFragment::class.java.getSimpleName(), "Failed to delete file: " + f.getAbsolutePath()) // ignore ".nomedia"
                    }
                }
            }

            // cache
            dir = File(GlobalConfig.getDefaultStoragePath() + "cache")
            if (!dir.exists()) dir = File(GlobalConfig.getBackupStoragePath() + "cache")
            childFile = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    if (!f.delete()) Log.d(ConfigFragment::class.java.getSimpleName(), "Failed to delete file: " + f.getAbsolutePath())
                }
            }
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            if (md != null) md.dismiss()
            val ctx: Context = contextWeakReference.get()
            if (ctx != null) {
                Toast.makeText(ctx, "OK", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class AsyncDeleteSlow internal constructor(context: Context?) : AsyncTask<Integer?, Integer?, Wenku8Error.ErrorCode?>() {
        private val contextWeakReference: WeakReference<Context?>?
        private var md: MaterialDialog? = null
        private var isLoading = false

        init {
            contextWeakReference = WeakReference(context)
        }

        @Override
        protected fun onPreExecute() {
            super.onPreExecute()
            val ctx: Context = contextWeakReference.get()
            if (ctx != null) {
                md = Builder(Objects.requireNonNull(ctx))
                        .theme(Theme.LIGHT)
                        .cancelListener { dialog ->
                            isLoading = false
                            this@AsyncDeleteSlow.cancel(true)
                        }
                        .title(R.string.config_clear_cache)
                        .content(R.string.dialog_content_wipe_cache_slow)
                        .progress(true, 0)
                        .cancelable(true)
                        .show()
            }
            isLoading = true
        }

        @Override
        protected fun doInBackground(vararg params: Integer?): Wenku8Error.ErrorCode? {
            // covers
            var dir: File? = File(GlobalConfig.getDefaultStoragePath() + "imgs")
            if (!dir.exists()) dir = File(GlobalConfig.getBackupStoragePath() + "imgs")
            var childFile: Array<File?> = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    val temp: Array<String?> = f.getAbsolutePath().split("/")
                    if (temp.size != 0) {
                        val id: String = temp[temp.size - 1].split("\\.").get(0)
                        if (LightTool.isInteger(id) && !GlobalConfig.testInLocalBookshelf(Integer.parseInt(id)) && !f.delete()) Log.d(ConfigFragment::class.java.getSimpleName(), "Failed to delete file: " + f.getAbsolutePath()) // ignore ".nomedia"
                    }
                }
            }

            // cache
            dir = File(GlobalConfig.getDefaultStoragePath() + "cache")
            if (!dir.exists()) dir = File(GlobalConfig.getBackupStoragePath() + "cache")
            childFile = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    if (!f.delete()) Log.d(ConfigFragment::class.java.getSimpleName(), "Failed to delete file: " + f.getAbsolutePath())
                }
            }
            if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK

            // get saved picture filename list. Rec all /wenku8/saves/novel, get all picture name, then delete if not in list
            val listPicture: List<String?> = ArrayList()
            dir = File(GlobalConfig.getFirstFullSaveFilePath() + "novel")
            if (!dir.exists()) dir = File(GlobalConfig.getSecondFullSaveFilePath() + "novel")
            childFile = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                    val temp: ByteArray = LightCache.loadFile(f.getAbsolutePath()) ?: continue
                    try {
                        val list: List<OldNovelContentParser.NovelContent?> = OldNovelContentParser.NovelContentParser_onlyImage(String(temp, "UTF-8"))
                        for (nv in list) listPicture.add(GlobalConfig.generateImageFileNameByURL(nv.content))
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            }

            // loop for images
            dir = File(GlobalConfig.getFirstFullSaveFilePath() + "imgs")
            if (!dir.exists()) dir = File(GlobalConfig.getSecondFullSaveFilePath() + "imgs")
            childFile = dir.listFiles()
            if (childFile != null && childFile.size != 0) {
                for (f in childFile) {
                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK
                    val temp: Array<String?> = f.getAbsolutePath().split("/")
                    if (temp.size != 0) {
                        val name = temp[temp.size - 1]
                        if (!listPicture.contains(name) && !f.delete()) Log.d(ConfigFragment::class.java.getSimpleName(), "Failed to delete file: " + f.getAbsolutePath())
                    }
                }
            }
            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            isLoading = false
            if (md != null) md.dismiss()
            val ctx: Context = contextWeakReference.get() ?: return
            if (errorCode === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                Toast.makeText(ctx, "OK", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(ctx, errorCode.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun newInstance(): ConfigFragment? {
            return ConfigFragment()
        }
    }
}