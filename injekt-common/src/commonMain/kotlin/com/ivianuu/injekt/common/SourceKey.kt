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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Inject
import kotlin.jvm.JvmInline

/**
 * A key which is unique for each root call site
 */
@JvmInline value class SourceKey(val value: String)

/**
 * Returns the [SourceKey] at this call site
 */
@Suppress("NOTHING_TO_INLINE")
inline fun sourceKey(@Inject key: SourceKey): SourceKey = key
