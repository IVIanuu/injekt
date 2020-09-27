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

package com.ivianuu.injekt.test

import com.ivianuu.injekt.Context

class Foo

class Bar(val foo: Foo)

class Baz(val bar: Bar, val foo: Foo)

interface TestContext : Context

interface TestContext2 : Context

interface TestParentContext : Context

interface TestParentContext2 : Context

interface TestChildContext : Context

interface TestChildContext2 : Context

interface Command

class CommandA : Command

class CommandB : Command

class CommandC : Command
