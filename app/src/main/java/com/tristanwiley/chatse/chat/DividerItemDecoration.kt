package com.tristanwiley.chatse.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View

@SuppressLint("DuplicateDivider")
class DividerItemDecoration @JvmOverloads constructor(
        context: Context,
        private val orientation: ListOrientation = ListOrientation.VERTICAL
) : RecyclerView.ItemDecoration() {

    /**
     * The actual divider that is displayed.
     */
    private val divider: Drawable

    init {
        val attrs = context.obtainStyledAttributes(ATTRS)
        divider = attrs.getDrawable(DIVIDER_POSITION)!!
        attrs.recycle()
    }

    /**
     * Draws the divider depending on the orientation of the RecyclerView.
     */
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            when (orientation) {
                ListOrientation.VERTICAL -> {
                    val top = child.bottom + params.bottomMargin
                    val bottom = top + divider.intrinsicHeight
                    divider.setBounds(parent.paddingLeft, top,
                            parent.width - parent.paddingRight, bottom)
                }
                ListOrientation.HORIZONTAL -> {
                    val left = child.right + params.rightMargin
                    val right = left + divider.intrinsicHeight
                    divider.setBounds(left, parent.paddingTop, right,
                            parent.height - parent.paddingBottom)
                }
            }

            divider.draw(c)
        }
    }

    /**
     * Determines the offset of the divider based on the orientation of the list.
     */
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) =
            when (orientation) {
                ListOrientation.VERTICAL -> outRect.set(0, 0, 0, divider.intrinsicHeight)
                ListOrientation.HORIZONTAL -> outRect.set(0, 0, divider.intrinsicWidth, 0)
            }

    companion object {
        // Attributes for the divider
        private val ATTRS = intArrayOf(android.R.attr.listDivider)
        private const val DIVIDER_POSITION = 0
    }

    enum class ListOrientation {
        HORIZONTAL,
        VERTICAL
    }
}