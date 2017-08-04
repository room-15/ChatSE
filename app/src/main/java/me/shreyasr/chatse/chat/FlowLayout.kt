package me.shreyasr.chatse.chat

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * @author RAW
 */
class FlowLayout : ViewGroup {

    private var line_height: Int = 0

    class LayoutParams
    /**
     * @param horizontal_spacing Pixels between items, horizontally
     * *
     * @param vertical_spacing   Pixels between items, vertically
     */
    (val horizontal_spacing: Int, val vertical_spacing: Int) : ViewGroup.LayoutParams(0, 0)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        assert(View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.UNSPECIFIED)

        val width = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var height = View.MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom
        val count = childCount
        var line_height = 0

        var xpos = paddingLeft
        var ypos = paddingTop

        val childHeightMeasureSpec: Int
        if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.AT_MOST) {
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
        } else {
            childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        }


        for (i in 0..count - 1) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as LayoutParams
                child.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST), childHeightMeasureSpec)
                val childw = child.measuredWidth
                line_height = Math.max(line_height, child.measuredHeight + lp.vertical_spacing)

                if (xpos + childw > width) {
                    xpos = paddingLeft
                    ypos += line_height
                }

                xpos += childw + lp.horizontal_spacing
            }
        }
        this.line_height = line_height

        if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.UNSPECIFIED) {
            height = ypos + line_height

        } else if (View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.AT_MOST) {
            if (ypos + line_height < height) {
                height = ypos + line_height
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams {
        return LayoutParams(1, 1) // default of 1px spacing
    }

    override fun generateLayoutParams(
            p: android.view.ViewGroup.LayoutParams): android.view.ViewGroup.LayoutParams {
        return LayoutParams(1, 1)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean {
        if (p is LayoutParams) {
            return true
        }
        return false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val width = r - l
        var xpos = paddingLeft
        var ypos = paddingTop

        for (i in 0..count - 1) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val childw = child.measuredWidth
                val childh = child.measuredHeight
                val lp = child.layoutParams as LayoutParams
                if (xpos + childw > width) {
                    xpos = paddingLeft
                    ypos += line_height
                }
                child.layout(xpos, ypos, xpos + childw, ypos + childh)
                xpos += childw + lp.horizontal_spacing
            }
        }
    }
}