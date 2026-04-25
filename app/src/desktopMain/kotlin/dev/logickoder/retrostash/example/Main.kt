package dev.logickoder.retrostash.example

import androidx.compose.material3.Surface
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Retrostash Playground",
        state = rememberWindowState(size = DpSize(420.dp, 720.dp)),
    ) {
        Surface { App() }
    }
}
