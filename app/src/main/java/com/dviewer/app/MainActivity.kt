package com.dviewer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.dviewer.app.ui.theme.DviewerTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DviewerTheme {
                Surface(modifier = Modifier) {
                    Text("Dviewer")
                }
            }
        }
    }
}
