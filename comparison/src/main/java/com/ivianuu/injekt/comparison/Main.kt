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

package com.ivianuu.injekt.comparison

import com.ivianuu.injekt.comparison.custom.CustomTest
import com.ivianuu.injekt.comparison.injekt.InjektTest
import com.ivianuu.injekt.comparison.katana.KatanaTest
import com.ivianuu.injekt.comparison.kodein.KodeinTest
import com.ivianuu.injekt.comparison.koin.KoinTest

fun main(args: Array<String>) {
    //runAllInjectionTests()
    runInjectionTests(CustomTest, KodeinTest, KoinTest, KatanaTest, InjektTest)
}