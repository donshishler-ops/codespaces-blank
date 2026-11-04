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
import android.graphics.Matrix
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

            val stackView = android.widget.StackView(context)
            // loop flag is applied via XML normally; re-set here isn't needed since
            // widget_stack.xml android:loopViews stays true — toggle is cosmetic via card art for now

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

    private var cards: List<CardDef> = emptyList()
    private var arrangement: Arrangement = Arrangement.STACKED

    override fun onCreate() {}

    override fun onDataSetChanged() {
        cards = StackSettings.getCards(ctx).filter { it.enabled }
        if (cards.isEmpty()) cards = StackSettings.defaultCards()
        arrangement = StackSettings.getArrangement(ctx)
    }

    override fun onDestroy() {}
    override fun getCount(): Int = cards.size
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 2
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true

    override fun getViewAt(position: Int): RemoteViews {
        val card = cards[position]
        val rv: RemoteViews
        val palette = palettes[position % palettes.size]

        if (card.id == "clock") {
            rv = RemoteViews(ctx.packageName, R.layout.widget_card_clock)
            rv.setImageViewBitmap(
                R.id.card_bg,
                CardArt.draw(520, 300, palette.first, palette.second, palette.third, palette.fourth, arrangement, position)
            )
        } else {
            rv = RemoteViews(ctx.packageName, R.layout.widget_card_text)
            rv.setImageViewBitmap(
                R.id.card_bg,
                CardArt.draw(520, 300, palette.first, palette.second, palette.third, palette.fourth, arrangement, position)
            )
            rv.setTextViewText(R.id.title, card.title)
            rv.setTextColor(R.id.title, palette.third)
            rv.setTextViewText(R.id.body, card.body)
        }
        rv.setOnClickFillInIntent(R.id.card_root, Intent())
        return rv
    }

    private data class Palette(val top: Int, val bottom: Int, val third: Int, val fourth: Int)
    private val palettes = listOf(
        Palette(Color.parseColor("#2B2B30"), Color.parseColor("#0E0E11"), Color.parseColor("#FF6B6B"), Color.parseColor("#FFB36B")),
        Palette(Color.parseColor("#2E7BFF"), Color.parseColor("#0A2A6B"), Color.parseColor("#7FD6FF"), Color.parseColor("#FFFFFF")),
        Palette(Color.parseColor("#2B2B30"), Color.parseColor("#0E0E11"), Color.parseColor("#FFD60A"), Color.parseColor("#FF9F0A"))
    )
    private val Palette.first get() = top
    private val Palette.second get() = bottom
}

object CardArt {
    fun draw(w: Int, h: Int, top: Int, bottom: Int, glowA: Int, glowB: Int,
              arrangement: Arrangement = Arrangement.STACKED, position: Int = 0): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val m = 18f
        val rad = 56f

        // Bake in the "arrangement" look as a static offset/rotation per card index,
        // since RemoteViews can't animate this live.
        when (arrangement) {
            Arrangement.OFFSET -> c.translate(position * 10f, position * 6f)
            Arrangement.CENTERED -> c.scale(1f - position * 0.03f, 1f - position * 0.03f, w / 2f, h / 2f)
            Arrangement.ANGLED -> c.rotate((position * 4f) - 4f, w / 2f, h / 2f)
            Arrangement.STACKED -> {}
        }

        val box = RectF(m, m, w - m, h - m)
        val sweep = intArrayOf(glowA, glowB, glowA, glowB, glowA)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 10f
        p.maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
        p.shader = SweepGradient(w / 2f, h / 2f, sweep, null)
        p.alpha = 150
        c.drawRoundRect(box, rad, rad, p)
        p.maskFilter = null

        p.style = Paint.Style.FILL
        p.alpha = 255
        p.shader = LinearGradient(0f, m, 0f, h - m, top, bottom, Shader.TileMode.CLAMP)
        c.drawRoundRect(box, rad, rad, p)

        p.shader = RadialGradient(
            w * 0.25f, m, w * 0.7f,
            intArrayOf(0x44FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP
        )
        c.drawRoundRect(box, rad, rad, p)

        p.shader = LinearGradient(0f, m, 0f, h * 0.5f, 0x2AFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(m, m, w - m, h * 0.5f), rad, rad, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 4f
        p.shader = SweepGradient(w / 2f, h / 2f, sweep, null)
        c.drawRoundRect(box, rad, rad, p)

        return bmp
    }
}
