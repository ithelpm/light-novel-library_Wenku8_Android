package org.mewx.wenku8.global.apiimport

kotlin.Throws
/**
 * Created by MewX on 2015/6/12.
 * Save error list, interpret each error.
 */
object Wenku8Error {
    fun getSystemDefinedErrorCode(err: Int): org.mewx.wenku8.global.api.Wenku8Error.ErrorCode? {
        /*
        0 请求发生错误
        1 成功(登陆、添加、删除、发帖)
        2 用户名错误
        3 密码错误
        4 请先登陆
        5 已经在书架
        6 书架已满
        7 小说不在书架
        8 回复帖子主题不存在
        9 签到失败
        10 推荐失败
        11 帖子发送失败
        22 refer page 0
        */
        return when (err) {
            0 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_0_REQUEST_ERROR
            1 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED
            2 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_2_ERROR_USERNAME
            3 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_3_ERROR_PASSWORD
            4 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_4_NOT_LOGGED_IN
            5 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_5_ALREADY_IN_BOOKSHELF
            6 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_6_BOOKSHELF_FULL
            7 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF
            8 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_8_TOPIC_NOT_EXIST
            9 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_9_SIGN_FAILED
            10 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_10_RECOMMEND_FAILED
            11 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_11_POST_FAILED
            22 -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.SYSTEM_22_REFER_PAGE_0
            else -> org.mewx.wenku8.global.api.Wenku8Error.ErrorCode.ERROR_DEFAULT
        }
    }

    enum class ErrorCode {
        // default unknown
        ERROR_DEFAULT,  // system defined
        SYSTEM_0_REQUEST_ERROR, SYSTEM_1_SUCCEEDED, SYSTEM_2_ERROR_USERNAME, SYSTEM_3_ERROR_PASSWORD, SYSTEM_4_NOT_LOGGED_IN, SYSTEM_5_ALREADY_IN_BOOKSHELF, SYSTEM_6_BOOKSHELF_FULL, SYSTEM_7_NOVEL_NOT_IN_BOOKSHELF, SYSTEM_8_TOPIC_NOT_EXIST, SYSTEM_9_SIGN_FAILED, SYSTEM_10_RECOMMEND_FAILED, SYSTEM_11_POST_FAILED, SYSTEM_22_REFER_PAGE_0,  // custom
        RETURNED_VALUE_EXCEPTION, BYTE_TO_STRING_EXCEPTION, USER_INFO_EMPTY, NETWORK_ERROR, STORAGE_ERROR, IMAGE_LOADING_ERROR, STRING_CONVERSION_ERROR, XML_PARSE_FAILED, USER_CANCELLED_TASK, PARAM_COUNT_NOT_MATCHED, LOCAL_BOOK_REMOVE_FAILED, SERVER_RETURN_NOTHING
    }
}