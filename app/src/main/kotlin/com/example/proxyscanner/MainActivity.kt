package com.example.proxyscanner

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var etHost: EditText
    private lateinit var btnCheck: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etHost = findViewById(R.id.etHost)
        btnCheck = findViewById(R.id.btnCheck)
        progressBar = findViewById(R.id.progressBar)
        tvResult = findViewById(R.id.tvResult)

        btnCheck.setOnClickListener {
            val inputHost = etHost.text.toString().trim()
            if (inputHost.isNotEmpty()) {
                checkHost(inputHost)
            } else {
                tvResult.text = "กรุณากรอก Host หรือ IP ก่อนครับ"
            }
        }
    }

    private fun checkHost(targetHost: String) {
        // แสดง ProgressBar และปิดปุ่มชั่วคราวระหว่างรอผล
        progressBar.visibility = View.VISIBLE
        btnCheck.isEnabled = false
        tvResult.text = "กำลังตรวจสอบ $targetHost ..."

        // แยกไปทำงานบน Background Thread ป้องกันแอปค้าง
        thread {
            val startTime = System.currentTimeMillis()
            var responseCode = -1
            var responseMessage = ""

            try {
                val formattedUrl = if (!targetHost.startsWith("http://") && !targetHost.startsWith("https://")) {
                    "http://$targetHost"
                } else {
                    targetHost
                }

                val url = URL(formattedUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000 // หมดเวลาใน 5 วินาที
                connection.readTimeout = 5000
                connection.requestMethod = "HEAD" // ใช้ HEAD เพื่อส่งข้อมูลน้อยและตอบกลับเร็ว
                connection.instanceFollowRedirects = false

                responseCode = connection.responseCode
                responseMessage = connection.responseMessage
                connection.disconnect()
            } catch (e: Exception) {
                responseMessage = e.localizedMessage ?: "Connection Failed"
            }

            val elapsedTime = System.currentTimeMillis() - startTime

            // ส่งผลลัพธ์กลับมาแสดงผลที่ UI Main Thread
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnCheck.isEnabled = true

                if (responseCode != -1) {
                    tvResult.text = """
                         STATUS: $responseCode ($responseMessage)
                         TIME: ${elapsedTime} ms
                         HOST: $targetHost
                    """.trimIndent()
                } else {
                    tvResult.text = """
                         ERROR: ไม่สามารถเชื่อมต่อได้
                         DETAIL: $responseMessage
                         TIME: ${elapsedTime} ms
                    """.trimIndent()
                }
            }
        }
    }
}
