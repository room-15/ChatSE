package com.tristanwiley.chatse.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import com.tristanwiley.chatse.R


/**
 * Created by mauker on 23/08/17.
 */
class CircleImageView : AppCompatImageView {
    companion object {
        val LOG_TAG: String = CircleImageView::class.java.simpleName

        val DEFAULT_RADIUS = 1f
        val DEFAULT_BORDER_WIDTH = 1f
        val DEFAULT_TILE_MODE: Shader.TileMode = Shader.TileMode.CLAMP
    }

    private val mCornerRadii: FloatArray = floatArrayOf(DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS, DEFAULT_RADIUS)
    private var mBackgroundDrawable: Drawable? = null
    private var mBorderColor: ColorStateList? = ColorStateList.valueOf(Color.WHITE)
    private var mBorderWidth: Float = DEFAULT_BORDER_WIDTH
    private var mColorFilter: ColorFilter? = null
    private var mColorMod = false
    private var mDrawable: Drawable? = null
    private var mHasColorFilter = false
    private var mIsOval = false
    private var mMutateBackground = false
    private var mResource: Int = 0
    private var mBackgroundResource: Int = 0
    private var mScaleType: ScaleType? = ScaleType.CENTER_CROP
    private var mTileModeX: Shader.TileMode? = DEFAULT_TILE_MODE
    private var mTileModeY: Shader.TileMode? = DEFAULT_TILE_MODE

//    init {
//        init()
//    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }


    private fun init() {
        setBorderColor(mBorderColor)
        setBorderWidth(resources.getDimension(R.dimen.about_pic_border))
        mutateBackground(true)
        setOval(true)
    }

    override fun onMeasure(width: Int, height: Int) {
        super.onMeasure(width, height)
        if (parent != null) {
            val lp = layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = measuredHeight / 2
            layoutParams = lp
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun setScaleType(scaleType: ImageView.ScaleType?) {
        assert(scaleType != null)

        if (mScaleType !== scaleType) {
            mScaleType = scaleType ?: ScaleType.FIT_CENTER

            when (scaleType) {
                ImageView.ScaleType.CENTER, ImageView.ScaleType.CENTER_CROP,
                ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_CENTER,
                ImageView.ScaleType.FIT_START, ImageView.ScaleType.FIT_END,
                ImageView.ScaleType.FIT_XY -> super.setScaleType(ImageView.ScaleType.FIT_XY)

                else -> super.setScaleType(scaleType)
            }

            updateDrawableAttrs()
            updateBackgroundDrawableAttrs(false)
            invalidate()
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        mResource = 0
        mDrawable = RoundedDrawable.fromDrawable(drawable)
        updateDrawableAttrs()
        super.setImageDrawable(mDrawable)
    }

    override fun setImageBitmap(bm: Bitmap?) {
        mResource = 0
        mDrawable = RoundedDrawable.fromBitmap(bm)
        updateDrawableAttrs()
        super.setImageDrawable(mDrawable)
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        if (mResource != resId) {
            mResource = resId
            mDrawable = resolveResource()
            updateDrawableAttrs()
            super.setImageDrawable(mDrawable)
        }
    }

    override fun setBackgroundResource(@DrawableRes resId: Int) {
        if (mBackgroundResource != resId) {
            mBackgroundResource = resId
            mBackgroundDrawable = resolveBackgroundResource()
            background = mBackgroundDrawable
        }
    }

    override fun setColorFilter(cf: ColorFilter) {
        if (mColorFilter !== cf) {
            mColorFilter = cf
            mHasColorFilter = true
            mColorMod = true
            applyColorMod()
            invalidate()
        }
    }

    private fun applyColorMod() {
        // Only mutate and apply when modifications have occurred. This should
        // not reset the mColorMod flag, since these filters need to be
        // re-applied if the Drawable is changed.
        if (mDrawable != null && mColorMod) {
            mDrawable = mDrawable?.mutate()
            if (mHasColorFilter) {
                mDrawable?.colorFilter = mColorFilter
            }
            // TODO: support, eventually...
            //mDrawable.setXfermode(mXfermode);
            //mDrawable.setAlpha(mAlpha * mViewAlphaScale >> 8);
        }
    }

    private fun resolveResource(): Drawable? {
        if (mResource != 0) {
            try {
                return RoundedDrawable.fromDrawable(ContextCompat.getDrawable(context, mResource))
            } catch (e: Exception) {
                mResource = 0
            }

        }
        return null
    }

    private fun resolveBackgroundResource(): Drawable? {
        if (mBackgroundResource != 0) {
            try {
                return RoundedDrawable.fromDrawable(ContextCompat.getDrawable(context, mBackgroundResource))
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Unable to find resource: " + mBackgroundResource, e)
                // Don't try again.
                mBackgroundResource = 0
            }

        }
        return null
    }

    fun setBorderWidth(width: Float) {
        if (width == mBorderWidth) return
        mBorderWidth = width

        updateDrawableAttrs()
        updateBackgroundDrawableAttrs(false);
        invalidate()
    }

    fun setBorderColor(color: ColorStateList?) {
        if (mBorderColor == color) return

        mBorderColor = color ?: ColorStateList.valueOf(Color.WHITE)

        updateDrawableAttrs()
        updateBackgroundDrawableAttrs(false)

        if (mBorderWidth > 0)
            invalidate()
    }

    override fun setBackground(background: Drawable?) {
        mBackgroundDrawable = background
        updateBackgroundDrawableAttrs(true)
        super.setBackground(background)
    }

    private fun updateAttrs(drawable: Drawable?, scaleType: ImageView.ScaleType?) {
        if (drawable == null) {
            return
        }

        if (drawable is RoundedDrawable) {
            drawable.setScaleType(scaleType)
                    .setBorderWidth(mBorderWidth)
                    .setBorderColor(mBorderColor)
                    .setOval(mIsOval)
                    .setTileModeX(mTileModeX)
                    .setTileModeY(mTileModeY)

            if (this !is CircleImageView) {
                val tl = mCornerRadii[Corner.TOP_LEFT.ordinal]
                val tr = mCornerRadii[Corner.TOP_RIGHT.ordinal]
                val br = mCornerRadii[Corner.BOTTOM_RIGHT.ordinal]
                val bl = mCornerRadii[Corner.BOTTOM_LEFT.ordinal]
                drawable.setCornerRadius(tl, tr, br, bl)
            }


            applyColorMod()
        } else if (drawable is LayerDrawable) {
            // loop through layers to and set drawable attrs
            var i = 0
            val layers = drawable.numberOfLayers
            while (i < layers) {
                updateAttrs(drawable.getDrawable(i), scaleType)
                i++
            }
        }
    }

    private fun updateDrawableAttrs() {
        updateAttrs(mDrawable, mScaleType)
    }

    private fun updateBackgroundDrawableAttrs(convert: Boolean) {
        if (mMutateBackground) {
            if (convert) {
                mBackgroundDrawable = RoundedDrawable.fromDrawable(mBackgroundDrawable)
            }
            updateAttrs(mBackgroundDrawable, ImageView.ScaleType.FIT_XY)
        }
    }

    fun setOval(isOval: Boolean) {
        if (isOval == mIsOval) return

        mIsOval = isOval
        updateDrawableAttrs()
        updateBackgroundDrawableAttrs(false)
        invalidate()
    }

    fun mutateBackground(mutate: Boolean) {
        if (mutate == mMutateBackground) return

        mMutateBackground = mutate
        updateBackgroundDrawableAttrs(true)
        invalidate()
    }
}