package com.alessandrolattao.lanotifica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.alessandrolattao.lanotifica.ui.screens.MainScreen
import com.alessandrolattao.lanotifica.ui.theme.LaNotificaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaNotificaTheme { Surface(modifier = Modifier.fillMaxSize()) { MainScreen() } }
        }
    }
}
