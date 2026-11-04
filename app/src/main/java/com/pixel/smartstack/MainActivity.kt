package com.pixel.smartstack

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            var cards by remember { mutableStateOf(StackSettings.getCards(this)) }
            var arrangement by remember { mutableStateOf(StackSettings.getArrangement(this)) }
            var loop by remember { mutableStateOf(StackSettings.getLoop(this)) }
            var newTitle by remember { mutableStateOf("") }
            var newBody by remember { mutableStateOf("") }

            fun sync() {
                StackSettings.saveCards(this, cards)
                StackSettings.setArrangement(this, arrangement)
                StackSettings.setLoop(this, loop)
                val mgr = AppWidgetManager.getInstance(this)
                val ids = mgr.getAppWidgetIds(ComponentName(this, SmartStackReceiver::class.java))
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.stack)
            }

            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF000000)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("iOS Smart Stack", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

                Text("Arrangement", color = Color.Gray, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Stacked" to com.pixel.smartstack.Arrangement.STACKED,
                        "Offset" to com.pixel.smartstack.Arrangement.OFFSET,
                        "Centered" to com.pixel.smartstack.Arrangement.CENTERED,
                        "Angled" to com.pixel.smartstack.Arrangement.ANGLED
                    ).forEach { (label, value) ->
                        FilterChip(
                            selected = arrangement == value,
                            onClick = { arrangement = value; sync() },
                            label = { Text(label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Loop widget", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = loop, onCheckedChange = { loop = it; sync() })
                }

                Text("Cards", color = Color.Gray, fontSize = 14.sp)
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cards) { card ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = card.enabled,
                                onCheckedChange = { checked ->
                                    cards = cards.map { if (it.id == card.id) it.copy(enabled = checked) else it }
                                    sync()
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(card.title, color = Color.White)
                                Text(card.body.take(40), color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text("Add your own card", color = Color.Gray, fontSize = 14.sp)
                OutlinedTextField(
                    value = newTitle, onValueChange = { newTitle = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = newBody, onValueChange = { newBody = it },
                    label = { Text("Text") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            cards = cards + CardDef(
                                id = "custom_${System.currentTimeMillis()}",
                                title = newTitle, body = newBody, enabled = true
                            )
                            newTitle = ""; newBody = ""
                            sync()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Add Card") }
            }
        }
    }
}
