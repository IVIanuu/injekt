/*
 * Copyright 2021 Manuel Wrage
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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.common.AppComponent
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.ComponentElement
import com.ivianuu.injekt.common.ComponentFactory
import com.ivianuu.injekt.common.ComponentName

/**
 * Returns a new [ReceiverComponent] which must be manually stored and disposed
 */
fun BroadcastReceiver.createReceiverComponent(
  context: Context,
  intent: Intent,
): Component<ReceiverComponent> =
  context.appComponent
    .element<@ComponentFactory (BroadcastReceiver, Context, Intent) -> Component<ReceiverComponent>>()
    .invoke(this, context, intent)

object ReceiverComponent : ComponentName {
  @Provide fun factoryElement(
    factory: (BroadcastReceiver, Context, Intent) -> Component<ReceiverComponent>
  ): @ComponentElement<AppComponent> @ComponentFactory (
    BroadcastReceiver,
    Context,
    Intent
  ) -> Component<ReceiverComponent> = factory
}
