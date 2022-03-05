package io.bimmergestalt.hassgestalt.carapp

import android.content.Context

class AndroidResources(val context: Context) {
    fun getRaw(id: Int): ByteArray {
        return context.resources.openRawResource(id).readBytes()
    }
}