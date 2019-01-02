/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.android

import android.util.Log
import com.ivianuu.injekt.INJEKT_TAG
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.Logger

/**
 * [Logger] for android
 */
class AndroidLogger : Logger {

    override fun debug(msg: String) {
        Log.d(INJEKT_TAG, msg)
    }

    override fun info(msg: String) {
        Log.i(INJEKT_TAG, msg)
    }

    override fun warn(msg: String) {
        Log.w(INJEKT_TAG, msg)
    }

    override fun error(msg: String) {
        Log.e(INJEKT_TAG, msg)
    }

}

/**
 * Sets the [AndroidLogger]
 */
fun InjektPlugins.androidLogger() {
    logger = AndroidLogger()
}