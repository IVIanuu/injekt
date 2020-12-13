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

/**
fun BroadcastReceiver.createReceiverComponent(
context: Context,
intent: Intent,
receiverComponentFactory:
): ReceiverComponent =
(context.applicationContext as Application).applicationComponent
.get<(BroadcastReceiver, ReceiverContext, ReceiverIntent) -> ReceiverComponent>()(
this, context, intent)

typealias ReceiverComponent = Component<ReceiverComponentKey<*>>

interface ReceiverComponentKey<T> : Component.Key<T>

@Given fun activityComponent(
activity: ComponentActivity = given,
activityRetainedComponent: ActivityRetainedComponent = given,
) = activity.lifecycle.component {
activityRetainedComponent[ActivityComponentFactoryKey](activity)
}

object ReceiverKey : ReceiverComponentKey<BroadcastReceiver>

@Given fun activity(component: ReceiverComponent = given): BroadcastReceiver =
component[ReceiverKey]

object ReceiverComponentFactoryKey :
ApplicationComponentKey<(@Given BroadcastReceiver) -> ReceiverComponent>

@GivenSet fun receiverComponentFactoryKey(
builderFactory: () -> Component.Builder<ReceiverComponentKey<*>> = given,
): ComponentElements<ApplicationComponentKey<*>> =
componentElementsOf(ReceiverComponentFactoryKey) {
builderFactory()
.set(ReceiverKey, it)
.build()
}*/