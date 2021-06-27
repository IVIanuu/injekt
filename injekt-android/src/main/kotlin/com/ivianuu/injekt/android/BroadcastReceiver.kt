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

import android.app.*
import android.content.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.container.*

/**
 * Returns a new [Container] for [ReceiverScope] which must be manually stored and disposed
 */
fun BroadcastReceiver.createReceiverContainer(
  context: Context,
  intent: Intent,
): Container<ReceiverScope> = (context.applicationContext as Application)
  .appContainer
  .element<@ChildContainerFactory (
    BroadcastReceiver,
    ReceiverContext,
    ReceiverIntent
  ) -> Container<ReceiverScope>>()
  .invoke(this, context, intent)


abstract class ReceiverScope private constructor()

@Provide val receiverContainerModule = ChildContainerModule3<AppScope,
    BroadcastReceiver, ReceiverContext, ReceiverIntent, ReceiverScope>()

typealias ReceiverContext = Context

typealias ReceiverIntent = Intent
