package org.buildsmali.viewer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MyViews {
    @Composable
    fun Greeting(name: String) {
        Text(text = "Hello $name!")
    }

    @Preview()
    @Composable
    fun SimpleComposablePreview() {
        Greeting("world2")
    }
}