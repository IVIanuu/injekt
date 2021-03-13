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

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import com.ivianuu.injekt.Given

typealias SampleTheme = @Composable (@Composable () -> Unit) -> Unit

@Given
fun sampleTheme(): SampleTheme = { content ->
    MaterialTheme(colors = darkColors()) {
        Surface(content = content)
    }
}
