package com.example.system

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class SamScreenContextService : AccessibilityService() {

    companion object {
        private const val TAG = "SamScreenContextService"
        private var instance: SamScreenContextService? = null

        fun getInstance(): SamScreenContextService? = instance

        fun captureScreenBase64(onResult: (String?) -> Unit) {
            val service = instance
            if (service == null) {
                Log.e(TAG, "AccessibilityService instance is not running.")
                onResult(null)
                return
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    service.takeScreenshot(
                        android.view.Display.DEFAULT_DISPLAY,
                        service.mainExecutor,
                        object : TakeScreenshotCallback {
                            override fun onSuccess(screenshotResult: ScreenshotResult) {
                                val hardwareBuffer = screenshotResult.hardwareBuffer
                                val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshotResult.colorSpace)
                                if (bitmap != null) {
                                    val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    val outputStream = ByteArrayOutputStream()
                                    softwareBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                    val bytes = outputStream.toByteArray()
                                    val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
                                    onResult(base64)
                                } else {
                                    onResult(null)
                                }
                            }

                            override fun onFailure(errorCode: Int) {
                                Log.e(TAG, "Screenshot capture failed with code: $errorCode")
                                onResult(null)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to capture screenshot", e)
                    onResult(null)
                }
            } else {
                onResult(null)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Sam Screen Context Accessibility Service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Inspect screen node info or events if needed
    }

    override fun onInterrupt() {
        // Handle interrupt
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
