package com.alireza.ophthalmologist

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.ActivityCompat
import com.alireza.colourpicker.R
import com.alireza.ophthalmologist.Listeners.PictureCapturingListener
import kotlinx.android.synthetic.main.activity_color.*
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import com.alireza.ophthalmologist.Services.APictureCapturingService
import com.alireza.ophthalmologist.Services.PictureCapturingServiceImpl
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Build
import android.view.KeyEvent


class ColorActivity : AppCompatActivity(), PictureCapturingListener, ActivityCompat.OnRequestPermissionsResultCallback  {
    private var pictureService: APictureCapturingService? = null

    private val requiredPermissions = arrayOf<String>(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
    private val MY_PERMISSIONS_REQUEST_ACCESS_CODE = 1

    override fun onCaptureDone(pictureUrl: String, pictureData: ByteArray) {
        if (pictureData != null && pictureUrl != null) {
            showToast("Picture saved to $pictureUrl")
        }
    }

    override fun onDoneCapturingAllPhotos(picturesTaken: TreeMap<String, ByteArray>) {
        if (picturesTaken != null && !picturesTaken.isEmpty()) {
            showToast("Done capturing all photos!");
            return;
        }
        showToast("No camera detected!");
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color)

        val intent = intent

        val brightness = intent.getIntExtra("brightness",100)

        color_activity.setBackgroundColor(Color.rgb(0,71,171))
        setBrightness(brightness)

        //check for camera and external storage permissions
        checkPermissions()
        pictureService = PictureCapturingServiceImpl.getInstance(this);

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            showToast("Starting capture!")
            pictureService!!.startCapturing(this)
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_CODE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    checkPermissions()
                }
            }
        }
    }

    private fun setBrightness(brightness: Int){
        val lp = window.attributes
        lp.screenBrightness = brightness / 100.0f
        window.attributes = lp
    }

    private fun checkPermissions() {
        val neededPermissions = ArrayList<String>()
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(applicationContext,
                            permission) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(permission)
            }
        }
        if (!neededPermissions.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(neededPermissions.toTypedArray(),
                        MY_PERMISSIONS_REQUEST_ACCESS_CODE)
            }
        }
    }

    private fun showToast(text: String) {
        runOnUiThread { Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show() }
    }
}
