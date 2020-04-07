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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeMarker
import com.ivianuu.injekt.synthetic.Factory

interface Command

object Command1 : Command

object Command2 : Command

object Command3 : Command

@QualifierMarker
val TestQualifier1 = Qualifier("TestQualifier1")

@QualifierMarker
val TestQualifier2 = Qualifier("TestQualifier2")

@QualifierMarker
val TestQualifier3 = Qualifier("TestQualifier3")

@ScopeMarker
val TestScope1 = Scope("TestScope1")

@ScopeMarker
val TestScope2 = Scope("TestScope2")

@ScopeMarker
val TestScope3 = Scope("TestScope3")

@Factory
class TestDep1

@Factory
class TestDep2(val dep1: TestDep1)

@Factory
class TestDep3(val dep1: TestDep1, val dep2: TestDep2)
