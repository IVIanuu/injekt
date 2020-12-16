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
import android.content.Intent
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSetElement
import com.ivianuu.injekt.component.ApplicationScoped
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.ComponentKey
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.get
import com.ivianuu.injekt.component.getDependency

@Given object ReceiverScoped : Component.Name

private val ReceiverKey = ComponentKey<BroadcastReceiver>()
private val ContextKey = ComponentKey<Context>()
private val IntentKey = ComponentKey<Intent>()

private val ReceiverComponentFactoryKey =
    ComponentKey<(BroadcastReceiver, Context, Intent) -> Component<ReceiverScoped>>()

@GivenSetElement fun receiverComponentFactory(
    @Given parent: Component<ApplicationScoped>,
    @Given builderFactory: () -> Component.Builder<ReceiverScoped>,
) = componentElement(ApplicationScoped,
    ReceiverComponentFactoryKey) { receiver, context, intent ->
    builderFactory()
        .dependency(parent)
        .element(ReceiverKey, receiver)
        .element(ContextKey, context)
        .element(IntentKey, intent)
        .build()
}

fun BroadcastReceiver.createReceiverComponent(
    context: Context,
    intent: Intent,
): Component<ReceiverScoped> = (context.applicationContext as Application)
    .applicationComponent[ReceiverComponentFactoryKey](this, context, intent)

@Given val @Given Component<ReceiverScoped>.applicationComponentFromReceiver: Component<ApplicationScoped>
    get() = getDependency(ApplicationScoped)

typealias ReceiverContext = Context

@Given val @Given Component<ReceiverScoped>.receiverContext: ReceiverContext
    get() = this[ContextKey]

typealias ReceiverIntent = Intent

@Given val @Given Component<ReceiverScoped>.receiverIntent: ReceiverIntent
    get() = this[IntentKey]

@Given val @Given Component<ReceiverScoped>.applicationComponent: Component<ApplicationScoped>
    get() = getDependency(ApplicationScoped)
