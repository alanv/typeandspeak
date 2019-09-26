/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googamaphone.typeandspeak.utils

import android.util.Log

import java.util.IllegalFormatException

/**
 * Handles logging formatted strings.
 */
object LogUtils {

    private const val TAG = "LogUtils"

    /**
     * The minimum log level that will be printed to the console. Set this to
     * [Log.ERROR] for release or [Log.VERBOSE] for debugging.
     */
    private const val LOG_LEVEL = Log.ERROR

    /**
     * Logs a formatted string to the console using the source object's name as
     * the log tag. If the source object is null, the default tag (see
     * [LogUtils.TAG] is used.
     *
     *
     * Example usage: <br></br>
     * `
     * LogUtils.log(this, Log.ERROR, "Invalid value: %d", value);
    ` *
     *
     * @param source The object that generated the log event.
     * @param priority The log entry priority, see
     * [Log.println].
     * @param format A format string, see
     * [String.format].
     * @param args String formatter arguments.
     */
    fun log(source: Any?, priority: Int, format: String, vararg args: Any) {
        if (priority < LOG_LEVEL) {
            return
        }

        val sourceClass: String = when (source) {
            null -> TAG
            is Class<*> -> source.simpleName
            else -> source.javaClass.simpleName
        }

        try {
            Log.println(priority, sourceClass, String.format(format, *args))
        } catch (e: IllegalFormatException) {
            Log.e(TAG, "Bad formatting string: \"$format\"", e)
        }

    }
}
