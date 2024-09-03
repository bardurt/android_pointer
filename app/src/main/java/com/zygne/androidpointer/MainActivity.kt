package com.zygne.androidpointer

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zygne.androidpointer.pointer.Content
import com.zygne.androidpointer.pointer.PointerLayout

class MainActivity : AppCompatActivity() {

    private lateinit var textViewOutput: TextView
    private lateinit var buttonA: Button
    private lateinit var buttonB: Button
    private lateinit var pointerLayout: PointerLayout
    private lateinit var view: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewOutput = findViewById(R.id.textViewOutput)
        buttonA = findViewById(R.id.buttonA)
        buttonB = findViewById(R.id.buttonB)
        pointerLayout = findViewById(R.id.pointerLayout)
        view = findViewById(R.id.main)


        buttonA.setOnClickListener {
            textViewOutput.text = "You Clicked A"
        }

        buttonB.setOnClickListener {
            textViewOutput.text = "You Clicked B"
        }

        val content = Content(view)
        pointerLayout.setContent(content)
    }

    override fun onDestroy() {
        super.onDestroy()
        pointerLayout.close()
    }

    internal class Content(private val view: View) : com.zygne.androidpointer.pointer.Content {
        override fun scrollTo(x: Int, y: Int) {
            view.scrollTo(x, y)
        }

        override val scrollX: Int
            get() = view.scrollX

        override val scrollY: Int
            get() = view.scrollY

        override fun canScrollVertically(direction: Int): Boolean {
            return view.canScrollVertically(direction)
        }

        override fun canScrollHorizontally(direction: Int): Boolean {
            return view.canScrollHorizontally(direction)
        }

        override fun onMouseVisibilityChanged(visible: Boolean) {
        }

    }
}