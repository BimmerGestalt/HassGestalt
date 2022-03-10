package io.bimmergestalt.hassgestalt.phoneui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import io.bimmergestalt.hassgestalt.phoneui.extensions.visible

object extensions {
	/** Toggle a View's visibility by a boolean */
	var View.visible: Boolean
		get() {
			return this.visibility == View.VISIBLE
		}
		set(value) {
			this.visibility = if (value) View.VISIBLE else View.GONE
		}
}

@BindingAdapter("android:visibility")
fun setViewVisibility(view: View, visible: Boolean) {
	view.visible = visible
}

@BindingAdapter("android:src")
fun setImageViewResource(view: ImageView, drawableProvider: (Context.() -> Drawable?)?) {
	val drawable = drawableProvider?.let {
		view.context.run(it)
	}

	view.setImageDrawable(drawable)
}