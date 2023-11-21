package org.mewx.wenku8.global.api

import java.util.ArrayList

class ReviewReplyList {
    class ReviewReply(@NonNull replyTime: Date?, @NonNull userName: String?, uid: Int, @NonNull content: String?) {
        @NonNull
        private var replyTime: Date? = null

        @NonNull
        private var userName: String? = null
        private var uid = 0 // post user

        @NonNull
        private var content: String? = null

        init {
            setReplyTime(replyTime)
            setUserName(userName)
            setUid(uid)
            setContent(content)
        }

        @NonNull
        fun getReplyTime(): Date? {
            return replyTime
        }

        fun setReplyTime(@NonNull replyTime: Date?) {
            this.replyTime = replyTime
        }

        @NonNull
        fun getUserName(): String? {
            return userName
        }

        fun setUserName(@NonNull userName: String?) {
            this.userName = userName
        }

        fun getUid(): Int {
            return uid
        }

        fun setUid(uid: Int) {
            this.uid = uid
        }

        @NonNull
        fun getContent(): String? {
            return content
        }

        fun setContent(@NonNull content: String?) {
            this.content = content
        }
    }

    private val list: List<ReviewReply?>? = ArrayList()
    private var totalPage = 1
    private var currentPage = 0 // 1-totalPage, 0 means not yet loaded
    fun getList(): List<ReviewReply?>? {
        return list
    }

    fun getTotalPage(): Int {
        return totalPage
    }

    fun setTotalPage(totalPage: Int) {
        this.totalPage = totalPage
    }

    fun getCurrentPage(): Int {
        return currentPage
    }

    fun setCurrentPage(currentPage: Int) {
        this.currentPage = currentPage
    }
}