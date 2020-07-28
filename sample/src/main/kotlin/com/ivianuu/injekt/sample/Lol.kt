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

import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.internal.ReaderImpl
import com.ivianuu.injekt.internal.ReaderLambdaInvocation
import com.ivianuu.injekt.rootComponent
import com.ivianuu.injekt.runReader

abstract class AbstractClass {
    abstract fun func(block: @Reader () -> Unit)
    abstract fun func2(block: @Reader () -> Unit)
}

interface Action {
    @Reader
    fun execute()
}

@ReaderImpl(
    "com.ivianuu.injekt.sample.Action.execute",
    "com.ivianuu.injekt.sample.MyAction.execute"
)
interface ActionExecuteImpl

@Reader
fun caller(action: Action) {
    action.execute()
}

open class MyAction : Action {
    @Reader
    override fun execute() {

    }
}

@ReaderImpl(
    "com.ivianuu.injekt.sample.MyAction.execute",
    "com.ivianuu.injekt.sample.MyOverrideAction.execute"
)
interface ActionExecuteImpl2

class MyOverrideAction : MyAction() {
    @Reader
    override fun execute() {
        super.execute()
    }
}

@ReaderLambdaInvocation(
    "com.ivianuu.injekt.sample.SuperClass.func.block",
    [ApplicationComponent::class]
)
interface invoc1

@ReaderLambdaInvocation(
    "com.ivianuu.injekt.sample.SuperClass.func2.block",
    [ActivityComponent::class]
)
interface invoc2

class SuperClass : AbstractClass() {

    override fun func(block: @Reader () -> Unit) {
        rootComponent<ApplicationComponent>().runReader { block() }
    }

    override fun func2(block: @Reader () -> Unit) {
        rootComponent<ActivityComponent>().runReader { block() }
    }

}

@ReaderLambdaInvocation(
    "com.ivianuu.injekt.sample.user.block",
    [ApplicationComponent::class, ActivityComponent::class]
)
interface invoc3

fun SuperClass.user(block: @Reader () -> Unit) {
    func(block)
    func2(block)
}

@ReaderLambdaInvocation(
    "com.ivianuu.injekt.sample.main",
    [ApplicationComponent::class, ActivityComponent::class]
)
interface invoc4

fun main() {
    SuperClass().user {

    }
}
