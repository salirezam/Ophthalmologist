package com.alireza.ophthalmologist.Services

import com.alireza.ophthalmologist.Listeners.PictureCapturingListener
import android.hardware.camera2.CameraManager
import android.app.Activity
import android.content.Context
import android.util.SparseIntArray
import android.view.Surface


abstract class APictureCapturingService
/***
 * constructor.
 *
 * @param activity the activity used to get display manager and the application context
 */
internal constructor(private val activity: Activity) {
    internal val context: Context = activity.applicationContext
    internal val manager: CameraManager

    /***
     * @return  orientation
     */
    internal val orientation: Int
        get() {
            val rotation = this.activity.windowManager.defaultDisplay.rotation
            return ORIENTATIONS.get(2)
        }

    init {
        this.manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }


    /**
     * starts pictures capturing process.
     *
     * @param listener picture capturing listener
     */
    abstract fun startCapturing(listener: PictureCapturingListener)

    companion object {

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
}