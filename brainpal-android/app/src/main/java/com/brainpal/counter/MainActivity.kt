package com.brainpal.counter

import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)
        findViewById<Button>(R.id.enable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.reset).setOnClickListener { store.resetToday(); render() }
        findViewById<SwitchCompat>(R.id.notchSwitch).apply {
            isChecked = store.notchEnabled
            setOnCheckedChangeListener { _, on -> store.notchEnabled = on }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val ig = store.get(Source.INSTAGRAM)
        val yt = store.get(Source.YOUTUBE)
        findViewById<TextView>(R.id.igCount).text = "$ig Reels"
        findViewById<TextView>(R.id.ytCount).text = "$yt Shorts"
        findViewById<TextView>(R.id.total).text = "${ig + yt} total today"
        findViewById<TextView>(R.id.status).text =
            if (ReelCounterService.instance != null) "● Counting is on" else "○ Counting is off — enable the service"
    }
}
