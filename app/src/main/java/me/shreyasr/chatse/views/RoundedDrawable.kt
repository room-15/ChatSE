package me.shreyasr.chatse.views

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.ColorInt
import android.util.Log
import android.widget.ImageView.ScaleType

/**
 * Created by mauker on 23/08/17.
 */
class RoundedDrawable(bitmap: Bitmap) : Drawable() {

    private var mBounds = RectF()
    private var mDrawableRect = RectF()
    private var mBitmapRect = RectF()
    private var mBitmap: Bitmap
    private var mBitmapPaint: Paint
    private var mBitmapWidth: Int = 0
    private var mBitmapHeight: Int = 0
    private var mBorderRect = RectF()
    private var mBorderPaint: Paint
    private var mShaderMatrix = Matrix()
    private var mSquareCornersRect = RectF()

    private var mTileModeX : Shader.TileMode? = Shader.TileMode.CLAMP
    private var mTileModeY : Shader.TileMode? = Shader.TileMode.CLAMP
    private var mRebuildShader = true

    // [ topLeft, topRight, bottomLeft, bottomRight ]
    private var mCornerRadius = 0f
    private var mCornersRounded = booleanArrayOf(true, true, true, true)

    private var mOval = false
    private var mBorderWidth = 0f
    private var mBorderColor = ColorStateList.valueOf(DEFAULT_BORDER_COLOR)
    private var mScaleType = ScaleType.FIT_CENTER

    init {
        mBitmap = bitmap

        mBitmapWidth = mBitmap.width
        mBitmapHeight = mBitmap.height
        mBitmapRect.set(0f, 0f, mBitmapWidth.toFloat(), mBitmapHeight.toFloat())

        mBitmapPaint = Paint()
        mBitmapPaint.style = Paint.Style.FILL
        mBitmapPaint.isAntiAlias = true

        mBorderPaint = Paint()
        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.isAntiAlias = true
        mBorderPaint.color = mBorderColor.getColorForState(state, DEFAULT_BORDER_COLOR)
        mBorderPaint.strokeWidth = mBorderWidth
    }

    companion object {
        val LOG_TAG = RoundedDrawable::class.java.simpleName

        val DEFAULT_BORDER_COLOR = Color.WHITE

        fun fromBitmap(bitmap: Bitmap?): RoundedDrawable? {
            return if (bitmap != null) RoundedDrawable(bitmap)
            else null
        }

        fun fromDrawable(drawable: Drawable?): Drawable? {
            if (drawable != null) {
                if (drawable is RoundedDrawable) {
                    // just return if it's already a RoundedDrawable
                    return drawable
                } else if (drawable is LayerDrawable) {
                    val num = drawable.numberOfLayers

                    // loop through layers to and change to RoundedDrawables if possible
                    for (i in 0 until num - 1) {
                        val d = drawable.getDrawable(i)
                        drawable.setDrawableByLayerId(drawable.getId(i), fromDrawable(d))
                    }
                    return drawable
                }

                // try to get a bitmap from the drawable and
                val bm = drawableToBitmap(drawable)
                if (bm != null) {
                    return RoundedDrawable(bm)
                }
            }
            return drawable
        }

        fun drawableToBitmap(drawable: Drawable): Bitmap? {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val bitmap: Bitmap
            val width = Math.max(drawable.intrinsicWidth, 2)
            val height = Math.max(drawable.intrinsicHeight, 2)
            try {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.w(LOG_TAG, "Failed to create bitmap from drawable!")
                return null
            }

            return bitmap
        }

        private fun any(booleans: BooleanArray): Boolean = true in booleans

        private fun all(booleans: BooleanArray): Boolean = true !in booleans

        private fun only(index: Int, booleans: BooleanArray): Boolean {
            var i = 0
            val len = booleans.size
            while (i < len) {
                if (booleans[i] != (i == index)) {
                    return false
                }
                i++
            }
            return true
        }
    }

    private fun redrawBitmapForSquareCorners(canvas: Canvas) {
        if (all(mCornersRounded)) {
            // no square corners
            return
        }

        if (mCornerRadius == 0f) {
            return  // no round corners
        }

        val left = mDrawableRect.left
        val top = mDrawableRect.top
        val right = left + mDrawableRect.width()
        val bottom = top + mDrawableRect.height()
        val radius = mCornerRadius

        if (!mCornersRounded[Corner.TOP_LEFT.ordinal]) {
            mSquareCornersRect.set(left, top, left + radius, top + radius)
            canvas.drawRect(mSquareCornersRect, mBitmapPaint)
        }

        if (!mCornersRounded[Corner.TOP_RIGHT.ordinal]) {
            mSquareCornersRect.set(right - radius, top, right, radius)
            canvas.drawRect(mSquareCornersRect, mBitmapPaint)
        }

        if (!mCornersRounded[Corner.BOTTOM_RIGHT.ordinal]) {
            mSquareCornersRect.set(right - radius, bottom - radius, right, bottom)
            canvas.drawRect(mSquareCornersRect, mBitmapPaint)
        }

        if (!mCornersRounded[Corner.BOTTOM_LEFT.ordinal]) {
            mSquareCornersRect.set(left, bottom - radius, left + radius, bottom)
            canvas.drawRect(mSquareCornersRect, mBitmapPaint)
        }
    }

    private fun redrawBorderForSquareCorners(canvas: Canvas) {
        if (all(mCornersRounded)) {
            // no square corners
            return
        }

        if (mCornerRadius == 0f) {
            return  // no round corners
        }

        val left = mDrawableRect.left
        val top = mDrawableRect.top
        val right = left + mDrawableRect.width()
        val bottom = top + mDrawableRect.height()
        val radius = mCornerRadius
        val offset = mBorderWidth / 2

        if (!mCornersRounded[Corner.TOP_LEFT.ordinal]) {
            canvas.drawLine(left - offset, top, left + radius, top, mBorderPaint)
            canvas.drawLine(left, top - offset, left, top + radius, mBorderPaint)
        }

        if (!mCornersRounded[Corner.TOP_RIGHT.ordinal]) {
            canvas.drawLine(right - radius - offset, top, right, top, mBorderPaint)
            canvas.drawLine(right, top - offset, right, top + radius, mBorderPaint)
        }

        if (!mCornersRounded[Corner.BOTTOM_RIGHT.ordinal]) {
            canvas.drawLine(right - radius - offset, bottom, right + offset, bottom, mBorderPaint)
            canvas.drawLine(right, bottom - radius, right, bottom, mBorderPaint)
        }

        if (!mCornersRounded[Corner.BOTTOM_LEFT.ordinal]) {
            canvas.drawLine(left - offset, bottom, left + radius, bottom, mBorderPaint)
            canvas.drawLine(left, bottom - radius, left, bottom, mBorderPaint)
        }
    }

    /**
     * @param corner the specific corner to get radius of.
     * @return the corner radius of the specified corner.
     */
    fun getCornerRadius(corner: Int): Float {
        return if (mCornersRounded[corner]) mCornerRadius else 0f
    }

    /**
     * Sets all corners to the specified radius.
     *
     * @param radius the radius.
     * @return the [RoundedDrawable] for chaining.
     */
    fun setCornerRadius(radius: Float): RoundedDrawable {
        setCornerRadius(radius, radius, radius, radius)
        return this
    }

    /**
     * Sets the corner radii of all the corners.
     *
     * @param topLeft top left corner radius.
     * @param topRight top right corner radius
     * @param bottomRight bototm right corner radius.
     * @param bottomLeft bottom left corner radius.
     * @return the [RoundedDrawable] for chaining.
     */
    fun setCornerRadius(topLeft: Float, topRight: Float, bottomRight: Float,
                        bottomLeft: Float): RoundedDrawable {
        val radiusSet = HashSet<Float>(4)
        radiusSet.add(topLeft)
        radiusSet.add(topRight)
        radiusSet.add(bottomRight)
        radiusSet.add(bottomLeft)

        radiusSet.remove(0f)

        if (radiusSet.size > 1) {
            throw IllegalArgumentException("Multiple nonzero corner radii not yet supported.")
        }

        if (!radiusSet.isEmpty()) {
            val radius = radiusSet.iterator().next()
            if (java.lang.Float.isInfinite(radius) || java.lang.Float.isNaN(radius) || radius < 0) {
                throw IllegalArgumentException("Invalid radius value: " + radius)
            }
            mCornerRadius = radius
        } else {
            mCornerRadius = 0f
        }

        mCornersRounded[Corner.TOP_LEFT.ordinal] = topLeft > 0
        mCornersRounded[Corner.TOP_RIGHT.ordinal] = topRight > 0
        mCornersRounded[Corner.BOTTOM_RIGHT.ordinal] = bottomRight > 0
        mCornersRounded[Corner.BOTTOM_LEFT.ordinal] = bottomLeft > 0
        return this
    }

    fun setBorderWidth(width: Float): RoundedDrawable {
        mBorderWidth = width
        mBorderPaint.strokeWidth = mBorderWidth
        return this
    }

    fun getBorderColor(): Int {
        return mBorderColor.defaultColor
    }

    fun setBorderColor(@ColorInt color: Int): RoundedDrawable = setBorderColor(ColorStateList.valueOf(color))

    fun setBorderColor(colors: ColorStateList?): RoundedDrawable {
        mBorderColor = colors ?: ColorStateList.valueOf(0)
        mBorderPaint.color = mBorderColor.getColorForState(state, DEFAULT_BORDER_COLOR)
        return this
    }

    fun setOval(oval: Boolean): RoundedDrawable {
        mOval = oval
        return this
    }

    fun setScaleType(scaleType: ScaleType?): RoundedDrawable {
        val type : ScaleType = scaleType ?: ScaleType.FIT_CENTER

        if (mScaleType != scaleType) {
            mScaleType = type
            updateShaderMatrix()
        }
        return this
    }

    fun setTileModeX(tileModeX: Shader.TileMode?): RoundedDrawable {
        if (mTileModeX != tileModeX) {
            mTileModeX = tileModeX
            mRebuildShader = true
            invalidateSelf()
        }
        return this
    }

    fun setTileModeY(tileModeY: Shader.TileMode?): RoundedDrawable {
        if (mTileModeY != tileModeY) {
            mTileModeY = tileModeY
            mRebuildShader = true
            invalidateSelf()
        }
        return this
    }

    fun toBitmap(): Bitmap? = drawableToBitmap(this)

    private fun updateShaderMatrix() {
        val scale: Float
        var dx: Float
        var dy: Float

        when (mScaleType) {
            ScaleType.CENTER -> {
                mBorderRect.set(mBounds)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)

                mShaderMatrix.reset()
                mShaderMatrix.setTranslate(((mBorderRect.width() - mBitmapWidth) * 0.5f + 0.5f),
                        ((mBorderRect.height() - mBitmapHeight) * 0.5f + 0.5f))
            }

            ScaleType.CENTER_CROP -> {
                mBorderRect.set(mBounds)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)

                mShaderMatrix.reset()

                dx = 0f
                dy = 0f

                if (mBitmapWidth * mBorderRect.height() > mBorderRect.width() * mBitmapHeight) {
                    scale = mBorderRect.height() / mBitmapHeight
                    dx = (mBorderRect.width() - mBitmapWidth * scale) * 0.5f
                } else {
                    scale = mBorderRect.width() / mBitmapWidth
                    dy = (mBorderRect.height() - mBitmapHeight * scale) * 0.5f
                }

                mShaderMatrix.setScale(scale, scale)
                mShaderMatrix.postTranslate((dx + 0.5f) + mBorderWidth / 2,
                        (dy + 0.5f) + mBorderWidth / 2)
            }

            ScaleType.CENTER_INSIDE -> {
                mShaderMatrix.reset()

                if (mBitmapWidth <= mBounds.width() && mBitmapHeight <= mBounds.height()) {
                    scale = 1.0f
                } else {
                    scale = Math.min(mBounds.width() / mBitmapWidth,
                            mBounds.height() / mBitmapHeight)
                }

                dx = ((mBounds.width() - mBitmapWidth * scale) * 0.5f + 0.5f)
                dy = ((mBounds.height() - mBitmapHeight * scale) * 0.5f + 0.5f)

                mShaderMatrix.setScale(scale, scale)
                mShaderMatrix.postTranslate(dx, dy)

                mBorderRect.set(mBitmapRect)
                mShaderMatrix.mapRect(mBorderRect)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)
                mShaderMatrix.setRectToRect(mBitmapRect, mBorderRect, Matrix.ScaleToFit.FILL)
            }

            ScaleType.FIT_END -> {
                mBorderRect.set(mBitmapRect)
                mShaderMatrix.setRectToRect(mBitmapRect, mBounds, Matrix.ScaleToFit.END)
                mShaderMatrix.mapRect(mBorderRect)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)
                mShaderMatrix.setRectToRect(mBitmapRect, mBorderRect, Matrix.ScaleToFit.FILL)
            }

            ScaleType.FIT_START -> {
                mBorderRect.set(mBitmapRect)
                mShaderMatrix.setRectToRect(mBitmapRect, mBounds, Matrix.ScaleToFit.START)
                mShaderMatrix.mapRect(mBorderRect)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)
                mShaderMatrix.setRectToRect(mBitmapRect, mBorderRect, Matrix.ScaleToFit.FILL)
            }

            ScaleType.FIT_XY -> {
                mBorderRect.set(mBounds)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)
                mShaderMatrix.reset()
                mShaderMatrix.setRectToRect(mBitmapRect, mBorderRect, Matrix.ScaleToFit.FILL)
            }

            else -> {
                mBorderRect.set(mBitmapRect)
                mShaderMatrix.setRectToRect(mBitmapRect, mBounds, Matrix.ScaleToFit.CENTER)
                mShaderMatrix.mapRect(mBorderRect)
                mBorderRect.inset(mBorderWidth / 2, mBorderWidth / 2)
                mShaderMatrix.setRectToRect(mBitmapRect, mBorderRect, Matrix.ScaleToFit.FILL)
            }
        }

        mDrawableRect.set(mBorderRect)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getAlpha(): Int {
        return mBitmapPaint.alpha
    }

    override fun setAlpha(alpha: Int) {
        mBitmapPaint.alpha = alpha
        invalidateSelf()
    }

    override fun getColorFilter(): ColorFilter? {
        return mBitmapPaint.colorFilter
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mBitmapPaint.colorFilter = cf
        invalidateSelf()
    }

    override fun setDither(dither: Boolean) {
        mBitmapPaint.isDither = dither
        invalidateSelf()
    }

    override fun setFilterBitmap(filter: Boolean) {
        mBitmapPaint.isFilterBitmap = filter
        invalidateSelf()
    }

    override fun isStateful(): Boolean = mBorderColor.isStateful

    override fun onStateChange(state: IntArray?): Boolean {
        val newColor : Int = mBorderColor.getColorForState(state, 0)
        if (mBorderPaint.color != newColor) {
            mBorderPaint.color = newColor
            return true
        } else {
            return super.onStateChange(state)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)

        mBounds.set(bounds)
        updateShaderMatrix()
    }

    override fun draw(canvas: Canvas) {
        if (mRebuildShader) {
            val bitmapShader = BitmapShader(mBitmap, mTileModeX, mTileModeY)
            if (mTileModeX === Shader.TileMode.CLAMP && mTileModeY === Shader.TileMode.CLAMP) {
                bitmapShader.setLocalMatrix(mShaderMatrix)
            }
            mBitmapPaint.shader = bitmapShader
            mRebuildShader = false
        }

        if (mOval) {
            if (mBorderWidth > 0) {
                canvas.drawOval(mDrawableRect, mBitmapPaint);
                canvas.drawOval(mBorderRect, mBorderPaint);
            } else {
                canvas.drawOval(mDrawableRect, mBitmapPaint);
            }
        }
        else {
            if (any(mCornersRounded)) {
                val radius: Float = mCornerRadius
                if (mBorderWidth > 0) {
                    canvas.drawRoundRect(mDrawableRect, radius, radius, mBitmapPaint)
                    canvas.drawRoundRect(mBorderRect, radius, radius, mBorderPaint)
                    redrawBitmapForSquareCorners(canvas)
                    redrawBorderForSquareCorners(canvas)
                } else {
                    canvas.drawRoundRect(mDrawableRect, radius, radius, mBitmapPaint)
                    redrawBitmapForSquareCorners(canvas)
                }
            } else {
                canvas.drawRect(mDrawableRect, mBitmapPaint)
                if (mBorderWidth > 0) {
                    canvas.drawRect(mBorderRect, mBorderPaint)
                }
            }
        }
    }
}