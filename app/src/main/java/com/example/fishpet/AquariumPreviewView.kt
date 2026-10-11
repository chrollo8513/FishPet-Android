package com.example.fishpet

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/** Smooth, in-app-only animation. Widgets receive battery-friendly rendered snapshots instead. */
class AquariumPreviewView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
) : View(context, attributes) {
    var fishState: FishState = FishState()
        set(value) {
            field = value
            invalidate()
        }

    private var running = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimationState()
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimationState()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimationState()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val nowSeconds = System.currentTimeMillis() / 1000L
        FishArt.draw(canvas, fishState, nowSeconds, System.currentTimeMillis(), width.toFloat(), height.toFloat())
        if (running) postInvalidateOnAnimation()
    }

    private fun updateAnimationState() {
        running = isAttachedToWindow && windowVisibility == VISIBLE && isShown
        if (running) postInvalidateOnAnimation()
    }
}
