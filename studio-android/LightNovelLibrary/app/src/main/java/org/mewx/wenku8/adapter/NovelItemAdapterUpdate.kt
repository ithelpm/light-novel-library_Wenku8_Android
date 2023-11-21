package org.mewx.wenku8.adapter

import android.annotation.SuppressLint

/**
 * Created by MewX on 2015/1/20.
 * Updated version of Novel Item Adapter.
 */
class NovelItemAdapterUpdate : RecyclerView.Adapter<NovelItemAdapterUpdate.ViewHolder?> {
    private var mItemClickListener: MyItemClickListener? = null
    private var mMyOptionClickListener: MyOptionClickListener? = null
    private var mItemLongClickListener: MyItemLongClickListener? = null
    private var mDataset: List<NovelItemInfoUpdate?>?

    // empty list, then use append method to add list elements
    constructor() {
        mDataset = ArrayList()
    }

    constructor(dataset: List<NovelItemInfoUpdate?>?) : super() {
        mDataset = dataset
    }

    fun refreshDataset(dataset: List<NovelItemInfoUpdate?>?) {
        mDataset = dataset
    }

    @Override
    @NonNull
    fun onCreateViewHolder(@NonNull viewGroup: ViewGroup?, i: Int): ViewHolder? {
        val view: View = View.inflate(viewGroup.getContext(), R.layout.view_novel_item, null)
        return ViewHolder(view, mItemClickListener, mMyOptionClickListener, mItemLongClickListener)
    }

    @Override
    fun onBindViewHolder(@NonNull viewHolder: ViewHolder?, i: Int) {
        if (!mDataset.get(i).isInitialized()) {
            refreshAllContent(viewHolder, mDataset.get(i))
        } else if (!viewHolder.isLoading.get()) {
            // Have to cache the aid here in UI thread.
            AsyncLoadNovelIntro(mDataset.get(i).aid, viewHolder).execute()
        }
    }

    private fun refreshAllContent(viewHolder: ViewHolder?, info: NovelItemInfoUpdate?) {
        // unknown NPE, just make
        if (viewHolder == null || mDataset == null || info == null) return

        // set text
        viewHolder.tvNovelTitle.setText(info.title)
        viewHolder.tvNovelAuthor.setText(info.author)
        viewHolder.tvNovelStatus.setText(info.status)
        viewHolder.tvNovelUpdate.setText(info.update)
        if (!GlobalConfig.testInBookshelf()) // show short intro
            viewHolder.tvNovelIntro.setText(info.intro_short) else if (info.latest_chapter.isEmpty()) {
            // latest chapter not set, hide it
            viewHolder.tvNovelIntro.setVisibility(View.GONE)
        } else {
            // latest chapter is set, show it
            viewHolder.tvLatestChapterNameText.setText(viewHolder.tvLatestChapterNameText.getResources().getText(R.string.novel_item_latest_chapter))
            viewHolder.tvNovelIntro.setText(info.latest_chapter)
        }

        // FIXME: these imgs folders are actually no in use.
        if (LightCache.testFileExist((GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + info.aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + info.aid + ".jpg", viewHolder.ivNovelCover) else if (LightCache.testFileExist((GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + info.aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + info.aid + ".jpg", viewHolder.ivNovelCover) else ImageLoader.getInstance().displayImage(Wenku8API.getCoverURL(info.aid), viewHolder.ivNovelCover)
    }

    @Override
    fun getItemCount(): Int {
        return mDataset.size()
    }

    fun setOnItemClickListener(listener: MyItemClickListener?) {
        mItemClickListener = listener
    }

    fun setOnDeleteClickListener(listener: MyOptionClickListener?) {
        mMyOptionClickListener = listener
    }

    fun setOnItemLongClickListener(listener: MyItemLongClickListener?) {
        mItemLongClickListener = listener
    }

    /**
     * View Holder:
     * Called by RecyclerView to display the data at the specified position.
     */
    class ViewHolder(itemView: View?, clickListener: MyItemClickListener?, myOptionClickListener: MyOptionClickListener?, longClickListener: MyItemLongClickListener?) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        private val mClickListener: MyItemClickListener?
        private val mMyOptionClickListener: MyOptionClickListener?
        private val mLongClickListener: MyItemLongClickListener?
        var position = 0
        var isLoading: AtomicBoolean? = AtomicBoolean(false)
        private val ibNovelOption: ImageButton?
        private val trNovelIntro: TableRow?
        var ivNovelCover: ImageView?
        var tvNovelTitle: TextView?
        var tvNovelStatus: TextView?
        var tvNovelAuthor: TextView?
        var tvNovelUpdate: TextView?
        var tvNovelIntro: TextView?
        var tvLatestChapterNameText: TextView?

        init {
            mClickListener = clickListener
            mMyOptionClickListener = myOptionClickListener
            mLongClickListener = longClickListener
            itemView.findViewById(R.id.item_card).setOnClickListener(this)
            itemView.findViewById(R.id.item_card).setOnLongClickListener(this)
            itemView.findViewById(R.id.novel_option).setOnClickListener(this)

            // get all views
            ibNovelOption = itemView.findViewById(R.id.novel_option)
            trNovelIntro = itemView.findViewById(R.id.novel_intro_row)
            ivNovelCover = itemView.findViewById(R.id.novel_cover)
            tvNovelTitle = itemView.findViewById(R.id.novel_title)
            tvNovelAuthor = itemView.findViewById(R.id.novel_author)
            tvNovelStatus = itemView.findViewById(R.id.novel_status)
            tvNovelUpdate = itemView.findViewById(R.id.novel_update)
            tvNovelIntro = itemView.findViewById(R.id.novel_intro)
            tvLatestChapterNameText = itemView.findViewById(R.id.novel_item_text_shortinfo)

            // test current fragment
            if (!GlobalConfig.testInBookshelf()) ibNovelOption.setVisibility(View.INVISIBLE)
        }

        @Override
        fun onClick(v: View?) {
            when (v.getId()) {
                R.id.item_card -> if (mClickListener != null) {
                    mClickListener.onItemClick(v, getAdapterPosition())
                }

                R.id.novel_option -> if (mClickListener != null) {
                    mMyOptionClickListener.onOptionButtonClick(v, getAdapterPosition())
                }
            }
        }

        @Override
        fun onLongClick(v: View?): Boolean {
            if (mLongClickListener != null) {
                mLongClickListener.onItemLongClick(v, getAdapterPosition())
            }
            return true
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class AsyncLoadNovelIntro internal constructor(private val aid: Int, private val vh: ViewHolder?) : AsyncTask<Void?, Void?, Wenku8Error.ErrorCode?>() {
        private var novelIntro: String? = null
        private val raceCondition: Boolean

        init {
            raceCondition = !vh.isLoading.compareAndSet(false, true)
        }

        @Override
        protected fun doInBackground(vararg params: Void?): Wenku8Error.ErrorCode? {
            return if (raceCondition) {
                Wenku8Error.ErrorCode.ERROR_DEFAULT
            } else try {
                val res: ByteArray = LightNetwork.LightHttpPostConnection(Wenku8API.BASE_URL,
                        Wenku8API.getNovelShortInfoUpdate_CV(aid, GlobalConfig.getCurrentLang()))
                        ?: return Wenku8Error.ErrorCode.ERROR_DEFAULT
                novelIntro = String(res, "UTF-8")
                Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
                Wenku8Error.ErrorCode.ERROR_DEFAULT
            }
        }

        @Override
        protected fun onPostExecute(errorCode: Wenku8Error.ErrorCode?) {
            super.onPostExecute(errorCode)
            if (errorCode === Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED) {
                // The index might have been changed. We need to find the correct index again.
                var currentIndex = -1
                for (j in 0 until mDataset.size()) {
                    if (mDataset.get(j).aid === aid) {
                        currentIndex = j
                        break
                    }
                }

                // Update info, but we need to validate the index first.
                if (currentIndex >= 0) {
                    val info: NovelItemInfoUpdate = NovelItemInfoUpdate.parse(novelIntro)
                    mDataset.set(currentIndex, info)
                    notifyItemChanged(currentIndex)
                }
            }
            vh.isLoading.set(false)
        }
    }
}