package io.bimmergestalt.hassgestalt.phoneui

import android.view.View
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