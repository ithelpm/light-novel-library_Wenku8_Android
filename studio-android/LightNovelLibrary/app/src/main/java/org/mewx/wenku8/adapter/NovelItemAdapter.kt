package org.mewx.wenku8.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by MewX on 2015/1/20.
 * Olde Novel Item Adapter.
 */
class NovelItemAdapter : RecyclerView.Adapter<NovelItemAdapter.ViewHolder?> {
    private var mItemClickListener: MyItemClickListener? = null
    private var mMyOptionClickListener: MyOptionClickListener? = null
    private var mItemLongClickListener: MyItemLongClickListener? = null
    private var mDataset: List<NovelItemInfoUpdate?>?

    // empty list, then use append method to add list elements
    constructor() {
        mDataset = ArrayList()
    }

    constructor(dataset: List<NovelItemInfoUpdate?>?) : super() {
        mDataset = dataset // reference
    }

    fun RefreshDataset(dataset: List<NovelItemInfoUpdate?>?) {
        mDataset = dataset // reference
    }

    @Override
    @NonNull
    fun onCreateViewHolder(@NonNull viewGroup: ViewGroup?, i: Int): ViewHolder? {
        val view: View = View.inflate(viewGroup.getContext(), R.layout.view_novel_item, null)
        return ViewHolder(view, mItemClickListener, mMyOptionClickListener, mItemLongClickListener)
    }

    @Override
    fun onBindViewHolder(@NonNull viewHolder: ViewHolder?, i: Int) {

        // set text
        if (viewHolder.tvNovelTitle != null) viewHolder.tvNovelTitle.setText(mDataset.get(i).title)
        if (viewHolder.tvNovelAuthor != null) viewHolder.tvNovelAuthor.setText(mDataset.get(i).author)
        if (viewHolder.tvNovelStatus != null) viewHolder.tvNovelStatus.setText(mDataset.get(i).status)
        if (viewHolder.tvNovelUpdate != null) viewHolder.tvNovelUpdate.setText(mDataset.get(i).update)
        if (viewHolder.tvNovelIntro != null) viewHolder.tvNovelIntro.setText(mDataset.get(i).intro_short)

        // need to solve flicking problem
        // FIXME: these imgs folders are actually no in use.
        if (LightCache.testFileExist((GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + mDataset.get(i).aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getDefaultStoragePath() + "imgs" + File.separator + mDataset.get(i).aid + ".jpg", viewHolder.ivNovelCover) else if (LightCache.testFileExist((GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + mDataset.get(i).aid).toString() + ".jpg")) ImageLoader.getInstance().displayImage("file://" + GlobalConfig.getBackupStoragePath() + "imgs" + File.separator + mDataset.get(i).aid + ".jpg", viewHolder.ivNovelCover) else ImageLoader.getInstance().displayImage(Wenku8API.getCoverURL(mDataset.get(i).aid), viewHolder.ivNovelCover)
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
    class ViewHolder internal constructor(itemView: View?, clickListener: MyItemClickListener?, myOptionClickListener: MyOptionClickListener?, longClickListener: MyItemLongClickListener?) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener {
        private val mClickListener: MyItemClickListener?
        private val mMyOptionClickListener: MyOptionClickListener?
        private val mLongClickListener: MyItemLongClickListener?
        var position = 0
        private val ibNovelOption: ImageButton?
        var ivNovelCover: ImageView?
        var tvNovelTitle: TextView?
        var tvNovelStatus: TextView?
        var tvNovelAuthor: TextView?
        var tvNovelUpdate: TextView?
        var tvNovelIntro: TextView?

        init {
            mClickListener = clickListener
            mMyOptionClickListener = myOptionClickListener
            mLongClickListener = longClickListener
            itemView.findViewById(R.id.item_card).setOnClickListener(this)
            itemView.findViewById(R.id.item_card).setOnLongClickListener(this)
            itemView.findViewById(R.id.novel_option).setOnClickListener(this)

            // get all views
            ibNovelOption = itemView.findViewById(R.id.novel_option)
            ivNovelCover = itemView.findViewById(R.id.novel_cover)
            tvNovelTitle = itemView.findViewById(R.id.novel_title)
            tvNovelAuthor = itemView.findViewById(R.id.novel_author)
            tvNovelStatus = itemView.findViewById(R.id.novel_status)
            tvNovelUpdate = itemView.findViewById(R.id.novel_update)
            tvNovelIntro = itemView.findViewById(R.id.novel_intro)

            // test current fragment
            if (!GlobalConfig.testInBookshelf()) {
                ibNovelOption.setVisibility(View.INVISIBLE)
            }
            if (GlobalConfig.testInLatest()) {
                itemView.findViewById(R.id.novel_status_row).setVisibility(View.GONE)
                (itemView.findViewById(R.id.novel_item_text_author) as TextView).setText(MyApp.getContext().getResources().getString(R.string.novel_item_hit_with_colon))
                (itemView.findViewById(R.id.novel_item_text_update) as TextView).setText(MyApp.getContext().getResources().getString(R.string.novel_item_push_with_colon))
                (itemView.findViewById(R.id.novel_item_text_shortinfo) as TextView).setText(MyApp.getContext().getResources().getString(R.string.novel_item_fav_with_colon))
            }
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
}