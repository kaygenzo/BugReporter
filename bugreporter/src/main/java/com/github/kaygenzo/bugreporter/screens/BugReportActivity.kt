package com.github.kaygenzo.bugreporter.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kaygenzo.bugreporter.BugReporter
import com.github.kaygenzo.bugreporter.BugReporterConstants
import com.github.kaygenzo.bugreporter.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_bug_report.*
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.*

internal class BugReportActivity: AppCompatActivity() {

    companion object {

        fun getIntent(
                context: Context,
                imagePath: String,
                width: Int,
                height: Int,
                previewScale: Float,
                fields: List<FieldType>
        ): Intent {
            return Intent(context, BugReportActivity::class.java).apply {
                putExtra(BugReporterConstants.EXTRA_IMAGE_PATH, imagePath)
                putExtra(BugReporterConstants.EXTRA_IMAGE_WIDTH, width)
                putExtra(BugReporterConstants.EXTRA_IMAGE_HEIGHT, height)
                putExtra(BugReporterConstants.EXTRA_PREVIEW_SCALE, previewScale)
                putExtra(BugReporterConstants.EXTRA_FIELDS, fields.map { it.ordinal }.toIntArray())
            }
        }
    }

    private lateinit var fieldItemsAdapter: FieldAdapter
    private val fieldItems = mutableListOf<FieldItem>()
    private var imagePath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug_report)
        fieldItemsAdapter = FieldAdapter(fieldItems)
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val previewScale = intent.getFloatExtra(BugReporterConstants.EXTRA_PREVIEW_SCALE, 1f)
        imagePath = intent.getStringExtra(BugReporterConstants.EXTRA_IMAGE_PATH) ?: ""
        val imageWidth = (intent.getIntExtra(BugReporterConstants.EXTRA_IMAGE_WIDTH, 0) * previewScale).toInt()
        val imageHeight = (intent.getIntExtra(BugReporterConstants.EXTRA_IMAGE_HEIGHT, 0) * previewScale).toInt()

        val now: Long
        val date: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.now().apply {
                now = this.toEpochMilli()
                date = Date.from(this).toString()
            }
        }
        else {
            now = Calendar.getInstance().timeInMillis
            date = Date(now).toString()
        }

        val fieldItems: List<FieldItem> = intent.getIntArrayExtra(BugReporterConstants.EXTRA_FIELDS)
                ?.map { FieldType.values()[it] }
                ?.map {
                    when(it) {
                        FieldType.DATE_TIME -> FieldItem(
                                it,
                                getString(R.string.label_field_date_and_time),
                                date
                        )
                        FieldType.DATE_TIME_MILLIS -> FieldItem(
                                it,
                                getString(R.string.label_field_date_and_time_millis),
                                now.toString()
                        )
                        FieldType.MANUFACTURER -> FieldItem(
                                it,
                                getString(R.string.label_field_manufacturer),
                                Build.MANUFACTURER
                        )
                        FieldType.BRAND -> FieldItem(
                                it,
                                getString(R.string.label_field_brand),
                                Build.BRAND
                        )
                        FieldType.MODEL -> FieldItem(
                                it,
                                getString(R.string.label_field_model),
                                Build.MODEL
                        )
                        FieldType.APP_VERSION -> {
                            val packageInfo = packageManager.getPackageInfo(packageName, 0)
                            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode
                            }
                            val versionName = packageInfo.versionName
                            FieldItem(
                                    it,
                                    getString(R.string.label_field_app_version),
                                    "$versionName ($versionCode)"
                            )
                        }
                        FieldType.ANDROID_VERSION -> FieldItem(
                                it,
                                getString(R.string.label_field_android_version),
                                Build.VERSION.SDK_INT.toString(),
                                true
                        )
                        FieldType.LOCALE -> FieldItem(
                                it,
                                getString(R.string.label_field_locale),
                                Locale.getDefault().toString(),
                                true
                        )
                        FieldType.BT_STATUS -> {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                                val status = BluetoothAdapter.getDefaultAdapter().isEnabled.toString()
                                FieldItem(it, getString(R.string.label_field_bluetooth_status), status)
                            } else {
                                FieldItem(it, getString(R.string.label_field_bluetooth_status), getString(
                                        R.string.text_field_missing_permission
                                ), visible = true)
                            }
                        }
                        FieldType.WIFI_STATUS -> {
                            if (ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_WIFI_STATE
                                    ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val status =
                                        (applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager)?.isWifiEnabled?.toString()
                                                ?: "false"
                                FieldItem(it, getString(R.string.label_field_wifi_status), status)
                            } else {
                                FieldItem(
                                        it,
                                        getString(R.string.label_field_wifi_status),
                                        getString(R.string.text_field_missing_permission),
                                        visible = true
                                )
                            }
                        }
                        FieldType.NETWORK_STATUS -> {
                            if (ContextCompat.checkSelfPermission(
                                            this,
                                            Manifest.permission.ACCESS_NETWORK_STATE
                                    ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val status =
                                        (applicationContext.getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnected?.toString()
                                                ?: "false"
                                FieldItem(it, getString(R.string.label_field_network_status), status)
                            } else {
                                FieldItem(
                                        it, getString(R.string.label_field_network_status), getString(
                                        R.string.text_field_missing_permission
                                ), visible = true
                                )
                            }
                        }
                        FieldType.SCREEN_DENSITY -> {

                            val density = resources.displayMetrics.density
                            val densityText = if (density == 0.75f) {
                                "ldpi"
                            } else if (density >= 1.0f && density < 1.5f) {
                                "mdpi"
                            } else if (density == 1.5f) {
                                "hdpi"
                            } else if (density > 1.5f && density <= 2.0f) {
                                "xhdpi"
                            } else if (density > 2.0f && density <= 3.0f) {
                                "xxhdpi"
                            } else {
                                "xxxhdpi"
                            }

                            FieldItem(
                                    it,
                                    getString(R.string.label_field_screen_density),
                                    densityText
                            )
                        }
                        FieldType.SCREEN_RESOLUTION -> {

                            val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val rect = windowManager.currentWindowMetrics.bounds
                                "${rect.right} x ${rect.bottom} px"
                            } else {
                                val metrics = DisplayMetrics()
                                windowManager.defaultDisplay.getRealMetrics(metrics)
                                "${metrics.widthPixels} x ${metrics.heightPixels} px"
                            }

                            FieldItem(
                                    it,
                                    getString(R.string.label_field_screen_resolution),
                                    text
                            )
                        }
                        FieldType.ORIENTATION -> FieldItem(
                                it,
                                getString(R.string.label_field_orientation),
                                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                                    getString(R.string.bug_reporting_portrait)
                                else
                                    getString(R.string.bug_reporting_landscape)
                        )
                        FieldType.BATTERY_STATUS -> {
                            val batteryStatus: Intent? =
                                    IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                                        this.registerReceiver(null, ifilter)
                                    }
                            val status: Int = batteryStatus?.getIntExtra(
                                    BatteryManager.EXTRA_STATUS,
                                    -1
                            ) ?: -1
                            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                                    || status == BatteryManager.BATTERY_STATUS_FULL

                            val batteryPct: Float? = batteryStatus?.let { intent ->
                                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                                level * 100 / scale.toFloat()
                            }
                            FieldItem(
                                    it,
                                    getString(R.string.label_field_battery_status),
                                    "$batteryPct% " + (if (isCharging) "- " + getString(R.string.bug_reporting_battery_charging) else "")
                            )
                        }
                    }
                } ?: listOf()

        this.fieldItems.apply {
            clear()
            addAll(fieldItems)
        }

        imagePath?.let { path ->
            Picasso.get()
                    .invalidate(File(path))

            Picasso.get()
                    .load(File(path))
                    .resize(imageWidth, imageHeight)
                    .into(bugReporterScreenshotPreview)

            bugReporterScreenshotPreview.setOnClickListener {
                startActivity(PaintActivity.getIntent(this, path))
            }
        }

        bugReporterOptionsRecycler.apply {
            addItemDecoration(DividerItemDecoration(this@BugReportActivity, RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(this@BugReportActivity)
            adapter = fieldItemsAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_report, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = createReport()
        BugReporter.resultSubject.onNext(intent)
        setResult(Activity.RESULT_OK, intent)
        finish()
        return true
    }

    private fun createReport(): Intent {

        val resultObject = JSONObject().apply {
            put(BugReporterConstants.KEY_DESCRIPTION, bugReporterDescription.text)
        }

        fieldItems.filter { it.enabled }.forEach {
            resultObject.put(it.type.name.toLowerCase(), it.text)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(BugReporter.developerEmailAddress))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_email_object))
            putExtra(Intent.EXTRA_TEXT, resultObject.toString().trim())
            val authority = "${packageName}${BugReporterConstants.FILE_AUTHORITY_SUFFIX}"
            val imageUri = FileProvider.getUriForFile(this@BugReportActivity, authority, File(imagePath))
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}