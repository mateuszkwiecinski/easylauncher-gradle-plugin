package com.example.custom.adaptive

import android.app.AlertDialog
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val iconName
        get() = intent?.getIntExtra("iconName", R.mipmap.ic_launcher) ?: R.mipmap.ic_launcher

    override fun onCreate(savedInstanceState: Bundle?) {
        StrictMode.enableDefaults()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<View>(R.id.button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hello, Android!")
                .setMessage("This is an example app with easylaucher icon applied")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        val iconView = findViewById<LauncherIconView>(R.id.launcher_icon)
        iconView.setIcon(iconName)

        val iconView2 = findViewById<ImageView>(R.id.launcher_icon_2)
        iconView2.setImageResource(iconName)
    }
}
