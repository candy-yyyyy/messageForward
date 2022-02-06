package cn.c7n6y.message

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        println("收到短信了。。。")
        val intent2 : Intent?
        intent2 = Intent(context, MainActivity::class.java)
        intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context?.startActivity(intent2)
    }
}