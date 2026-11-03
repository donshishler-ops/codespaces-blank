package com.pixel.smartstack

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("smart_stack_prefs", Context.MODE_PRIVATE)
        setContent {
            var noteText by remember {
                mutableStateOf(prefs.getString("note", "⚡ Buy coffee\n📦 Pick up package\n🎯 Gym at 6 PM") ?: "")
            }
            var weatherText by remember {
                mutableStateOf(prefs.getString("weather", "72°F • Sunny & Clear") ?: "")
            }
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF000000)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("iOS Smart Stack", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Customize your swipeable stack widget.", color = Color.Gray, fontSize = 15.sp)
                OutlinedTextField(
                    value = weatherText,
                    onValueChange = { weatherText = it },
                    label = { Text("Weather Card Text") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Notes Card Items") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        prefs.edit().putString("note", noteText).putString("weather", weatherText).apply()
                        val mgr = AppWidgetManager.getInstance(this@MainActivity)
                        val ids = mgr.getAppWidgetIds(
                            ComponentName(this@MainActivity, SmartStackReceiver::class.java)
                        )
                        mgr.notifyAppWidgetViewDataChanged(ids, R.id.stack)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Save & Sync Widget", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
