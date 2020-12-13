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
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElementsOf
import com.ivianuu.injekt.given

object ReceiverScoped : Component.Name

private object ReceiverKey : Component.Key<BroadcastReceiver>
private object ContextKey : Component.Key<Context>
private object IntentKey : Component.Key<Intent>

private object ReceiverComponentFactoryKey :
    Component.Key<(BroadcastReceiver, Context, Intent) -> Component<ReceiverScoped>>

@GivenSet fun receiverComponentFactory(
    builderFactory: () -> Component.Builder<ReceiverScoped> = given,
) = componentElementsOf(ApplicationScoped::class,
    ReceiverComponentFactoryKey) { receiver, context, intent ->
    builderFactory()
        .set(ReceiverKey, receiver)
        .set(ContextKey, context)
        .set(IntentKey, intent)
        .build()
}

fun BroadcastReceiver.createReceiverComponent(
    context: Context,
    intent: Intent,
): Component<ReceiverScoped> = (context.applicationContext as Application)
    .applicationComponent[ReceiverComponentFactoryKey](this, context, intent)

typealias ReceiverContext = Context

@Given val @Given Component<ReceiverScoped>.receiverContext: ReceiverContext
    get() = this[ContextKey]

typealias ReceiverIntent = Intent

@Given val @Given Component<ReceiverScoped>.receiverIntent: ReceiverIntent
    get() = this[IntentKey]
