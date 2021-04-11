package com.telen.library.bugreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.ReportResult
import com.github.kaygenzo.bugreporter.utils.PermissionsUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_BUG_REPORT = 0
        const val REQUEST_CODE_PERMISSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        reportBugEmail.setOnClickListener {
            BugReporter.setReportResult(ReportResult.EMAIL, REQUEST_CODE_BUG_REPORT)
            BugReporter.startReport(this)
        }
        reportBugCustom.setOnClickListener {
            BugReporter.setReportResult(ReportResult.CUSTOM, REQUEST_CODE_BUG_REPORT)
            BugReporter.startReport(this)
        }
        askPermission.setOnClickListener {
            BugReporter.askOverlayPermission(this, REQUEST_CODE_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REQUEST_CODE_BUG_REPORT -> {
                Toast.makeText(this, "Bug report result: ${data?.getStringExtra(Intent.EXTRA_TEXT)}", Toast.LENGTH_SHORT).show()
            }
            REQUEST_CODE_PERMISSION -> {
                if(PermissionsUtils.hasPermissionOverlay(this)) {
                    Toast.makeText(this, "Permission success", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, "Permission failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}