package com.telen.library.bugreporter

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import com.tarek360.instacapture.Instacapture
import com.tarek360.instacapture.listener.SimpleScreenCapturingListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object BugReporter {

    private const val SCREENSHOT_DIR = "screenshots"
    private const val DEFAULT_JPEG_COMPRESSION_QUALITY = 75
    private const val DEFAULT_PREVIEW_SCALE = 0.3f

    private val reportFields = mutableListOf<FieldType>()
    private var compressionQuality = DEFAULT_JPEG_COMPRESSION_QUALITY
    private var previewScale = DEFAULT_PREVIEW_SCALE

    class Builder {

        fun setFields(fields: List<FieldType>): Builder {
            reportFields.apply {
                clear()
                addAll(fields)
            }
            return this
        }

        fun setCompressionQuality(compressionQuality: Int): Builder  {
            BugReporter.compressionQuality = compressionQuality
            return this
        }

        fun setImagePreviewScale(scale: Float): Builder  {
            previewScale = scale
            return this
        }

        fun build(): BugReporter {
            return BugReporter
        }
    }

    fun startReport(activity: AppCompatActivity) {
        Instacapture.capture(activity, object : SimpleScreenCapturingListener() {
            override fun onCaptureComplete(bitmap: Bitmap) {
                val width = bitmap.width
                val height = bitmap.height
               getScreenshotFile(context = activity, bitmap = bitmap)
                   .subscribeOn(Schedulers.io())
                   .observeOn(AndroidSchedulers.mainThread())
                   .subscribe({
                       activity.startActivity(BugReportActivity.getIntent(activity, it.absolutePath, width, height, previewScale, reportFields))
                   }, {
                       //TODO
                       it.printStackTrace()
                   })
            }
        })
    }

    private fun getScreenshotFile(context: Context): File {
        //TODO use instant
        val dateFormat: DateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss.SS", Locale.getDefault())
        val fileName = "screenshot-" + dateFormat.format(Date()) + ".jpg"
        val screenshotsDir = File(context.cacheDir, SCREENSHOT_DIR)
        screenshotsDir.mkdirs()
        return File(screenshotsDir, fileName)
    }

    private fun getScreenshotFile(context: Context, bitmap: Bitmap): Single<File> {
        return Single.create {  emitter ->
            var outputStream: BufferedOutputStream? = null
            try {
                val screenshotFile: File = getScreenshotFile(context)
                outputStream = BufferedOutputStream(FileOutputStream(screenshotFile))
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
                outputStream.flush()
                emitter.onSuccess(screenshotFile)
            }
            catch (e: IOException) {
                emitter.onError(e)
            }
            finally {
                try {
                    outputStream?.close()
                } catch (exception: IOException) {
                    //TODO
                    exception.printStackTrace()
                }
            }
        }
    }
}