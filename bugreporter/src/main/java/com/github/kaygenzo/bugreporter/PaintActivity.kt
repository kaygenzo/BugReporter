package com.github.kaygenzo.bugreporter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_paint_screen.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class PaintActivity: AppCompatActivity() {

    companion object {
        fun getIntent(context: Context, imagePath: String): Intent {
            return Intent(context, PaintActivity::class.java).apply {
                putExtra(BugReporterConstants.EXTRA_IMAGE_PATH, imagePath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paint_screen)

        val imagePath = intent.getStringExtra(BugReporterConstants.EXTRA_IMAGE_PATH)?.also { path ->
            Picasso.get().load(File(path)).into(paintCanvas)
        }

        val blackColor = ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_black)
        paintCanvas.apply {
            normal()
            strokeWidth = 20f
            strokeColor = blackColor
        }
        paintPaletteColor.setColorFilter(blackColor)

        paintActionUndo.setOnClickListener {
            paintCanvas.undo()
        }
        paintActionClear.setOnClickListener {
            paintCanvas.clear()
        }
        paintActionCheck.setOnClickListener {
            imagePath?.let { path ->
                paintCanvas.drawable?.toBitmap()?.let { bitmapImage ->
                    var outputStream: BufferedOutputStream? = null
                    try {
                        outputStream = BufferedOutputStream(FileOutputStream(File(path)))
                        bitmapImage.compress(Bitmap.CompressFormat.JPEG,100, outputStream)
                        outputStream.flush()
                        finish()
                    }
                    catch (e: IOException) {
                        //TODO
                        e.printStackTrace()
                        finish()
                    }
                    finally {
                        try {
                            outputStream?.close()
                        } catch (exception: IOException) {
                            //TODO
                            exception.printStackTrace()
                        }
                    }
                } ?: let {
                    //TODO
                    finish()
                }
            } ?: let {
                //TODO
                finish()
            }
        }

        paintPaletteRed.setOnClickListener {
            val color = ContextCompat.getColor(this, R.color.canvas_palette_red)
            paintPaletteColor.setColorFilter(color)
            paintCanvas.strokeColor = color
        }
        paintPaletteGreen.setOnClickListener {
            val color = ContextCompat.getColor(this, R.color.canvas_palette_green)
            paintPaletteColor.setColorFilter(color)
            paintCanvas.strokeColor = color
        }
        paintPaletteBlue.setOnClickListener {
            val color = ContextCompat.getColor(this, R.color.canvas_palette_blue)
            paintPaletteColor.setColorFilter(color)
            paintCanvas.strokeColor = color
        }
        paintPaletteYellow.setOnClickListener {
            val color = ContextCompat.getColor(this, R.color.canvas_palette_yellow)
            paintPaletteColor.setColorFilter(color)
            paintCanvas.strokeColor = color
        }
        paintPaletteBlack.setOnClickListener {
            paintPaletteColor.setColorFilter(blackColor)
            paintCanvas.strokeColor = blackColor
        }
        paintPaletteWhite.setOnClickListener {
            val color = ContextCompat.getColor(this, R.color.canvas_palette_white)
            paintPaletteColor.setColorFilter(color)
            paintCanvas.strokeColor = color
        }
    }
}