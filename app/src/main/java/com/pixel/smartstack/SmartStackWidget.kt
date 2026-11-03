package com.pixel.smartstack

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.text.SimpleDateFormat
import java.util.*

val CARD_INDEX_KEY = intPreferencesKey("card_index")

class SmartStackReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmartStackWidget()
}

class FlipCardAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[CARD_INDEX_KEY] ?: 0
            prefs[CARD_INDEX_KEY] = (current + 1) % 3
        }
        SmartStackWidget().update(context, glanceId)
    }
}

class SmartStackWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("smart_stack_prefs", Context.MODE_PRIVATE)
        val note = prefs.getString("note", "⚡ Buy coffee\n📦 Pick up package\n🎯 Gym at 6 PM") ?: ""
        val weather = prefs.getString("weather", "72°F • Sunny & Clear") ?: ""
        val dateStr = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()).uppercase()
        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        provideContent {
            val state = currentState<Preferences>()
            val activeCard = state[CARD_INDEX_KEY] ?: 0

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF1C1C1E)))
                    .cornerRadius(28.dp)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smooth Scrollable iPhone-Style Stack (Swipe Up/Down OR Tap to Flip)
                LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                    item {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable(actionRunCallback<FlipCardAction>())
                        ) {
                            when (activeCard) {
                                0 -> ClockCard(dateStr, timeStr)
                                1 -> WeatherCard(weather)
                                else -> NotesCard(note)
                            }
                        }
                    }
                    item {
                        // Scroll down to peek at all cards in the stack!
                        Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("↓ STACK OVERVIEW ↓", style = TextStyle(color = ColorProvider(Color(0xFF8E8E93)), fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            Spacer(GlanceModifier.height(8.dp))
                            WeatherCard(weather)
                            Spacer(GlanceModifier.height(10.dp))
                            NotesCard(note)
                        }
                    }
                }

                // iPhone Smart Stack Side Dots Indicator
                Column(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .padding(start = 10.dp)
                        .clickable(actionRunCallback<FlipCardAction>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (i in 0..2) {
                        val dotColor = if (i == activeCard) Color(0xFFFFFFFF) else Color(0xFF636366)
                        Text("●", style = TextStyle(color = ColorProvider(dotColor), fontSize = 12.sp))
                    }
                }
            }
        }
    }

    @Composable
    private fun ClockCard(dateStr: String, timeStr: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF2C2C2E)))
                .cornerRadius(22.dp)
                .padding(16.dp)
        ) {
            Text(dateStr, style = TextStyle(color = ColorProvider(Color(0xFFFF453A)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(4.dp))
            Text(timeStr, style = TextStyle(color = ColorProvider(Color.White), fontSize = 34.sp, fontWeight = FontWeight.Bold))
            Text("Tap card or dots to flip • Scroll down for stack", style = TextStyle(color = ColorProvider(Color(0xFF8E8E93)), fontSize = 11.sp))
        }
    }

    @Composable
    private fun WeatherCard(weather: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF0A84FF)))
                .cornerRadius(22.dp)
                .padding(16.dp)
        ) {
            Text("☀️ WEATHER", style = TextStyle(color = ColorProvider(Color(0xFFE5F1FF)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text(weather, style = TextStyle(color = ColorProvider(Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold))
            Text("H: 76°  L: 58° • Updated just now", style = TextStyle(color = ColorProvider(Color(0xFFD0E8FF)), fontSize = 12.sp))
        }
    }

    @Composable
    private fun NotesCard(note: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(Color(0xFF2C2C2E)))
                .cornerRadius(22.dp)
                .padding(16.dp)
        ) {
            Text("📝 NOTES & TO-DO", style = TextStyle(color = ColorProvider(Color(0xFFFFD60A)), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            Text(note, style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp))
        }
    }
}
