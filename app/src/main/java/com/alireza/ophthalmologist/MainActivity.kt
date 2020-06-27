package com.alireza.ophthalmologist

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

        // Set a click listener for button widget
        aboutButton.setOnClickListener{
            // Initialize a new layout inflater instance
            val inflater: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            // Inflate a custom view using layout inflater
            val view = inflater.inflate(R.layout.dialog_view,null)

            // Initialize a new instance of popup window
            val popupWindow = PopupWindow(
                    view, // Custom view to show in popup window
                    LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
                    LinearLayout.LayoutParams.WRAP_CONTENT // Window height
            )

            // Set an elevation for the popup window
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.elevation = 10.0F
            }


            // If API level 23 or higher then execute the code
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                // Create a new slide animation for popup window enter transition
                val slideIn = Slide()
                slideIn.slideEdge = Gravity.TOP
                popupWindow.enterTransition = slideIn

                // Slide animation for popup window exit transition
                val slideOut = Slide()
                slideOut.slideEdge = Gravity.RIGHT
                popupWindow.exitTransition = slideOut

            }

            val buttonPopup = view.findViewById<Button>(R.id.button_popup)


            // Set a click listener for popup's button widget
            buttonPopup.setOnClickListener{
                // Dismiss the popup window
                popupWindow.dismiss()
            }

            val textView = view.findViewById<TextView>(R.id.text_view)
            val content = resources.getString(R.string.about).replace("\\n",System.getProperty("line.separator"))
            textView.setText(content)

            popupWindow.showAtLocation(
                    main_activity, // Location to display popup window
                    Gravity.CENTER, // Exact position of layout to display popup
                    0, // X offset
                    0 // Y offset
            )
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
