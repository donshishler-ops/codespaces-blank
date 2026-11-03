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
        <service
            android:name=".StackService"
            android:permission="android.permission.BIND_REMOTEVIEWS"
            android:exported="false" />
    </application>
</manifest>
END

cat << 'END' > app/src/main/res/layout/widget_stack.xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <StackView
        android:id="@+id/stack"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:loopViews="true" />
    <TextView
        android:id="@+id/empty"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="Loading..."
        android:textColor="#FFFFFF" />
</FrameLayout>
END

cat << 'END' > app/src/main/res/layout/widget_card_clock.xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/card_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ImageView
        android:id="@+id/card_bg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY" />
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center_vertical"
        android:paddingLeft="28dp"
        android:paddingRight="28dp">
        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format12Hr="EEEE, MMM d"
            android:format24Hr="EEEE, MMM d"
            android:textAllCaps="true"
            android:textColor="#FF6B6B"
            android:textSize="13sp"
            android:textStyle="bold" />
        <TextClock
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:format12Hr="h:mm"
            android:format24Hr="HH:mm"
            android:fontFamily="sans-serif-light"
            android:textColor="#FFFFFF"
            android:textSize="58sp" />
    </LinearLayout>
</FrameLayout>
END

cat << 'END' > app/src/main/res/layout/widget_card_text.xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/card_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ImageView
        android:id="@+id/card_bg"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY" />
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center_vertical"
        android:paddingLeft="28dp"
        android:paddingRight="28dp">
        <TextView
            android:id="@+id/title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="12sp"
            android:textStyle="bold" />
        <TextView
            android:id="@+id/body"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:textColor="#FFFFFF"
            android:textSize="18sp" />
    </LinearLayout>
</FrameLayout>
END

cat << 'END' > app/src/main/java/com/pixel/smartstack/SmartStackWidget.kt
package com.pixel.smartstack

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class SmartStackReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val serviceIntent = Intent(context, StackService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))

            val views = RemoteViews(context.packageName, R.layout.widget_stack)
            views.setRemoteAdapter(R.id.stack, serviceIntent)
            views.setEmptyView(R.id.stack, R.id.empty)

            val open = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.stack, pending)
            manager.updateAppWidget(id, views)
        }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.stack)
    }
}

class StackService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return StackFactory(applicationContext)
    }
}

class StackFactory(private val ctx: Context) : RemoteViewsService.RemoteViewsFactory {
    private var weather = ""
    private var note = ""

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val prefs = ctx.getSharedPreferences("smart_stack_prefs", Context.MODE_PRIVATE)
        weather = prefs.getString("weather", "72°F • Sunny & Clear") ?: ""
        note = prefs.getString("note", "⚡ Buy coffee\n📦 Pick up package\n🎯 Gym at 6 PM") ?: ""
    }

    override fun onDestroy() {}
    override fun getCount(): Int = 3
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 2
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getViewAt(position: Int): RemoteViews {
        val rv: RemoteViews
        if (position == 0) {
            rv = RemoteViews(ctx.packageName, R.layout.widget_card_clock)
            rv.setImageViewBitmap(
                R.id.card_bg,
                CardArt.draw(520, 300, Color.parseColor("#2B2B30"), Color.parseColor("#0E0E11"),
                    Color.parseColor("#FF6B6B"), Color.parseColor("#FFB36B"))
            )
        } else if (position == 1) {
            rv = textCard("☀ WEATHER", weather, Color.parseColor("#CFE6FF"),
                CardArt.draw(520, 300, Color.parseColor("#2E7BFF"), Color.parseColor("#0A2A6B"),
                    Color.parseColor("#7FD6FF"), Color.parseColor("#FFFFFF")))
        } else {
            rv = textCard("📝 NOTES", note, Color.parseColor("#FFD60A"),
                CardArt.draw(520, 300, Color.parseColor("#2B2B30"), Color.parseColor("#0E0E11"),
                    Color.parseColor("#FFD60A"), Color.parseColor("#FF9F0A")))
        }
        rv.setOnClickFillInIntent(R.id.card_root, Intent())
        return rv
    }

    private fun textCard(title: String, body: String, titleColor: Int, bg: Bitmap): RemoteViews {
        val rv = RemoteViews(ctx.packageName, R.layout.widget_card_text)
        rv.setImageViewBitmap(R.id.card_bg, bg)
        rv.setTextViewText(R.id.title, title)
        rv.setTextColor(R.id.title, titleColor)
        rv.setTextViewText(R.id.body, body)
        return rv
    }
}

object CardArt {
    fun draw(w: Int, h: Int, top: Int, bottom: Int, glowA: Int, glowB: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val m = 18f
        val rad = 56f
        val box = RectF(m, m, w - m, h - m)
        val sweep = intArrayOf(glowA, glowB, glowA, glowB, glowA)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // soft outer glow
        p.style = Paint.Style.STROKE
        p.strokeWidth = 10f
        p.maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
        p.shader = SweepGradient(w / 2f, h / 2f, sweep, null)
        p.alpha = 150
        c.drawRoundRect(box, rad, rad, p)
        p.maskFilter = null

        // card body
        p.style = Paint.Style.FILL
        p.alpha = 255
        p.shader = LinearGradient(0f, m, 0f, h - m, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(box, rad, rad, p)

        // light bloom from the top-left
        p.shader = RadialGradient(
            w * 0.25f, m, w * 0.7f,
            intArrayOf(0x44FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
        )
        c.drawRoundRect(box, rad, rad, p)

        // glass sheen across the top half
        p.shader = LinearGradient(0f, m, 0f, h * 0.5f, 0x2AFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(m, m, w - m, h * 0.5f), rad, rad, p)

        // crisp gradient border
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.shader = SweepGradient(w / 2f, h / 2f, sweep, null)
        c.drawRoundRect(box, rad, rad, p)
        return bmp
    }
}
END

cat << 'END' > app/src/main/java/com/pixel/smartstack/MainActivity.kt
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
END

git add .
git commit -m "Swipeable snapping stack with glass clock card"
git push origin main