cat << 'EOF' > setup.sh
#!/bin/bash
mkdir -p app/src/main/java/com/pixel/smartstack
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/layout
mkdir -p app/src/main/res/values
mkdir -p .github/workflows

cat << 'END' > settings.gradle.kts
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "SmartStack"
include(":app")
END

cat << 'END' > build.gradle.kts
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
END

cat << 'END' > gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
END

cat << 'END' > app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pixel.smartstack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixel.smartstack"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
}
END

cat << 'END' > app/src/main/AndroidManifest.xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="iOS Smart Stack"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <receiver android:name=".SmartStackReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/smart_stack_info" />
        </receiver>
    </application>
</manifest>
END

cat << 'END' > app/src/main/res/layout/widget_loading.xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#1C1C1E">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Loading Smart Stack..."
        android:textColor="#FFFFFF" />
</FrameLayout>
END

cat << 'END' > app/src/main/res/xml/smart_stack_info.xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="140dp"
    android:targetCellWidth="4"
    android:targetCellHeight="2"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_loading"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
END

cat << 'END' > app/src/main/java/com/pixel/smartstack/MainActivity.kt
package com.pixel.smartstack

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("smart_stack_prefs", Context.MODE_PRIVATE)
        setContent {
            var noteText by remember { mutableStateOf(prefs.getString("note", "⚡ Buy coffee\n📦 Pick up package\n🎯 Gym at 6 PM") ?: "") }
            var weatherText by remember { mutableStateOf(prefs.getString("weather", "72°F • Sunny & Clear") ?: "") }
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF000000)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("iOS Smart Stack", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Customize your scrollable iPhone-style widget for your Pixel 10 Pro.", color = Color.Gray, fontSize = 15.sp)

                OutlinedTextField(
                    value = weatherText,
                    onValueChange = { weatherText = it },
                    label = { Text("Weather Card Text") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Notes Card Items") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Button(
                    onClick = {
                        prefs.edit().putString("note", noteText).putString("weather", weatherText).apply()
                        scope.launch { SmartStackWidget().updateAll(this@MainActivity) }
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
END

cat << 'END' > app/src/main/java/com/pixel/smartstack/SmartStackWidget.kt
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
END

cat << 'END' > .github/workflows/build-apk.yml
name: Build Smart Stack APK
on:
  push:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'

      - name: Build Debug APK
        run: gradle assembleDebug

      - name: Upload APK for Pixel 10 Pro
        uses: actions/upload-artifact@v4
        with:
          name: iOS-Smart-Stack-APK
          path: app/build/outputs/apk/debug/app-debug.apk
END

git add .
git commit -m "Build iOS Smart Stack Widget App"
git push origin main
EOF
bash setup.sh