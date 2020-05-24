package com.alireza.ophthalmologist

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import com.alireza.colourpicker.R
import kotlinx.android.synthetic.main.activity_color.*


class ColorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color)

        val intent = intent

        val brightness = intent.getIntExtra("brightness",100)

        color_activity.setBackgroundColor(Color.rgb(0,71,171))
        setBrightness(brightness)
    }

    private fun setBrightness(brightness: Int){
        val lp = window.attributes
        lp.screenBrightness = brightness / 100.0f
        window.attributes = lp
    }
}
