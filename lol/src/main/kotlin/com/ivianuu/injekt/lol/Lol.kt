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

package com.ivianuu.injekt.lol

import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.create
import com.ivianuu.injekt.get
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.scoped

class Foo
class Bar(foo: Foo)

abstract class SuperClass {
    val foo: Foo by inject()
    lateinit var foo2: Foo

    @Inject
    fun injectFoo2(foo: Foo) {
        foo2 = foo
    }
}

class MyClass : SuperClass() {
    val bar: Bar by inject()
    lateinit var bar2: Bar

    @Inject
    fun injectBar2(bar: Bar) {
        bar2 = bar
    }
}

interface TestComponent {
    val injectMyClass: @MembersInjector (MyClass) -> Unit
    val foo: Foo
    val bar: Bar
}

@Factory
fun createComponent(): TestComponent {
    scoped { Foo() }
    scoped { Bar(get()) }
    return create()
}

fun invoke() {
    val testComponent = createComponent()
    val myClass = MyClass()
    testComponent.injectMyClass(myClass)
    check(myClass.foo === testComponent.foo) {
        "my class foo " + myClass.foo.toString() +
                "test component foo " + testComponent.foo.toString()
    }
    check(myClass.foo2 === testComponent.foo)
    check(myClass.bar === testComponent.bar)
    check(myClass.bar2 === testComponent.bar)
}
