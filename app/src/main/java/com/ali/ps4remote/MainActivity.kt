package com.ali.ps4remote

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import kotlin.math.abs

class MainActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private val socket by lazy { DatagramSocket() }
    private lateinit var ipBox: EditText
    private lateinit var portBox: EditText

    private fun send(msg: String) {
        val ip = ipBox.text.toString().trim()
        val port = portBox.text.toString().toIntOrNull() ?: 9090
        io.execute {
            try {
                val d = msg.toByteArray()
                socket.send(DatagramPacket(d, d.size, InetAddress.getByName(ip), port))
            } catch (_: Exception) {}
        }
    }

    private fun btn(label: String, cmd: String) = Button(this).apply {
        text = label
        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        setOnClickListener { send("B $cmd") }
    }

    private fun row(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        v.forEach { addView(it) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        ipBox = EditText(this).apply {
            setText("192.168.1.17")
            hint = "PS4 IP"
            inputType = InputType.TYPE_CLASS_PHONE
        }
        root.addView(ipBox)

        portBox = EditText(this).apply {
            setText("9090")
            hint = "Port"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        root.addView(portBox)

        val pad = View(this).apply {
            setBackgroundColor(Color.parseColor("#33000000"))
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        var lx = 0f; var ly = 0f; var moved = 0f; var t0 = 0L
        pad.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lx = e.x; ly = e.y; moved = 0f; t0 = e.eventTime }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.x - lx).toInt(); val dy = (e.y - ly).toInt()
                    if (dx != 0 || dy != 0) {
                        lx = e.x; ly = e.y
                        moved += abs(dx) + abs(dy)
                        send("M $dx $dy")
                    }
                }
                MotionEvent.ACTION_UP ->
                    if (moved < 15 && e.eventTime - t0 < 250) send("C")
            }
            true
        }
        root.addView(pad)

        root.addView(row(btn("↑", "UP"), btn("↓", "DOWN"), btn("←", "LEFT"), btn("→", "RIGHT")))
        root.addView(row(btn("✕ OK", "CROSS"), btn("○ Back", "CIRCLE"),
            btn("Options", "OPTIONS"), btn("PS", "PS")))

        val kb = EditText(this).apply {
            hint = "Keyboard"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_SEND
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun afterTextChanged(s: Editable?) {}
                override fun onTextChanged(s: CharSequence, st: Int, before: Int, count: Int) {
                    repeat(maxOf(0, before - count)) { send("K BS") }
                    if (count > before) send("K " + s.substring(st, st + count))
                }
            })
            setOnEditorActionListener { _, _, _ -> send("K ENTER"); true }
        }
        root.addView(kb)
        setContentView(root)
    }
}
