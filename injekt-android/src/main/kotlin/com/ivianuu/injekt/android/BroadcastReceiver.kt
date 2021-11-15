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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ivianuu.injekt.common.AppComponent
import com.ivianuu.injekt.common.Component
import com.ivianuu.injekt.common.EntryPoint
import com.ivianuu.injekt.common.`as`

/**
 * Returns a new [ReceiverComponent] which must be manually stored and disposed
 */
fun BroadcastReceiver.createReceiverComponent(
  context: Context,
  intent: Intent,
): ReceiverComponent = context.appComponent
  .`as`<AppComponent, ReceiverComponentFactory>()
  .receiverComponent(this, context, intent)

@Component interface ReceiverComponent

@EntryPoint<AppComponent> interface ReceiverComponentFactory {
  fun receiverComponent(
    receiver: BroadcastReceiver,
    context: Context,
    intent: Intent
  ): ReceiverComponent
}
