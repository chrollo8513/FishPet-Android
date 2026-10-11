package com.example.fishpet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object FishArt {
    fun render(s: FishState): Bitmap {
        val bmp = Bitmap.createBitmap(480, 230, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(18, 83, 124); c.drawRoundRect(RectF(0f,0f,480f,230f),28f,28f,p)
        p.color = Color.rgb(29,120,152)
        for (i in 0..5) c.drawCircle(38f + i * 80, 36f + (i % 3) * 27, 7f + i % 3, p)
        p.color = Color.rgb(35,130,113)
        for (i in 0..6) c.drawOval(15f+i*76,165f-(i%3)*28,30f+i*76,245f,p)
        val scale = when(s.stage) { "Baby" -> .60f; "Juvenile" -> .82f; else -> 1f }
        c.save(); c.translate(240f,120f); c.scale(scale,scale)
        p.color = if (s.dead) Color.GRAY else if(s.mood == "Sick") Color.rgb(177,140,82) else Color.rgb(255,160,51)
        val tail = Path().apply { moveTo(-65f,0f); lineTo(-135f,-50f); lineTo(-125f,55f); close() }
        c.drawPath(tail,p)
        c.drawOval(-85f,-48f,85f,48f,p)
        p.color = Color.rgb(255,204,103); c.drawOval(-10f,-42f,50f,40f,p)
        p.color = Color.WHITE; c.drawCircle(45f,-13f,13f,p)
        p.color = Color.rgb(12,31,47); c.drawCircle(49f,-13f,6f,p)
        if(s.dead) { p.color = Color.BLACK; p.strokeWidth = 4f; c.drawLine(36f,-22f,54f,-4f,p); c.drawLine(54f,-22f,36f,-4f,p) }
        p.color = Color.rgb(18,49,65); c.drawArc(RectF(52f,4f,71f,21f),0f,180f,false,p)
        c.restore()
        return bmp
    }
}
