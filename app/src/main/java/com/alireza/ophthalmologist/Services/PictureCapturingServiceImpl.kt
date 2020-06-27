package com.alireza.ophthalmologist.Services

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.alireza.ophthalmologist.Listeners.PictureCapturingListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class PictureCapturingServiceImpl
/***
 * private constructor, meant to force the use of [.getInstance]  method
 */
private constructor(activity: Activity) : APictureCapturingService(activity) {

    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    /***
     * camera ids queue.
     */
    private var cameraIds: Queue<String>? = null

    private var currentCameraId: String? = null
    private var cameraClosed: Boolean = false
    /**
     * stores a sorted map of (pictureUrlOnDisk, PictureData).
     */
    private var picturesTaken: TreeMap<String, ByteArray>? = null
    private var capturingListener: PictureCapturingListener? = null

    private val captureListener = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest,
                                        result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            if (picturesTaken!!.lastEntry() != null) {
                capturingListener!!.onCaptureDone(picturesTaken!!.lastEntry().key, picturesTaken!!.lastEntry().value)
                Log.i(TAG, "done taking picture from camera " + cameraDevice!!.id)
            }
            closeCamera()
        }
    }

    private val onImageAvailableListener = { imReader: ImageReader ->
        val image = imReader.acquireLatestImage()
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        saveImageToDisk(bytes)
        image.close()
    }

    private fun setupFPS(builder: CaptureRequest.Builder) {
        var fpsRange = getRange()
        if (fpsRange != null) {
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        }
    }

    private fun getRange(): Range<Int>? {
        var chars: CameraCharacteristics? = null
        try {
            chars = this.manager.getCameraCharacteristics(this.currentCameraId)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

        val ranges = chars!!.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

        var result: Range<Int>? = null

        var biggestDifference = 0

        for (range in ranges!!) {
            var currentDifference = range.upper - range.lower
            val upper = range.upper

            if (currentDifference > biggestDifference) {
                biggestDifference = currentDifference
                result = range
            }
        }
        return result
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraClosed = false
            Log.d(TAG, "camera " + camera.id + " opened")
            cameraDevice = camera
            Log.i(TAG, "Taking picture from camera " + camera.id)
            //
            if (null == cameraDevice) {
                Log.e(TAG, "cameraDevice is null")
                return
            }
            val characteristics = manager.getCameraCharacteristics(cameraDevice!!.id)
            var jpegSizes: Array<Size>? = null
            val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (streamConfigurationMap != null) {
                jpegSizes = streamConfigurationMap!!.getOutputSizes(ImageFormat.JPEG)
            }
            val jpegSizesNotEmpty = jpegSizes != null && jpegSizes.isNotEmpty()
            val width = if (jpegSizesNotEmpty) jpegSizes!![0].width else 640
            val height = if (jpegSizesNotEmpty) jpegSizes!![0].height else 480
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces = ArrayList<Surface>()
            outputSurfaces.add(reader.surface)
            val captureBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            captureBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false)
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            //captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            //captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 6)
            //captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation)
            setupFPS(captureBuilder)

            reader.setOnImageAvailableListener(onImageAvailableListener, null)
            val request = captureBuilder.build()
            //Take the picture after some delay. It may resolve getting a black dark photos.
            Handler().postDelayed({
                try {
                    takePicture(request, outputSurfaces)
                } catch (e: CameraAccessException) {
                    Log.e(TAG, " exception occurred while taking picture from " + currentCameraId!!, e)
                }
            }, 500)
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, " camera " + camera.id + " disconnected")
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true
                cameraDevice!!.close()
            }
        }

        override fun onClosed(camera: CameraDevice) {
            cameraClosed = true
            Log.d(TAG, "camera " + camera.id + " closed")
            //once the current camera has been closed, start taking another picture
            if (!cameraIds!!.isEmpty()) {
                takeAnotherPicture()
            } else {
                capturingListener!!.onDoneCapturingAllPhotos(picturesTaken!!)
            }
        }


        override fun onError(camera: CameraDevice, error: Int) {
            Log.e(TAG, "camera in error, int code $error")
            if (cameraDevice != null && !cameraClosed) {
                cameraDevice!!.close()
            }
        }
    }

    /**
     * Starts pictures capturing treatment.
     *
     * @param listener picture capturing listener
     */
    override fun startCapturing(listener: PictureCapturingListener) {
        this.picturesTaken = TreeMap()
        this.capturingListener = listener
        this.cameraIds = LinkedList()
        try {

            val cameraIds = manager.cameraIdList

            if (cameraIds.isNotEmpty()) {
//                this.cameraIds!!.addAll(cameraIds)
                this.cameraIds!!.add("1")
                this.currentCameraId = this.cameraIds!!.poll()
                openCamera()
            } else {
                //No camera detected!
                capturingListener!!.onDoneCapturingAllPhotos(picturesTaken!!)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Exception occurred while accessing the list of cameras", e)
        }

    }

    private fun openCamera() {
        Log.d(TAG, "opening camera " + currentCameraId!!)
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(currentCameraId, stateCallback, null)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, " exception occurred while opening camera " + currentCameraId!!, e)
        }

    }


    @Throws(CameraAccessException::class)
    private fun takePicture(captureRequest: CaptureRequest, outputSurfaces: List<Surface>) {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null")
            return
        }
        cameraDevice!!.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                try {
                    session.capture(captureRequest, captureListener, null)
                } catch (e: CameraAccessException) {
                    Log.e(TAG, " exception occurred while accessing " + currentCameraId!!, e)
                }

            }

            override fun onConfigureFailed(session: CameraCaptureSession) {}
        }, null)
    }


    private fun saveImageToDisk(bytes: ByteArray) {
        val cameraId = if (this.cameraDevice == null) UUID.randomUUID().toString() else this.cameraDevice!!.id
        val file = File(Environment.getExternalStorageDirectory().toString() + "/ophthalmologist/" + cameraId + "_pic.jpg")
        try {
            FileOutputStream(file).use({ output ->
                output.write(bytes)
                this.picturesTaken!!.put(file.path, bytes)
            })
        } catch (e: IOException) {
            Log.e(TAG, "Exception occurred while saving picture to external storage ", e)
        }

    }

    private fun takeAnotherPicture() {
        this.currentCameraId = this.cameraIds!!.poll()
        openCamera()
    }

    private fun closeCamera() {
        Log.d(TAG, "closing camera " + cameraDevice!!.id)
        if (null != cameraDevice && !cameraClosed) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }

    companion object {

        private val TAG = PictureCapturingServiceImpl::class.java.simpleName

        /**
         * @param activity the activity used to get the app's context and the display manager
         * @return a new instance
         */
        fun getInstance(activity: Activity): APictureCapturingService {
            return PictureCapturingServiceImpl(activity)
        }
    }


}