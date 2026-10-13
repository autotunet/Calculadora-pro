package com.example

import android.app.Application
import android.system.Os

class FileManagerApplication : Application() {

    companion object {
        init {
            configureGraphicsEnvironment()
        }

        private fun configureGraphicsEnvironment() {
            try {
                // Suppress Mesa and EGL driver search logs when running in emulator/container without DRM nodes
                Os.setenv("EGL_LOG_LEVEL", "fatal", true)
                Os.setenv("MESA_DEBUG", "0", true)
                Os.setenv("MESA_NO_ERROR", "1", true)
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("GALLIUM_DRIVER", "softpipe", true)
                Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
            } catch (_: Throwable) {}
        }
    }

    override fun onCreate() {
        super.onCreate()
        configureGraphicsEnvironment()
    }
}
