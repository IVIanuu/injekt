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

package com.ivianuu.injekt

import com.ivianuu.injekt.synthetic.Factory

@QualifierMarker
val TestQualifier1 = Qualifier()

@QualifierMarker
val TestQualifier2 = Qualifier()

@QualifierMarker
val TestQualifier3 = Qualifier()

@ScopeMarker
val TestScope1 = Scope()

@ScopeMarker
val TestScope2 = Scope()

@ScopeMarker
val TestScope3 = Scope()

class TestDep1

class TestDep2(val dep1: TestDep1)

class TestDep3(val dep1: TestDep1, val dep2: TestDep2)

@Factory
class CTestDep1

@Factory
class CTestDep2(val dep1: CTestDep1)

@Factory
class CTestDep3(val dep1: CTestDep1, val dep2: CTestDep2)
