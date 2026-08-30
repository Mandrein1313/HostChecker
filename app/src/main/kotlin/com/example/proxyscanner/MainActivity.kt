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
            val input = etHost.text.toString().trim()
            if (input.isNotEmpty()) {
                val hosts = input.split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                tvResult.text = ""
                checkHosts(hosts)
            } else {
                tvResult.text = "กรุณากรอก Host หรือ IP ก่อนครับ"
            }
        }
    }

    /** ตรวจสอบหลาย Host พร้อมกัน */
    private fun checkHosts(hosts: List<String>) {
        val latch = java.util.concurrent.CountDownLatch(hosts.size)

        progressBar.visibility = View.VISIBLE
        btnCheck.isEnabled = false

        for (host in hosts) {
            thread {
                val startTime = System.currentTimeMillis()
                var responseCode = -1
                var responseMessage = ""

                try {
                    val formattedUrl = if (!host.startsWith("http://") && !host.startsWith("https://")) {
                        "http://$host"
                    } else host

                    val url = URL(formattedUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.requestMethod = "HEAD"
                    connection.instanceFollowRedirects = false

                    responseCode = connection.responseCode
                    responseMessage = connection.responseMessage
                    connection.disconnect()
                } catch (e: Exception) {
                    responseMessage = e.localizedMessage ?: "Connection Failed"
                }

                val elapsedTime = System.currentTimeMillis() - startTime

                runOnUiThread {
                    val result = if (responseCode != -1) {
                        "HOST: $host\nSTATUS: $responseCode ($responseMessage)\nTIME: ${elapsedTime} ms\n\n"
                    } else {
                        "HOST: $host\nERROR: ไม่สามารถเชื่อมต่อได้\nDETAIL: $responseMessage\nTIME: ${elapsedTime} ms\n\n"
                    }
                    tvResult.append(result)
                }

                latch.countDown()
            }
        }

        // เมื่อทุก thread เสร็จแล้ว ปิด ProgressBar
        thread {
            latch.await()
            runOnUiThread {
                progressBar.visibility = View.GONE
                btnCheck.isEnabled = true
            }
        }
    }
}