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

package com.ivianuu.injekt.android

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.composition.CompositionComponent
import com.ivianuu.injekt.composition.CompositionFactory
import com.ivianuu.injekt.composition.get
import com.ivianuu.injekt.composition.parent
import com.ivianuu.injekt.create
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.scope

@Scope
annotation class ReceiverScoped

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Qualifier
annotation class ForReceiver

@CompositionComponent
interface ReceiverComponent

fun BroadcastReceiver.newReceiverComponent(
    context: Context
): ReceiverComponent {
    return (context.applicationContext as Application).applicationComponent
        .get<@ChildFactory (BroadcastReceiver) -> ReceiverComponent>()(this)
}

@CompositionFactory
fun createReceiverComponent(instance: BroadcastReceiver): ReceiverComponent {
    parent<ApplicationComponent>()
    scope<ServiceScoped>()
    instance(instance)
    return create()
}
