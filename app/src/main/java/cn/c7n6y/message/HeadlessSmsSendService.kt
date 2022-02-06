package cn.c7n6y.message

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class HeadlessSmsSendService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return Binder()
    }

    override fun onCreate() {
        super.onCreate()
        println("服务被创建")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("服务被销毁")
    }
}