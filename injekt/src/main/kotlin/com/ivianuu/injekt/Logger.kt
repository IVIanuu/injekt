/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

/**
 * Logs useful information
 */
interface Logger {

    /**
     * Logs a debug message
     *
     * @param msg the message
     */
    fun debug(msg: String)

    /**
     * Logs a info message
     *
     * @param msg the message
     */
    fun info(msg: String)

    /**
     * Logs a warning message
     *
     * @param msg the message
     */
    fun warn(msg: String)

    /**
     * Logs a error message
     *
     * @param msg the message
     */
    fun error(msg: String)
}

/**
 * Default tag to use in a [Logger]
 */
const val INJEKT_TAG = "[INJEKT]"

/**
 * Logs messages using [System.out]
 */
class PrintLogger : Logger {

    override fun debug(msg: String) {
        println("[DEBUG] $INJEKT_TAG $msg")
    }

    override fun info(msg: String) {
        println("[INFO] $INJEKT_TAG $msg")
    }

    override fun warn(msg: String) {
        println("[WARN] $INJEKT_TAG $msg")
    }

    override fun error(msg: String) {
        println("[ERROR] $INJEKT_TAG $msg")
    }
}
