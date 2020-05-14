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

package com.ivianuu.injekt.comparison

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import com.ivianuu.injekt.comparison.base.defaultConfig
import com.ivianuu.injekt.comparison.base.runInjectionTests
import com.ivianuu.injekt.comparison.injekt.InjektTest

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runInjectionTests(InjektTest, config = defaultConfig.copy(rounds = 100000))
        setContentView(FrameLayout(this).apply { setBackgroundColor(Color.BLACK) })
    }
}
