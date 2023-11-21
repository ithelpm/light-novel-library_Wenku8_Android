package org.mewx.wenku8.global.api

import java.util.ArrayList

class ReviewList {
    class Review(rid: Int, @NonNull postTime: Date?, noReplies: Int, @NonNull lastReplyTime: Date?, @NonNull userName: String?, uid: Int, @NonNull title: String?) {
        private var rid = 0 // review id

        @NonNull
        private var postTime: Date? = Date()
        private var noReplies = 0

        @NonNull
        private var lastReplyTime: Date? = Date()

        @NonNull
        private var userName: String? = ""
        private var uid = 0 // post user

        @NonNull
        private var title: String? = "" // review title

        init {
            setRid(rid)
            setPostTime(postTime)
            setNoReplies(noReplies)
            setLastReplyTime(lastReplyTime)
            setUserName(userName)
            setUid(uid)
            setTitle(title)
        }

        fun getRid(): Int {
            return rid
        }

        fun setRid(rid: Int) {
            this.rid = rid
        }

        @NonNull
        fun getPostTime(): Date? {
            return postTime
        }

        fun setPostTime(@NonNull postTime: Date?) {
            this.postTime = postTime
        }

        fun getNoReplies(): Int {
            return noReplies
        }

        fun setNoReplies(noReplies: Int) {
            this.noReplies = noReplies
        }

        @NonNull
        fun getLastReplyTime(): Date? {
            return lastReplyTime
        }

        fun setLastReplyTime(@NonNull lastReplyTime: Date?) {
            this.lastReplyTime = lastReplyTime
        }

        fun getUid(): Int {
            return uid
        }

        fun setUid(uid: Int) {
            this.uid = uid
        }

        @NonNull
        fun getTitle(): String? {
            return title
        }

        fun setTitle(@NonNull title: String?) {
            this.title = title
        }

        @NonNull
        fun getUserName(): String? {
            return userName
        }

        fun setUserName(@NonNull userName: String?) {
            this.userName = userName
        }
    }

    private val list: List<Review?>? = ArrayList()
    private var totalPage = 1
    private var currentPage = 0 // 1-totalPage, 0 means not yet loaded
    fun getList(): List<Review?>? {
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