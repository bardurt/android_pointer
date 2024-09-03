package com.zygne.androidpointer.pointer

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.abs

class PointerLayout : FrameLayout {
    private var clock = AppClock
    private var cursorRadius = 0
    private val cursorDirection = Point(0, 0)
    private val cursorPosition = PointF(0.0f, 0.0f)
    private val cursorSpeed = PointF(0.0f, 0.0f)
    private var content: Content? = null
    private val gameLoop: GameLoop = GameLoop(this)
    private var dpadCenterPressed = false
    private var lastCursorUpdate = clock.getTimeMillis()
    private val paint = Paint()
    private val tmpPointF = PointF()
    private var cursorHideNotification = false
    private var cursorVisibleNotification = false
    private val loadingSizes = intArrayOf(2, 6, 10, 14)
    private var loadingIndex = 0
    private var loadingTick = 0


    @Volatile
    private var loading = false
    private val handler = Handler(Looper.getMainLooper())

    fun setContent(content: Content?) {
        this.content = content
        val t = Thread(gameLoop)
        t.start()
    }

    private fun bound(f: Float, f2: Float): Float {
        if (f > f2) {
            return f2
        }
        val f3 = -f2
        return f.coerceAtLeast(f3)
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        init()
    }

    @Suppress("deprecation")
    private fun init() {
        if (!isInEditMode) {
            paint.isAntiAlias = true
            setWillNotDraw(false)
            val defaultDisplay =
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val point = Point()
            defaultDisplay.getSize(point)
            CURSOR_STROKE_WIDTH = (point.x / 300).toFloat()
            cursorRadius = point.x / 25
            MAX_CURSOR_SPEED = (point.x / 25).toFloat()
            SCROLL_START_PADDING = point.x / 15
        }
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        return super.onInterceptTouchEvent(motionEvent)
    }

    public override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        super.onSizeChanged(i, i2, i3, i4)
        if (!isInEditMode) {
            cursorPosition[i.toFloat() / 2.0f] = i2.toFloat() / 2.0f
        }
    }

    override fun dispatchKeyEvent(keyEvent: KeyEvent): Boolean {
        val keyCode = keyEvent.keyCode
        if (!(keyCode == 66 || keyCode == 160)) {
            when (keyCode) {
                19 -> {
                    if (keyEvent.action == 0) {
                        if (cursorPosition.y <= 0.0f) {
                            return super.dispatchKeyEvent(keyEvent)
                        }
                        handleDirectionKeyEvent(keyEvent, -100, -1, true)
                    } else if (keyEvent.action == 1) {
                        handleDirectionKeyEvent(keyEvent, -100, 0, false)
                    }
                    return true
                }

                20 -> {
                    if (keyEvent.action == 0) {
                        if (cursorPosition.y >= height.toFloat()) {
                            return super.dispatchKeyEvent(keyEvent)
                        }
                        handleDirectionKeyEvent(keyEvent, -100, 1, true)
                    } else if (keyEvent.action == 1) {
                        handleDirectionKeyEvent(keyEvent, -100, 0, false)
                    }
                    return true
                }

                21 -> {
                    if (keyEvent.action == 0) {
                        if (cursorPosition.x <= 0.0f) {
                            return super.dispatchKeyEvent(keyEvent)
                        }
                        handleDirectionKeyEvent(keyEvent, -1, -100, true)
                    } else if (keyEvent.action == 1) {
                        handleDirectionKeyEvent(keyEvent, 0, -100, false)
                    }
                    return true
                }

                22 -> {
                    if (keyEvent.action == 0) {
                        if (cursorPosition.x >= width.toFloat()) {
                            return super.dispatchKeyEvent(keyEvent)
                        }
                        handleDirectionKeyEvent(keyEvent, 1, -100, true)
                    } else if (keyEvent.action == 1) {
                        handleDirectionKeyEvent(keyEvent, 0, -100, false)
                    }
                    return true
                }

                23 -> {}
                else -> when (keyCode) {
                    268 -> {
                        if (keyEvent.action == 0) {
                            handleDirectionKeyEvent(keyEvent, -1, -1, true)
                        } else if (keyEvent.action == 1) {
                            handleDirectionKeyEvent(keyEvent, 0, 0, false)
                        }
                        return true
                    }

                    269 -> {
                        if (keyEvent.action == 0) {
                            handleDirectionKeyEvent(keyEvent, -1, 1, true)
                        } else if (keyEvent.action == 1) {
                            handleDirectionKeyEvent(keyEvent, 0, 0, false)
                        }
                        return true
                    }

                    270 -> {
                        if (keyEvent.action == 0) {
                            handleDirectionKeyEvent(keyEvent, 1, -1, true)
                        } else if (keyEvent.action == 1) {
                            handleDirectionKeyEvent(keyEvent, 0, 0, false)
                        }
                        return true
                    }

                    271 -> {
                        if (keyEvent.action == 0) {
                            handleDirectionKeyEvent(keyEvent, 1, 1, true)
                        } else if (keyEvent.action == 1) {
                            handleDirectionKeyEvent(keyEvent, 0, 0, false)
                        }
                        return true
                    }
                }
            }
        }
        if (!isCursorHidden) {
            if (keyEvent.action == 0 && !keyDispatcherState.isTracking(keyEvent)) {
                keyDispatcherState.startTracking(keyEvent, this)
                dpadCenterPressed = true
                dispatchMotionEvent(cursorPosition.x, cursorPosition.y, 0)
            } else if (keyEvent.action == 1) {
                keyDispatcherState.handleUpEvent(keyEvent)
                dispatchMotionEvent(cursorPosition.x, cursorPosition.y, 1)
                dpadCenterPressed = false
            }
            return true
        }
        return super.dispatchKeyEvent(keyEvent)
    }

    private fun dispatchMotionEvent(x: Float, y: Float, action: Int) {
        val uptimeMillis = SystemClock.uptimeMillis()
        val uptimeMillis2 = SystemClock.uptimeMillis()
        val pointerProperties = PointerProperties()
        pointerProperties.id = 0
        pointerProperties.toolType = 1
        val pointerPropertiesArr = arrayOf(pointerProperties)
        val pointerCoords = PointerCoords()
        pointerCoords.x = x
        pointerCoords.y = y
        pointerCoords.pressure = 1.0f
        pointerCoords.size = 1.0f
        dispatchTouchEvent(
            MotionEvent.obtain(
                uptimeMillis,
                uptimeMillis2,
                action,
                1,
                pointerPropertiesArr,
                arrayOf(pointerCoords),
                0,
                0,
                1.0f,
                1.0f,
                0,
                0,
                0,
                0
            )
        )
    }

    private fun handleDirectionKeyEvent(keyEvent: KeyEvent, x: Int, y: Int, down: Boolean) {
        var i = x
        var i2 = y
        if (!down) {
            keyDispatcherState.handleUpEvent(keyEvent)
            cursorSpeed[0.0f] = 0.0f
        } else if (!keyDispatcherState.isTracking(keyEvent)) {
            keyDispatcherState.startTracking(keyEvent, this)
        } else {
            return
        }
        val point = cursorDirection
        if (i == -100) {
            i = point.x
        }
        if (i2 == -100) {
            i2 = cursorDirection.y
        }
        point[i] = i2
    }

    public override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!isInEditMode && !isCursorHidden) {
            val poxX = cursorPosition.x
            val posY = cursorPosition.y
            if (loading) {
                paint.color = Color.argb(128, 3, 189, 113)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(poxX, posY, cursorRadius.toFloat(), paint)
                paint.color = Color.argb(64, 3, 189, 113)
                paint.strokeWidth = CURSOR_STROKE_WIDTH
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(poxX, posY, cursorRadius.toFloat(), paint)
                paint.color = Color.parseColor("#00D1FF")
                paint.strokeWidth = 6f
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(
                    poxX,
                    posY,
                    cursorRadius.toFloat() + loadingSizes[loadingIndex],
                    paint
                )
                loadingTick++
                if (loadingTick > 1) {
                    loadingTick = 0
                    loadingIndex++
                    if (loadingIndex > loadingSizes.size - 1) {
                        loadingIndex = 0
                    }
                }
            } else {
                paint.color = Color.argb(128, 3, 189, 113)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(poxX, posY, cursorRadius.toFloat(), paint)
                paint.color = Color.argb(64, 3, 189, 113)
                paint.strokeWidth = CURSOR_STROKE_WIDTH
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(poxX, posY, cursorRadius.toFloat(), paint)
                paint.color = Color.parseColor("#03BD47")
                paint.strokeWidth = 1f
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(poxX, posY, cursorRadius.toFloat(), paint)
            }
        }
    }

    private val isCursorHidden: Boolean
        get() = if (loading) {
            false
        } else {
            clock.getTimeMillis() - lastCursorUpdate > CURSOR_TIME_TO_HIDE
        }

    fun setLoading(loading: Boolean) {
        this.loading = loading
    }

    fun close() {
        gameLoop.running = false
    }

    private inner class GameLoop(private val pointerLayout: PointerLayout) : Runnable {
        @Volatile
        var running = false
        override fun run() {
            running = true
            while (running) {
                val currentTimeMillis = clock.getTimeMillis()
                val lastUpdate = currentTimeMillis - pointerLayout.lastCursorUpdate
                val time = lastUpdate.toFloat() * 0.05f
                val cursorSpeedX = pointerLayout.cursorSpeed.x
                val bound = pointerLayout.bound(
                    cursorSpeedX + pointerLayout.bound(
                        pointerLayout.cursorDirection.x.toFloat(),
                        1.0f
                    ) * time, MAX_CURSOR_SPEED
                )
                val cursorSpeedY = pointerLayout.cursorSpeed.y
                pointerLayout.cursorSpeed[bound] = pointerLayout.bound(
                    cursorSpeedY + pointerLayout.bound(
                        pointerLayout.cursorDirection.y.toFloat(),
                        1.0f
                    ) * time, MAX_CURSOR_SPEED
                )
                if (abs(pointerLayout.cursorSpeed.x) < 0.1f) {
                    pointerLayout.cursorSpeed.x = 0.0f
                }
                if (abs(pointerLayout.cursorSpeed.y) < 0.1f) {
                    pointerLayout.cursorSpeed.y = 0.0f
                }
                if (pointerLayout.cursorDirection.x == 0 && pointerLayout.cursorDirection.y == 0 && pointerLayout.cursorSpeed.x == 0.0f && pointerLayout.cursorSpeed.y == 0.0f) {
                    if (isCursorHidden) {
                        if (!loading) {
                            if (!cursorHideNotification) {
                                handler.post { content!!.onMouseVisibilityChanged(false) }
                                cursorHideNotification = true
                                cursorVisibleNotification = false
                            }
                        }
                    }
                } else {
                    if (!cursorVisibleNotification) {
                        handler.post { content!!.onMouseVisibilityChanged(true) }
                        cursorVisibleNotification = true
                        cursorHideNotification = false
                    }
                    pointerLayout.lastCursorUpdate = clock.getTimeMillis()
                    pointerLayout.tmpPointF.set(pointerLayout.cursorPosition)
                    pointerLayout.cursorPosition.offset(
                        pointerLayout.cursorSpeed.x,
                        pointerLayout.cursorSpeed.y
                    )
                    if (pointerLayout.cursorPosition.x < 0.0f) {
                        pointerLayout.cursorPosition.x = 0.0f
                    } else if (pointerLayout.cursorPosition.x > (pointerLayout.width - 1).toFloat()) {
                        pointerLayout.cursorPosition.x = (pointerLayout.width - 1).toFloat()
                    }
                    if (pointerLayout.cursorPosition.y < 0.0f) {
                        pointerLayout.cursorPosition.y = 0.0f
                    } else if (pointerLayout.cursorPosition.y > (pointerLayout.height - 1).toFloat()) {
                        pointerLayout.cursorPosition.y = (pointerLayout.height - 1).toFloat()
                    }
                    if (pointerLayout.tmpPointF != pointerLayout.cursorPosition && pointerLayout.dpadCenterPressed) {
                        pointerLayout.dispatchMotionEvent(
                            pointerLayout.cursorPosition.x,
                            pointerLayout.cursorPosition.y,
                            2
                        )
                    }
                    if (content != null) {
                        if (pointerLayout.cursorPosition.y > (pointerLayout.height - SCROLL_START_PADDING).toFloat()) {
                            if (pointerLayout.cursorSpeed.y > 0.0f && content!!.canScrollVertically(
                                    pointerLayout.cursorSpeed.y.toInt()
                                )
                            ) {
                                handler.post {
                                    content!!.scrollTo(
                                        content!!.scrollX,
                                        content!!.scrollY + pointerLayout.cursorSpeed.y.toInt()
                                    )
                                }
                            }
                        } else if (pointerLayout.cursorPosition.y < SCROLL_START_PADDING.toFloat() && pointerLayout.cursorSpeed.y < 0.0f && content!!.canScrollVertically(
                                pointerLayout.cursorSpeed.y.toInt()
                            )
                        ) {
                            handler.post {
                                content!!.scrollTo(
                                    content!!.scrollX,
                                    content!!.scrollY + pointerLayout.cursorSpeed.y.toInt()
                                )
                            }
                        }
                        if (pointerLayout.cursorPosition.x > (pointerLayout.width - SCROLL_START_PADDING).toFloat()) {
                            if (pointerLayout.cursorSpeed.x > 0.0f && content!!.canScrollHorizontally(
                                    pointerLayout.cursorSpeed.x.toInt()
                                )
                            ) {
                                handler.post {
                                    content!!.scrollTo(
                                        content!!.scrollX + pointerLayout.cursorSpeed.x.toInt(),
                                        content!!.scrollY
                                    )
                                }
                            }
                        } else if (pointerLayout.cursorPosition.x < SCROLL_START_PADDING.toFloat() && pointerLayout.cursorSpeed.x < 0.0f && content!!.canScrollHorizontally(
                                pointerLayout.cursorSpeed.x.toInt()
                            )
                        ) {
                            handler.post {
                                content!!.scrollTo(
                                    content!!.scrollX + pointerLayout.cursorSpeed.x.toInt(),
                                    content!!.scrollY
                                )
                            }
                        }
                    }
                }
                pointerLayout.invalidate()
                try {
                    Thread.sleep(25)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    companion object {
        const val TAG = "CursorLayout"
        private const val CURSOR_TIME_TO_HIDE: Long = 8000
        var CURSOR_STROKE_WIDTH = 0.0f
        var MAX_CURSOR_SPEED = 0.0f
        var SCROLL_START_PADDING = 100
    }
}