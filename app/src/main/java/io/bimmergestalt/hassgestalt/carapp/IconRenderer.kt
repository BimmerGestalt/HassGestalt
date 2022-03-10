package io.bimmergestalt.hassgestalt.carapp

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import java.io.ByteArrayOutputStream

class IconRenderer(val context: Context) {
	fun render(drawableProvider: Context.() -> Drawable?, width: Int, height: Int): Bitmap {
		val drawable = context.run(drawableProvider)
		return drawable?.let { render(it, width, height) } ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
	}
	fun render(drawable: Drawable, width: Int, height: Int): Bitmap {
		val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		val canvas = Canvas(outBitmap)
		val paint = Paint()
		paint.isFilterBitmap = true
		drawable.setBounds(0, 0, width, height)
		drawable.draw(canvas)
		return outBitmap
	}

	fun compress(bitmap: Bitmap, quality: Int): ByteArray {
		val data = ByteArrayOutputStream()
		if (quality == 100) {
			bitmap.compress(Bitmap.CompressFormat.PNG, quality, data)
		} else {
			bitmap.compress(Bitmap.CompressFormat.JPEG, quality, data)
		}
		return data.toByteArray()
	}
}