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

import android.app.*
import android.content.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*

/**
 * Returns a new [ReceiverGivenScope] which must be manually stored and disposed
 */
fun BroadcastReceiver.createReceiverGivenScope(
    context: Context,
    intent: Intent,
): ReceiverGivenScope = (context.applicationContext as Application)
    .appGivenScope
    .element<@ChildScopeFactory (BroadcastReceiver, ReceiverContext, ReceiverIntent) -> ReceiverGivenScope>()
    .invoke(this, context, intent)

typealias ReceiverGivenScope = DefaultGivenScope

@Given
val receiverGivenScopeModule =
    ChildGivenScopeModule3<AppGivenScope, BroadcastReceiver, ReceiverContext, ReceiverIntent, ReceiverGivenScope>()

typealias ReceiverContext = Context

typealias ReceiverIntent = Intent
