package android.graphics

object Color {
	@JvmStatic
	fun parseColor(input: String): Int {
		return input.substring(1).toInt(16)
	}
}