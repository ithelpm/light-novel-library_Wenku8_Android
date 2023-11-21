package org.mewx.wenku8.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by MewX on 2018/7/12.
 * Review List Item Adapter.
 */
class ReviewReplyItemAdapter(reviewReplyList: ReviewReplyList?) : RecyclerView.Adapter<ReviewReplyItemAdapter.ViewHolder?>() {
    private var mItemLongClickListener: MyItemLongClickListener? = null
    private val reviewReplyList: ReviewReplyList?

    init {
        this.reviewReplyList = reviewReplyList
    }

    @Override
    @NonNull
    fun onCreateViewHolder(@NonNull viewGroup: ViewGroup?, viewType: Int): ViewHolder? {
        val view: View = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_review_reply_item, viewGroup, false)
        return ViewHolder(view, mItemLongClickListener)
    }

    fun setOnItemLongClickListener(listener: MyItemLongClickListener?) {
        mItemLongClickListener = listener
    }

    @Override
    fun onBindViewHolder(@NonNull viewHolder: ViewHolder?, position: Int) {
        val reviewReply: ReviewReplyList.ReviewReply = reviewReplyList.getList().get(position)
        if (viewHolder.tvUserName != null) viewHolder.tvUserName.setText(String.format("[%s]", reviewReply.getUserName()))
        if (viewHolder.tvReplyTime != null) viewHolder.tvReplyTime.setText(DATE_FORMATTER.format(reviewReply.getReplyTime()))
        if (viewHolder.tvNumberedId != null) viewHolder.tvNumberedId.setText(String.format(Locale.CHINA, "%d", position + 1))
        if (viewHolder.tvContent != null) viewHolder.tvContent.setText(reviewReply.getContent())
    }

    @Override
    fun getItemCount(): Int {
        return reviewReplyList.getList().size()
    }

    class ViewHolder(view: View?, longClickListener: MyItemLongClickListener?) : RecyclerView.ViewHolder(view), View.OnLongClickListener {
        private val mClickListener: MyItemLongClickListener?
        var tvUserName: TextView?
        var tvReplyTime: TextView?
        var tvNumberedId: TextView?
        var tvContent: TextView?

        init {
            mClickListener = longClickListener
            view.findViewById(R.id.review_reply_item).setOnLongClickListener(this)

            // real content
            tvUserName = view.findViewById(R.id.review_reply_item_user)
            tvReplyTime = view.findViewById(R.id.review_reply_item_time)
            tvNumberedId = view.findViewById(R.id.review_reply_item_numbered_id)
            tvContent = view.findViewById(R.id.review_reply_content)
        }

        @Override
        fun onLongClick(v: View?): Boolean {
            if (mClickListener != null) {
                mClickListener.onItemLongClick(v, getAdapterPosition())
            }
            return true
        }
    }

    companion object {
        private val DATE_FORMATTER: SimpleDateFormat? = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }
}