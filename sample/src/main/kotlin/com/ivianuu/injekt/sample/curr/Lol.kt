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

package com.ivianuu.injekt.sample.curr

import com.ivianuu.injekt.composition.Readable
import com.ivianuu.injekt.composition.given
import com.ivianuu.injekt.internal.Uninitialized

@Readable
fun getFooAndBar(): Pair<Foo, Bar> =
    getFoo() to getBar()

interface GetFooAndBar_Context : GetFoo_Context, GetBar_Context {
    val foo: Foo
}

class Foo

@Readable
fun getFoo(): Foo =
    get()

interface GetFoo_Context :
    Get_Context<Foo>

@Readable
fun getFoo(context: GetFoo_Context): Foo =
    get(context = context)

class Bar

@Readable
fun getBar(): Bar =
    get()

interface GetBar_Context :
    Get_Context<Bar>

@Readable
fun getBar(context: GetBar_Context): Bar =
    get(context = context)

@Readable
fun <T> get(value: T = given()): T = value

interface Get_Context<T> {
    val value: T
}

@Readable
fun <T> get(value: Any? = Uninitialized, context: Get_Context<T>): T =
    if (value === Uninitialized) context.value else value as T
