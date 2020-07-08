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

package com.ivianuu.injekt.sample

import android.app.Application
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.internal.DoubleCheck
import com.ivianuu.injekt.runReader

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applicationComponent.runReader {
            initializeWorkers()
            startAppServices()
            refreshRepo()
        }
    }

}

class Foo

@Scoped(MyComponent::class)
fun foo(context: fooContext): Foo = Foo()
interface fooContext

class Bar(foo: Foo)

@Scoped(MyComponent::class)
fun bar(context: barContext): Bar = Bar(context.foo())
interface barContext {
    fun foo(): Foo
}

class MyComponent : fooContext, barContext, ReaderContext {
    private val foo = DoubleCheck { foo(this) }
    override fun foo() = foo.invoke()
    private val bar = DoubleCheck { bar(this) }
    override fun bar() = bar.invoke()
}

interface ReaderContext {
    fun bar(): Bar
}
