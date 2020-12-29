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
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.Component
import com.ivianuu.injekt.component.componentElement
import com.ivianuu.injekt.component.element
import com.ivianuu.injekt.component.get

typealias ReceiverComponent = Component

@GivenSetElement fun receiverComponentFactory(
    @Given parent: AppComponent,
    @Given builderFactory: () -> Component.Builder<ReceiverComponent>,
) = componentElement<AppComponent, (BroadcastReceiver, ReceiverContext, ReceiverIntent) -> ReceiverComponent> {
        receiver, context, intent ->
    builderFactory()
        .dependency(parent)
        .element(receiver)
        .element(context)
        .element(intent)
        .build()
}

fun BroadcastReceiver.createReceiverComponent(
    context: Context,
    intent: Intent,
): ReceiverComponent = (context.applicationContext as Application)
    .appComponent
    .get<(BroadcastReceiver, ReceiverContext, ReceiverIntent) -> ReceiverComponent>()
    .invoke(this, context, intent)

@Given val @Given ReceiverComponent.appComponentFromReceiver: AppComponent
    get() = get()

typealias ReceiverContext = Context

@Given val @Given ReceiverComponent.receiverContext: ReceiverContext
    get() = get()

typealias ReceiverIntent = Intent

@Given val @Given ReceiverComponent.receiverIntent: ReceiverIntent
    get() = get()

@Given val @Given ReceiverComponent.appComponent: AppComponent
    get() = get()
