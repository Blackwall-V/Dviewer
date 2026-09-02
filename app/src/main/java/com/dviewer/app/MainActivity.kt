package com.dviewer.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.dviewer.app.ui.DviewerNavHost
import com.dviewer.app.ui.theme.DviewerTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DviewerTheme {
                DviewerNavHost()
            }
        }
    }
}
