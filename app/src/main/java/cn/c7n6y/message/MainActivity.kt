package cn.c7n6y.message

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.*
import javax.mail.Address
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class MainActivity : AppCompatActivity() {

    var tv: TextView? = null
    var button: Button? = null
    var addressEditText: EditText? = null

    var addressText: String? = null

    var sendFlag = false

    // 只取收件箱
    var SMS_INBOX: Uri = Uri.parse("content://sms/inbox")

    private var smsObserver: SmsObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        println("打开页面了")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        setView()
        // 权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            var hasReadSmsPermission = checkSelfPermission(Manifest.permission.READ_SMS)
            if (hasReadSmsPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_SMS), 123)
            }
        }
        smsObserver = SmsObserver(this, smsHandler)
        contentResolver.registerContentObserver(
            SMS_INBOX, true,
            smsObserver!!
        )
    }

    var smsHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            println("smsHandler 执行了...")
        }
    }

    @SuppressLint("Range")
    fun getSmsFromPhone() {

        // 发送标识为false则不发送
        if (!sendFlag) {
            return
        }
        /**
         * _id：短信序号，如100
        　　
        　　thread_id：对话的序号，如100，与同一个手机号互发的短信，其序号是相同的
        　　
        　　address：发件人地址，即手机号，如+86138138000
        　　
        　　person：发件人，如果发件人在通讯录中则为具体姓名，陌生人为null
        　　
        　　date：日期，long型，如1346988516，可以对日期显示格式进行设置
        　　
        　　protocol：协议0SMS_RPOTO短信，1MMS_PROTO彩信
        　　
        　　read：是否阅读0未读，1已读
        　　
        　　status：短信状态-1接收，0complete,64pending,128failed
        　　
        　　type：短信类型1是接收到的，2是已发出
        　　
        　　body：短信具体内容
        　　
        　　service_center：短信服务中心号码编号，如+8613800755500
         */
        var cr: ContentResolver = contentResolver
        var projection = arrayOf("body", "address", "_id")
        // 条件为10分钟以内
        var where = " date > " + (System.currentTimeMillis() - 10 * 60 * 1000)
        var cur: Cursor? = cr.query(SMS_INBOX, projection, where, null, "date desc")
        if (cur != null) {
            while (cur.moveToNext()) {
                var number: String = cur.getString(cur.getColumnIndex("address")) //手机号
                var body: String = cur.getString(cur.getColumnIndex("body"))
                var msgId: String = cur.getString(cur.getColumnIndex("_id"))
                // 判断是否已发送
                if (getData(this@MainActivity, "msgId$msgId") == "") {
                    println("$msgId,$number,$body")
                    // 未发送的短信则记录至app缓存中
                    saveData(this@MainActivity, "msgId$msgId", msgId)
                    // 发送邮件
                    sendEmail("$msgId,$number,$body")
                }
            }
        }
    }

    inner class SmsObserver(context: Context?, handler: Handler?) : ContentObserver(handler) {
        // 监听收到短信事件
        override fun onChange(selfChange: Boolean) {
            println("收到短信")
            super.onChange(selfChange)
            this@MainActivity.getSmsFromPhone()
        }
    }

    //存储key对应的数据
    fun saveData(context: Activity, key: String, info: String) {
        val sharedPreferences = context.getSharedPreferences(key, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, info)
        editor.apply()
    }

    //取key对应的数据
    fun getData(context: Activity, key: String): String {
        val result = context.getSharedPreferences(key, MODE_PRIVATE).getString(key, "")
        return if (result!!.isEmpty()) {
            ""
        } else {
            result
        }
    }

    // 发送短信
    fun sendEmail(message: String) {
        var props = Properties()
        // 邮件服务器 这里用的QQ
        props.put("mail.smtp.host", "smtp.qq.com")
        var session: Session = Session.getInstance(props, null)
        try {
            var msg = MimeMessage(session)
            // 收件人地址
//            var addressArr = arrayOf("545496535@qq.com")
            var addressArr = addressText!!.split(",")
            var tos = arrayOfNulls<Address>(addressArr.size)
            for (i in addressArr.indices) {
                tos[i] = InternetAddress(addressArr[i])
            }
            // 发件人
            msg.setFrom("545496535@qq.com")
            // 收件人
            msg.setRecipients(javax.mail.Message.RecipientType.TO, tos)
            // 主题
            msg.subject = "短信转发"
            // 发送时间
            msg.sentDate = Date()
            // 发送内容
            msg.setText(message)
            Thread {
                // 发送 需要申请授权码

                try {
                    // 这里需要自己申请邮箱授权码
                    Transport.send(msg, "545496535@qq.com", "")
                } catch (mex: MessagingException) {
                    println("发送短信异常：$mex")
                }
            }.start()

        } catch (mex: MessagingException) {
            println("send failed, exception: $mex")
        }

    }

    // 初始化界面
    fun initView() {
        tv = findViewById(R.id.tv)
        button = findViewById(R.id.btn)
        addressEditText = findViewById(R.id.addressText)
//        addressEditText?.inputType = EditorInfo.TYPE_NULL
//        addressEditText?.isCursorVisible = true
    }

    // 设置界面样式参数
    fun setView() {
        var imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
        // 输入框点击事件
        /*addressEditText!!.setOnClickListener{
            // 打开软键盘
            if (!sendFlag) {
                imm.showSoftInput(this@MainActivity.currentFocus, InputMethodManager.SHOW_FORCED)
            }
        }*/
        // 为了展示光标 参考 https://www.jianshu.com/p/a7b3aa7897ce
        addressEditText?.setOnTouchListener(OnTouchListener { v, event ->
            val inType: Int = addressEditText!!.inputType
            addressEditText?.inputType = InputType.TYPE_NULL
            addressEditText?.onTouchEvent(event)
            addressEditText?.inputType = inType
            // 打开软键盘
            var softShowingFlag = isSoftShowing(this@MainActivity)
            println("软键盘状态：$softShowingFlag")
            if (!sendFlag && !softShowingFlag) {
                imm.showSoftInput(this@MainActivity.currentFocus, InputMethodManager.SHOW_FORCED)
            }
            val text: CharSequence = addressEditText!!.text
            if (text is Spannable) {
                val spanText = text as Spannable
                Selection.setSelection(spanText, text.length)
            }
            true
        })
        button!!.setOnClickListener {
            addressText = addressEditText?.text.toString()
            if (addressText!!.isEmpty()) {
                val toast = Toast.makeText(this@MainActivity, "收件地址不能为空", Toast.LENGTH_SHORT)
                toast.show()
                return@setOnClickListener
            }
            if (addressText!!.length > 1000) {
                val toast = Toast.makeText(this@MainActivity, "输入内容过长", Toast.LENGTH_SHORT)
                toast.show()
                return@setOnClickListener
            }
            addressText!!.replace("，", ",")
            if (addressText!!.split(",").size > 10) {
                val toast = Toast.makeText(this@MainActivity, "收件地址不能超过10个", Toast.LENGTH_SHORT)
                toast.show()
                return@setOnClickListener
            }
            if (sendFlag) {
                // 当前状态为开启时则将按钮设置为关闭状态
                closeBtnView()
                // 打开光标
                addressEditText?.isCursorVisible = true
//                addressEditText?.inputType = EditorInfo.TYPE_CLASS_TEXT
            } else {
                // 当前状态为关闭时则将按钮设置为开启状态
                openBtnView()
                // 关闭光标
                addressEditText?.isCursorVisible = false
//                addressEditText?.inputType = EditorInfo.TYPE_NULL
            }
            sendFlag = !sendFlag
            imm.hideSoftInputFromWindow(this@MainActivity.currentFocus?.windowToken, 0)
        }
    }

    // 开启按钮样式
    fun openBtnView() {
        button!!.setText("停止转发")
        button!!.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.openBtnBg))
        button!!.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
    }

    // 关闭按钮样式
    fun closeBtnView() {
        button!!.setText("开启转发")
        button!!.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        button!!.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
    }

    // 判断是否打开了软键盘
    fun isSoftShowing(activity: Activity): Boolean {
        //获取当屏幕内容的高度
        var screenHeight = activity.window.decorView.height
        //获取View可见区域的bottom
        var rect = Rect()
        //DecorView即为activity的顶级view
        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
        //考虑到虚拟导航栏的情况（虚拟导航栏情况下：screenHeight = rect.bottom + 虚拟导航栏高度）
        //选取screenHeight*3/4进行判断
        return screenHeight * 3 / 4 > rect.bottom + getSoftButtonsBarHeight(activity)
    }

    private fun getSoftButtonsBarHeight(activity: Activity): Int {
        var metrics = DisplayMetrics();
        //这个方法获取可能不是真实屏幕的高度
        activity.getWindowManager().defaultDisplay.getMetrics(metrics);
        var usableHeight = metrics.heightPixels
        //获取当前屏幕的真实高度
        activity.getWindowManager().defaultDisplay.getRealMetrics(metrics);
        var realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }
}

