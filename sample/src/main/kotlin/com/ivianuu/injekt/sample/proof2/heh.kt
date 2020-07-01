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

package com.ivianuu.injekt.sample.proof2

import com.ivianuu.injekt.composition.Readable
import com.ivianuu.injekt.composition.given
import com.ivianuu.injekt.internal.Uninitialized
import com.ivianuu.injekt.sample.Logger

@Readable
fun getFooAndBar(): Pair<Foo, Bar> {
    return getFoo() to getBar()
}

interface GetFooAndBar_Context : GetFoo_Context, GetBar_Context {
    val foo: Foo
}

fun getFooAndBar(context: GetFooAndBar_Context): Pair<Foo, Bar> {
    return getFoo(context = context) to getBar(context = context)
}

class Foo

@Readable
fun getFoo(): Foo {
    return get()
}

interface GetFoo_Context : log_Context {
    val fooValue: Foo
}

@Readable
fun getFoo(context: GetFoo_Context): Foo {
    return get(context = object : Get_Context<Foo> {
        override val logger: Logger
            get() = context.logger
        override val value: Foo
            get() = context.fooValue
    })
}

class Bar

@Readable
fun getBar(): Bar {
    return get()
}

interface GetBar_Context : log_Context {
    val barValue: Bar
}

@Readable
fun getBar(context: GetBar_Context): Bar {
    return get(context = object : Get_Context<Bar> {
        override val logger: Logger
            get() = context.logger
        override val value: Bar
            get() = context.barValue
    })
}

@Readable
fun <T> get(value: T = given()): T {
    log("get called")
    return value
}

interface Get_Context<T> : log_Context {
    val value: T
}

@Readable
fun <T> get(value: Any? = Uninitialized, context: Get_Context<T>): T {
    log("get called", context = context)
    return if (value === Uninitialized) context.value else value as T
}

@Readable
fun log(msg: String, logger: Logger = given()) {
    logger.log(msg)
}

interface log_Context {
    val logger: Logger
}

@Readable
fun log(msg: String, logger: Any? = Uninitialized, context: log_Context) {
    (if (logger === Uninitialized) context.logger else logger as Logger)
        .log(msg)
}
