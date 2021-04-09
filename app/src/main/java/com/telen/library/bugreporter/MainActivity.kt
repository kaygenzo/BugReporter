package com.telen.library.bugreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reportBug.setOnClickListener {
            BugReporter.startReport(this)
        }
        askPermission.setOnClickListener {
            BugReporter.askOverlayPermission(this, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(PermissionsUtils.hasPermissionOverlay(this)) {
            Toast.makeText(this, "Permission success", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Permission failed", Toast.LENGTH_SHORT).show()
        }
    }
}