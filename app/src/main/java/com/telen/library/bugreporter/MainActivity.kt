package com.telen.library.bugreporter

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kaygenzo.bugreporter.api.ReportMethod
import com.telen.library.bugreporter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_PERMISSION = 1
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)

            disable.setOnClickListener {
                (application as DemoApplication).reporter.disable()
            }

            restartManual.setOnClickListener {
                (application as DemoApplication).apply {
                    reporter.setReportMethods(listOf())
                    reporter.restart()
                }
            }

            restartShake.setOnClickListener {
                (application as DemoApplication).apply {
                    reporter.setReportMethods(listOf(ReportMethod.SHAKE))
                    reporter.restart()
                }
            }

            restartFloatingButton.setOnClickListener {
                (application as DemoApplication).apply {
                    reporter.setReportMethods(listOf(ReportMethod.FLOATING_BUTTON))
                    reporter.restart()
                }
            }

            reportBug.setOnClickListener {
                (application as DemoApplication).reporter.startReport(this@MainActivity)
            }

            release.setOnClickListener {
                (application as DemoApplication).reporter.release()
            }

            askPermission.setOnClickListener {
                (application as DemoApplication).reporter.askOverlayPermission(
                    this@MainActivity,
                    REQUEST_CODE_PERMISSION
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if ((application as DemoApplication).reporter.hasPermissionOverlay(this)) {
                    Toast.makeText(this, "Permission success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}