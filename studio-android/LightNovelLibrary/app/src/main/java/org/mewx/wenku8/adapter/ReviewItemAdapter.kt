package org.mewx.wenku8.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by MewX on 2018/7/12.
 * Review List Item Adapter.
 */
class ReviewItemAdapter(reviewList: ReviewList?) : RecyclerView.Adapter<ReviewItemAdapter.ViewHolder?>() {
    private var mItemClickListener: MyItemClickListener? = null
    private val reviewList: ReviewList?

    init {
        this.reviewList = reviewList
    }

    @Override
    @NonNull
    fun onCreateViewHolder(@NonNull viewGroup: ViewGroup?, viewType: Int): ViewHolder? {
        val view: View = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_review_post_item, viewGroup, false)
        return ViewHolder(view, mItemClickListener)
    }

    fun setOnItemClickListener(listener: MyItemClickListener?) {
        mItemClickListener = listener
    }

    @Override
    fun onBindViewHolder(@NonNull viewHolder: ViewHolder?, position: Int) {
        val review: ReviewList.Review = reviewList.getList().get(position)
        if (viewHolder.tvReviewTitle != null) viewHolder.tvReviewTitle.setText(review.getTitle())
        if (viewHolder.tvPostTime != null) viewHolder.tvPostTime.setText(DATE_FORMATTER.format(review.getPostTime()))
        if (viewHolder.tvReviewAuthor != null) viewHolder.tvReviewAuthor.setText(review.getUserName())
        if (viewHolder.tvNumberOfReplies != null) viewHolder.tvNumberOfReplies.setText(String.format(Locale.CHINA, "%d", review.getNoReplies()))
    }

    @Override
    fun getItemCount(): Int {
        return reviewList.getList().size()
    }

    class ViewHolder(view: View?, clickListener: MyItemClickListener?) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val mClickListener: MyItemClickListener?
        var tvReviewTitle: TextView?
        var tvPostTime: TextView?
        var tvReviewAuthor: TextView?
        var tvNumberOfReplies: TextView?

        init {
            mClickListener = clickListener
            view.findViewById(R.id.item_card).setOnClickListener(this)

            // real content
            tvReviewTitle = view.findViewById(R.id.review_title)
            tvPostTime = view.findViewById(R.id.review_item_post_time)
            tvReviewAuthor = view.findViewById(R.id.review_item_author)
            tvNumberOfReplies = view.findViewById(R.id.review_item_number_of_posts)
        }

        @Override
        fun onClick(v: View?) {
            if (mClickListener != null) {
                mClickListener.onItemClick(v, getAdapterPosition())
            }
        }
    }

    companion object {
        private val DATE_FORMATTER: SimpleDateFormat? = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }
}