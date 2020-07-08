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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Component

class Foo

class Bar(foo: Foo)

class Baz(foo: Foo, bar: Bar)

@Component
interface TestComponent {
    @Component.Factory
    interface Factory {
        fun create(): TestComponent
    }
}

interface Command

class CommandA : Command

class CommandB : Command

class CommandC : Command
