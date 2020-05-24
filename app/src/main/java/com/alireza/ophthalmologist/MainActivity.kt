package com.alireza.ophthalmologist

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import com.alireza.colourpicker.R


class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setBrightness(100)
        main_activity.setBackgroundColor(Color.rgb(0,71,171))

        brightnessBar.setOnSeekBarChangeListener(this)

        fullscreenButton.setOnClickListener {
            var myIntent = Intent(this, ColorActivity::class.java)
            myIntent.putExtra("brightness", brightnessBar.progress)

            startActivity(myIntent)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
        //Get the chnaged value
        when (seekBar) {
            brightnessBar -> brightnessBar.progress = progress
        }
        //Build and show the cobalt blue colour
        setBrightness(brightnessBar.progress)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }

    private fun setBrightness(brightness: Int){
        val lp = window.attributes
        lp.screenBrightness = brightness / 100.0f
        window.attributes = lp
    }
}
