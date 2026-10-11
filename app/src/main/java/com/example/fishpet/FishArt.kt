package com.example.fishpet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object FishArt {
    private const val WIDTH = 720
    private const val HEIGHT = 400
    private const val BOWL_RADIUS = 196f
    private const val BITMAP_WIDTH = 480
    private const val BITMAP_HEIGHT = 480

    fun render(
        state: FishState,
        nowSeconds: Long = System.currentTimeMillis() / 1000L,
        motionTimeMillis: Long = nowSeconds * 1000L,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), state, nowSeconds, motionTimeMillis, BITMAP_WIDTH.toFloat(), BITMAP_HEIGHT.toFloat())
        return bitmap
    }

    fun draw(canvas: Canvas, state: FishState, nowSeconds: Long, motionTimeMillis: Long, width: Float, height: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.save()
        val scale = min(width / WIDTH, height / HEIGHT)
        canvas.translate((width - WIDTH * scale) / 2f, (height - HEIGHT * scale) / 2f)
        canvas.scale(scale, scale)
        canvas.clipPath(Path().apply { addCircle(WIDTH / 2f, HEIGHT / 2f, BOWL_RADIUS, Path.Direction.CW) })
        drawWater(canvas, paint, state, nowSeconds, motionTimeMillis)
        drawScenery(canvas, paint, motionTimeMillis)
        drawBubbles(canvas, paint, motionTimeMillis)
        drawFood(canvas, paint, state, nowSeconds, motionTimeMillis)
        val fish = drawFish(canvas, paint, state, motionTimeMillis)
        drawCareBadges(canvas, paint, state, fish.pose, fish.bodyHeight)
        drawBowlRim(canvas, paint)
        canvas.restore()
    }

    private data class RenderedFish(val pose: SwimmingPose, val bodyHeight: Float)

    private fun drawWater(canvas: Canvas, paint: Paint, state: FishState, nowSeconds: Long, motionTimeMillis: Long) {
        paint.shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(
            Color.rgb(118, 223, 245), Color.rgb(38, 153, 205), Color.rgb(15, 84, 147)
        ), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
        paint.shader = null
        paint.color = Color.argb(78, 255, 255, 255)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 18f, paint)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(130, 225, 252, 255)
        for (offset in 0..3) {
            val y = 18f + offset * 7f
            canvas.drawArc(RectF(-90f + offset * 54f, y - 10f, 270f + offset * 54f, y + 18f), 195f, 150f, false, paint)
        }
        if (state.isFeeding(nowSeconds)) {
            val duration = max(1L, state.feedingSceneUntilSeconds - state.lastFedAtSeconds)
            val elapsed = (motionTimeMillis - state.lastFedAtSeconds * 1000L).coerceIn(0L, duration * 1000L)
            val progress = elapsed.toFloat() / (duration * 1000L)
            val radius = 14f + progress * 58f
            paint.strokeWidth = 3f
            paint.color = Color.argb(((1f - progress) * 175f).toInt(), 242, 255, 255)
            canvas.drawOval(RectF(355f - radius, 28f - radius * .23f, 355f + radius, 28f + radius * .23f), paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(28, 255, 255, 255)
        canvas.drawOval(-110f, 56f, 210f, 330f, paint)
        canvas.drawOval(420f, 95f, 810f, 410f, paint)
    }

    private fun drawScenery(canvas: Canvas, paint: Paint, motionTimeMillis: Long) {
        paint.color = Color.rgb(16, 103, 109)
        canvas.drawOval(-50f, 300f, 220f, 455f, paint)
        canvas.drawOval(440f, 310f, 790f, 465f, paint)
        paint.color = Color.rgb(27, 120, 105)
        val sway = sin(motionTimeMillis / 1000.0 * Math.PI * 2.0 / 7.0).toFloat() * 5f
        repeat(7) { index ->
            val x = 55f + index * 96f
            val height = 72f + (index % 3) * 28f
            val plant = Path().apply {
                moveTo(x, 372f)
                cubicTo(x - 22f, 334f, x + sway - 22f, 326f - height, x + sway, 286f - height)
                cubicTo(x + 25f, 325f - height, x + 24f, 352f, x + 16f, 372f)
                close()
            }
            canvas.drawPath(plant, paint)
            paint.color = Color.rgb(39, 150, 120)
            canvas.drawOval(x - 17f, 324f - height / 2f, x + 9f, 365f - height / 2f, paint)
            paint.color = Color.rgb(27, 120, 105)
        }
        drawRock(canvas, paint, 104f, 349f, 72f, Color.rgb(67, 98, 121))
        drawRock(canvas, paint, 567f, 356f, 100f, Color.rgb(57, 87, 108))
        drawRock(canvas, paint, 644f, 364f, 55f, Color.rgb(91, 119, 135))
        paint.color = Color.rgb(201, 173, 117)
        canvas.drawOval(0f, 366f, WIDTH.toFloat(), 438f, paint)
        paint.color = Color.argb(60, 255, 255, 255)
        repeat(9) { index -> canvas.drawCircle(42f + index * 81f, 378f + (index % 3) * 6f, 3f, paint) }
    }

    private fun drawRock(canvas: Canvas, paint: Paint, x: Float, y: Float, size: Float, color: Int) {
        paint.color = color
        canvas.drawOval(x - size, y - size * .55f, x + size, y + size * .55f, paint)
        paint.color = Color.argb(55, 255, 255, 255)
        canvas.drawOval(x - size * .48f, y - size * .36f, x + size * .14f, y - size * .03f, paint)
    }

    private fun drawBubbles(canvas: Canvas, paint: Paint, motionTimeMillis: Long) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(180, 225, 255, 255)
        repeat(13) { index ->
            val x = 48f + (index * 113 % 645)
            val baseY = 330f - (index * 47 % 260)
            val drift = ((motionTimeMillis / 1000f + index * 3f) % 18f) * 2f
            val radius = 4f + index % 4 * 2f
            canvas.drawCircle(x, max(34f, baseY - drift), radius, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawFood(canvas: Canvas, paint: Paint, state: FishState, nowSeconds: Long, motionTimeMillis: Long) {
        if (!state.isFeeding(nowSeconds)) return
        val duration = max(1L, state.feedingSceneUntilSeconds - state.lastFedAtSeconds)
        val elapsed = (motionTimeMillis - state.lastFedAtSeconds * 1000L).coerceIn(0L, duration * 1000L)
        val progress = elapsed.toFloat() / (duration * 1000L)
        val y = 40f + progress * 165f
        paint.color = Color.rgb(118, 72, 28)
        repeat(5) { index ->
            canvas.drawCircle(306f + index * 23f, y + (index % 2) * 13f, 8f, paint)
        }
    }

    private fun drawFish(canvas: Canvas, paint: Paint, state: FishState, motionTimeMillis: Long): RenderedFish {
        val stageScale = when (state.stage()) {
            GrowthStage.BABY -> .62f
            GrowthStage.JUVENILE -> .82f
            GrowthStage.ADULT -> 1f
        }
        val slimness = (0.60f + state.health.toFloat() / 250f - max(0.0, state.hunger - 65.0).toFloat() / 220f).coerceIn(.50f, 1f)
        val bodyWidth = 178f * stageScale * slimness
        val bodyHeight = 100f * stageScale * slimness
        val pose = FishMotion.pose(state, motionTimeMillis)
        val bodyColor = fishColor(state)
        val finColor = blend(bodyColor, Color.rgb(255, 230, 132), .38f)
        val finDroop = when (state.swimmingState) {
            SwimmingState.STARVING -> 16f
            SwimmingState.CRITICAL -> 25f
            else -> 0f
        }
        canvas.save()
        canvas.translate(100f + pose.horizontalFraction * 520f, 210f + pose.bobOffset)
        canvas.scale(pose.facingScaleX, 1f)
        canvas.rotate(pose.bodyTiltDegrees)

        paint.color = finColor
        canvas.save()
        canvas.translate(-bodyWidth * .42f, 0f)
        canvas.rotate(pose.tailAngleDegrees)
        canvas.translate(bodyWidth * .42f, 0f)
        canvas.drawPath(Path().apply {
            moveTo(-bodyWidth * .45f, 0f)
            lineTo(-bodyWidth * 1.08f, -bodyHeight * .65f)
            lineTo(-bodyWidth * .96f, bodyHeight * .72f)
            close()
        }, paint)
        canvas.restore()
        canvas.save()
        canvas.translate(-bodyWidth * .05f, -bodyHeight * .32f)
        canvas.rotate(pose.finAngleDegrees + finDroop)
        canvas.translate(bodyWidth * .05f, bodyHeight * .32f)
        canvas.drawPath(Path().apply {
            moveTo(-bodyWidth * .05f, -bodyHeight * .32f)
            cubicTo(-bodyWidth * .14f, -bodyHeight * 1.02f, bodyWidth * .26f, -bodyHeight * .95f, bodyWidth * .38f, -bodyHeight * .35f)
            close()
        }, paint)
        canvas.drawPath(Path().apply {
            moveTo(0f, bodyHeight * .34f)
            cubicTo(-bodyWidth * .06f, bodyHeight * .96f, bodyWidth * .35f, bodyHeight * .83f, bodyWidth * .49f, bodyHeight * .34f)
            close()
        }, paint)
        canvas.restore()

        paint.color = Color.argb(58, 2, 42, 63)
        canvas.drawOval(-bodyWidth * .58f, -bodyHeight * .43f + 7f, bodyWidth * .72f, bodyHeight * .57f + 7f, paint)
        paint.shader = RadialGradient(
            bodyWidth * .24f,
            -bodyHeight * .25f,
            max(bodyWidth, bodyHeight),
            intArrayOf(blend(bodyColor, Color.WHITE, .56f), bodyColor, blend(bodyColor, Color.BLACK, .32f)),
            floatArrayOf(0f, .52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawOval(-bodyWidth * .62f, -bodyHeight * .52f, bodyWidth * .70f, bodyHeight * .52f, paint)
        paint.shader = null
        paint.color = Color.argb(92, 255, 230, 143)
        canvas.drawOval(-bodyWidth * .10f, -bodyHeight * .42f, bodyWidth * .58f, bodyHeight * .42f, paint)
        paint.color = Color.argb(120, 255, 255, 255)
        canvas.drawOval(bodyWidth * .10f, -bodyHeight * .38f, bodyWidth * .49f, -bodyHeight * .12f, paint)

        val eyeSize = if (state.stage() == GrowthStage.BABY) bodyHeight * .20f else bodyHeight * .16f
        if (state.color == FishColor.KOI && !state.dead) {
            paint.color = Color.argb(180, 252, 246, 226)
            canvas.drawOval(-bodyWidth * .15f, -bodyHeight * .38f, bodyWidth * .16f, -bodyHeight * .06f, paint)
            canvas.drawOval(bodyWidth * .12f, bodyHeight * .14f, bodyWidth * .43f, bodyHeight * .37f, paint)
        }
        drawEye(canvas, paint, bodyWidth * .42f, -bodyHeight * .19f, eyeSize, state.dead)
        if (state.stage() == GrowthStage.BABY) drawEye(canvas, paint, bodyWidth * .20f, -bodyHeight * .24f, eyeSize * .78f, state.dead)
        drawExpression(canvas, paint, state, bodyWidth, bodyHeight)
        canvas.restore()
        return RenderedFish(pose, bodyHeight)
    }

    private fun drawCareBadges(canvas: Canvas, paint: Paint, state: FishState, pose: SwimmingPose, bodyHeight: Float) {
        val hud = FishHudLayout.position(pose, bodyHeight)
        drawBadge(canvas, paint, hud.x - 104f, hud.y, "♥ ${state.health.toInt()}%", Color.rgb(222, 76, 98))
        drawBadge(canvas, paint, hud.x + 4f, hud.y, "● ${state.hunger.toInt()}%", Color.rgb(245, 180, 57))
    }

    private fun drawBadge(canvas: Canvas, paint: Paint, left: Float, top: Float, text: String, color: Int) {
        val badge = RectF(left, top, left + 100f, top + 28f)
        paint.color = Color.argb(205, 4, 41, 66)
        canvas.drawRoundRect(badge, 14f, 14f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = color
        canvas.drawRoundRect(badge, 14f, 14f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 13f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(text, badge.centerX(), badge.centerY() + 5f, paint)
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBowlRim(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.color = Color.argb(205, 218, 249, 255)
        canvas.drawCircle(WIDTH / 2f, HEIGHT / 2f, BOWL_RADIUS - 3f, paint)
        paint.strokeWidth = 2f
        paint.color = Color.argb(130, 255, 255, 255)
        canvas.drawCircle(WIDTH / 2f - 3f, HEIGHT / 2f - 3f, BOWL_RADIUS - 10f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawEye(canvas: Canvas, paint: Paint, x: Float, y: Float, radius: Float, dead: Boolean) {
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, radius, paint)
        paint.color = Color.rgb(32, 48, 69)
        if (dead) {
            paint.strokeWidth = max(3f, radius * .30f)
            canvas.drawLine(x - radius * .55f, y - radius * .55f, x + radius * .55f, y + radius * .55f, paint)
            canvas.drawLine(x + radius * .55f, y - radius * .55f, x - radius * .55f, y + radius * .55f, paint)
        } else {
            canvas.drawCircle(x + radius * .16f, y, radius * .48f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x + radius * .33f, y - radius * .22f, radius * .14f, paint)
        }
    }

    private fun drawExpression(canvas: Canvas, paint: Paint, state: FishState, bodyWidth: Float, bodyHeight: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(3f, bodyHeight * .055f)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(94, 56, 39)
        val mouth = RectF(bodyWidth * .45f, bodyHeight * .08f, bodyWidth * .67f, bodyHeight * .31f)
        when (state.mood) {
            FishMood.HAPPY -> canvas.drawArc(mouth, 8f, 148f, false, paint)
            FishMood.WORRIED -> canvas.drawArc(mouth, 208f, 124f, false, paint)
            FishMood.SAD, FishMood.CRITICAL, FishMood.DEAD -> canvas.drawArc(mouth, 205f, 135f, false, paint)
        }
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun fishColor(state: FishState): Int = when {
        state.dead -> Color.rgb(128, 139, 145)
        state.mood == FishMood.CRITICAL -> blend(baseColor(state), Color.rgb(181, 160, 131), .78f)
        state.mood == FishMood.SAD -> blend(baseColor(state), Color.rgb(225, 185, 118), .62f)
        else -> baseColor(state)
    }

    private fun baseColor(state: FishState): Int = Color.rgb(state.color.red, state.color.green, state.color.blue)

    private fun blend(first: Int, second: Int, amount: Float): Int {
        val inverse = 1f - amount
        return Color.rgb(
            (Color.red(first) * inverse + Color.red(second) * amount).toInt(),
            (Color.green(first) * inverse + Color.green(second) * amount).toInt(),
            (Color.blue(first) * inverse + Color.blue(second) * amount).toInt(),
        )
    }
}
