package com.solosailing.ui.components.audio

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun SoundTestScreen(context: Context) {
    val categories = SoundConfig.Category.values()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        categories.forEach { category ->
            Text(
                text = category.label,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            category.sounds.forEach { sound ->
                Button(
                    onClick = { SoundConfig.play(context, sound) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Text("▶️ ${sound.name}")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Button(
            onClick = { SoundConfig.stop() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🛑 Detener", color = Color.White)
        }
    }
}
