package com.example.overlayservices

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.animation.AnimationUtils
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        askForSystemOverlayPermission()

        clickMe.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverLayService::class.java))
            } else {
                errorToast()
            }
        }

    }

    override fun onPause() {
        super.onPause()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverLayService::class.java).putExtra("activity_background", true))
            finish()
        } else {
            errorToast()
        }
    }

    private fun askForSystemOverlayPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            val intent = Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION")
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, 123)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 123) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    errorToast()
                    finish()
                }
            }
        }
    }

    /* override fun onDestroy() {
         super.onDestroy()
         stopService(Intent(this,OverLayService::class.java))
     }*/

    private fun errorToast() {
        Toast.makeText(
            this,
            "Draw over other app permission not available. Can't start the application without the permission.",
            Toast.LENGTH_LONG
        ).show()
    }
}
