package com.github.kaygenzo.bugreporter.internal.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.github.kaygenzo.bugreporter.R
import com.github.kaygenzo.bugreporter.databinding.ActivityPaintScreenBinding
import com.github.kaygenzo.bugreporter.internal.InternalConstants
import com.squareup.picasso.Picasso
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class PaintActivity : BugReportActivity() {

    companion object {
        fun getIntent(context: Context, imagePath: String): Intent {
            return Intent(context, PaintActivity::class.java).apply {
                putExtra(InternalConstants.EXTRA_IMAGE_PATH, imagePath)
            }
        }
    }

    private lateinit var binding: ActivityPaintScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaintScreenBinding.inflate(layoutInflater).apply {
            setContentView(root)
            val imagePath =
                intent.getStringExtra(InternalConstants.EXTRA_IMAGE_PATH)?.also { path ->
                    Picasso.get().load(File(path)).into(paintCanvas)
                }

            val blackColor =
                ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_black)
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
                        try {
                            BufferedOutputStream(FileOutputStream(File(path))).use {
                                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, it)
                                it.flush()
                                finish()
                            }
                        } catch (e: IOException) {
                            Timber.e(e)
                            finish()
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
                val color = ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_red)
                paintPaletteColor.setColorFilter(color)
                paintCanvas.strokeColor = color
            }
            paintPaletteGreen.setOnClickListener {
                val color = ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_green)
                paintPaletteColor.setColorFilter(color)
                paintCanvas.strokeColor = color
            }
            paintPaletteBlue.setOnClickListener {
                val color = ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_blue)
                paintPaletteColor.setColorFilter(color)
                paintCanvas.strokeColor = color
            }
            paintPaletteYellow.setOnClickListener {
                val color =
                    ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_yellow)
                paintPaletteColor.setColorFilter(color)
                paintCanvas.strokeColor = color
            }
            paintPaletteBlack.setOnClickListener {
                paintPaletteColor.setColorFilter(blackColor)
                paintCanvas.strokeColor = blackColor
            }
            paintPaletteWhite.setOnClickListener {
                val color = ContextCompat.getColor(this@PaintActivity, R.color.canvas_palette_white)
                paintPaletteColor.setColorFilter(color)
                paintCanvas.strokeColor = color
            }
        }
    }
}