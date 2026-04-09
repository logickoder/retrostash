package dev.logickoder.retrostash

import android.content.SharedPreferences

/**
 * Internal extension to avoid forcing a dependency on androidx.core-ktx
 */
internal inline fun SharedPreferences.edit(action: SharedPreferences.Editor.() -> Unit) {
    val editor = edit()
    action(editor)
    editor.apply()
}