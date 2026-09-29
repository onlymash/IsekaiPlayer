/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fiepi.mpv

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import androidx.annotation.Keep
import com.fiepi.mpv.constants.MpvLogLevel

@Keep
@Suppress("unused")
class MpvLib {

    companion object {
        init {
            System.loadLibrary("mpv")
            System.loadLibrary("mpv-android-jni")
        }
    }

    private var nativeContext: Long = 0

    external fun create(appContext: Context)
    external fun init()
    external fun destroy()
    external fun attachSurface(surface: Surface)
    external fun detachSurface()

    /**
     * Internal native command function.
     * Uses Array<String> to ensure JNI signature matches jobjectArray and eliminate IDE warnings.
     */
    private external fun nativeCommand(args: Array<String>)

    fun command(vararg args: String) {
        nativeCommand(arrayOf(*args))
    }

    external fun setOptionString(name: String, value: String): Int

    external fun grabThumbnail(dimension: Int): Bitmap?

    external fun getPropertyInt(property: String): Int?
    external fun setPropertyInt(property: String, value: Int)
    external fun getPropertyDouble(property: String): Double?
    external fun setPropertyDouble(property: String, value: Double)
    external fun getPropertyBoolean(property: String): Boolean?
    external fun setPropertyBoolean(property: String, value: Boolean)
    external fun getPropertyString(property: String): String?
    external fun setPropertyString(property: String, value: String)

    external fun observeProperty(property: String, format: Int)

    private val observers = mutableListOf<EventObserver>()

    fun addObserver(observer: EventObserver) {
        synchronized(observers) {
            observers.add(observer)
        }
    }

    fun removeObserver(observer: EventObserver) {
        synchronized(observers) {
            observers.remove(observer)
        }
    }

    @Keep
    private fun eventProperty(property: String, value: Long) {
        synchronized(observers) {
            for (o in observers) o.eventProperty(property, value)
        }
    }

    @Keep
    private fun eventProperty(property: String, value: Boolean) {
        synchronized(observers) {
            for (o in observers) o.eventProperty(property, value)
        }
    }

    @Keep
    private fun eventProperty(property: String, value: Double) {
        synchronized(observers) {
            for (o in observers) o.eventProperty(property, value)
        }
    }

    @Keep
    private fun eventProperty(property: String, value: String) {
        synchronized(observers) {
            for (o in observers) o.eventProperty(property, value)
        }
    }

    @Keep
    private fun eventProperty(property: String) {
        synchronized(observers) {
            for (o in observers) o.eventProperty(property)
        }
    }

    @Keep
    private fun event(eventId: Int) {
        synchronized(observers) {
            for (observer in observers) observer.event(eventId)
        }
    }

    private val logObservers = mutableListOf<LogObserver>()

    fun addLogObserver(observer: LogObserver) {
        synchronized(logObservers) {
            logObservers.add(observer)
        }
    }

    fun removeLogObserver(observer: LogObserver) {
        synchronized(logObservers) {
            logObservers.remove(observer)
        }
    }

    @Keep
    private fun logMessage(prefix: String, level: Int, text: String) {
        if (level <= MpvLogLevel.MPV_LOG_LEVEL_INFO) {
            val priority = when {
                level <= MpvLogLevel.MPV_LOG_LEVEL_ERROR -> android.util.Log.ERROR
                level <= MpvLogLevel.MPV_LOG_LEVEL_WARN -> android.util.Log.WARN
                else -> android.util.Log.INFO
            }
            android.util.Log.println(priority, "mpv-$prefix", text.trim())
        }

        synchronized(logObservers) {
            for (o in logObservers) o.logMessage(prefix, level, text)
        }
    }

    @Keep
    interface EventObserver {
        fun eventProperty(property: String)
        fun eventProperty(property: String, value: Long)
        fun eventProperty(property: String, value: Boolean)
        fun eventProperty(property: String, value: String)
        fun eventProperty(property: String, value: Double)
        fun event(eventId: Int)
    }

    @Keep
    interface LogObserver {
        fun logMessage(prefix: String, level: Int, text: String)
    }
}
