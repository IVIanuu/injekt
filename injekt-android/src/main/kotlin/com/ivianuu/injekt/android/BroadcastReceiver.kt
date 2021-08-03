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

import android.content.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns a new [ReceiverScope] which must be manually stored and disposed
 */
fun BroadcastReceiver.createReceiverScope(
  context: Context,
  intent: Intent,
): ReceiverScope = context
  .appScope
  .element<@ChildScopeFactory (
    BroadcastReceiver,
    ReceiverContext,
    ReceiverIntent
  ) -> ReceiverScope>()
  .invoke(this, context, intent)


typealias ReceiverScope = Scope

@Provide val receiverScopeModule = ChildScopeModule3<AppScope,
    BroadcastReceiver, ReceiverContext, ReceiverIntent, ReceiverScope>()

typealias ReceiverContext = Context

typealias ReceiverIntent = Intent
