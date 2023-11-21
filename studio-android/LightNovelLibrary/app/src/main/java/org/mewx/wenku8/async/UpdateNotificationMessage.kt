package org.mewx.wenku8.async

import android.os.AsyncTask

/**
 * This async task is used for checking new notification texts.
 */
class UpdateNotificationMessage : AsyncTask<Void?, Void?, String?>() {
    @Override
    protected fun doInBackground(vararg voids: Void?): String? {
        val codeByte: ByteArray = LightNetwork.LightHttpDownload(
                if (GlobalConfig.getCurrentLang() !== Wenku8API.LANG.SC) GlobalConfig.noticeCheckTc else GlobalConfig.noticeCheckSc
        )
        if (codeByte == null) {
            Log.e(UpdateNotificationMessage::class.java.getSimpleName(), "unable to get notification text")
            return null
        }
        val notice = String(codeByte)
        return notice.trim()
    }

    @Override
    protected fun onPostExecute(notice: String?) {
        super.onPostExecute(notice)
        if (notice == null || notice.isEmpty()) {
            Log.e(UpdateNotificationMessage::class.java.getSimpleName(), "received empty notification text")
            return
        }
        Log.i("MewX", "received notification text: $notice")

        // update the latest string
        Wenku8API.NoticeString = notice
        // save to local file
        GlobalConfig.writeTheNotice(notice)
    }
}