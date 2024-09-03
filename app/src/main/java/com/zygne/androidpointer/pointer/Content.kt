package com.zygne.androidpointer.pointer

interface Content {
    val scrollX: Int
    val scrollY: Int
    fun scrollTo(x: Int, y: Int)
    fun canScrollVertically(direction: Int): Boolean
    fun canScrollHorizontally(direction: Int): Boolean
    fun onMouseVisibilityChanged(visible: Boolean)
}