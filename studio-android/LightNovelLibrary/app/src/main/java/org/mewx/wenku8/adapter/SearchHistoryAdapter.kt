package org.mewx.wenku8.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by MewX on 2015/5/10.
 * Search History Adapter.
 */
class SearchHistoryAdapter(private val history: List<String?>?) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder?>() {
    private var mItemClickListener: MyItemClickListener? = null
    private var mItemLongClickListener: MyItemLongClickListener? = null
    @Override
    @NonNull
    fun onCreateViewHolder(@NonNull viewGroup: ViewGroup?, viewType: Int): ViewHolder? {
        val view: View = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_search_history_item, viewGroup, false)
        return ViewHolder(view, mItemClickListener, mItemLongClickListener)
    }

    fun setOnItemClickListener(listener: MyItemClickListener?) {
        mItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: MyItemLongClickListener?) {
        mItemLongClickListener = listener
    }

    @Override
    fun onBindViewHolder(@NonNull viewHolder: ViewHolder?, position: Int) {
        if (viewHolder.mTextView != null) viewHolder.mTextView.setText(history.get(position))
    }

    @Override
    fun getItemCount(): Int {
        return history.size()
    }

    class ViewHolder(view: View?, clickListener: MyItemClickListener?, longClickListener: MyItemLongClickListener?) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {
        private val mClickListener: MyItemClickListener?
        private val mLongClickListener: MyItemLongClickListener?
        var mTextView: TextView?

        init {
            mClickListener = clickListener
            mLongClickListener = longClickListener
            view.findViewById(R.id.item_card).setOnClickListener(this)
            view.findViewById(R.id.item_card).setOnLongClickListener(this)

            // real content
            mTextView = view.findViewById(R.id.search_history_text)
        }

        @Override
        fun onClick(v: View?) {
            if (mClickListener != null) {
                mClickListener.onItemClick(v, getAdapterPosition())
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