package org.mewx.wenku8.service

import android.app.Service

/**
 * Created by MewX on 2015/5/17.
 * Heartbeat Session Keeper. Useless.
 */
class HeartbeatSessionKeeper : Service() {
    private var interval = 60 * 10 * 1000

    /**
     * Set interval, must cancel it, then set & start.
     * @param i time count
     */
    fun setInterval(i: Int) {
        if (i > 0) interval = i
    }

    @Override
    fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @Override
    fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            @Override
            fun run() {
                // frequently request
                val err: Wenku8Error.ErrorCode = LightUserSession.heartbeatLogin()
                Toast.makeText(MyApp.getContext(), err.toString(), Toast.LENGTH_SHORT).show()
            }
        }, 0, interval)
        return super.onStartCommand(intent, flags, startId)
    }
}