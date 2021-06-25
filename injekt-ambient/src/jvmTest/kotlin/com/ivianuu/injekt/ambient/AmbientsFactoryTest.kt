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

package com.ivianuu.injekt.ambient

import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.*
import org.junit.*

class AmbientsFactoryTest {
  @Test fun testNamedProvidedValue() {
    @Provide val ambient = ambientOf { 0 }
    @Provide val providedInt: NamedProvidedValue<ForApp, Int> = provide(42)
    @Provide val ambients = ambientsOf<ForApp>(ambientsOf())
    current<Int>() shouldBe 42
  }

  @Test fun testAmbientsFactoryModule() {
    @Provide val ambient = ambientOf { 0 }
    @Provide val childAmbientsFactoryModule = AmbientsFactoryModule0<ForApp, ForChild>()
    @Provide val providedInt: NamedProvidedValue<ForApp, Int> = provide(42)
    @Provide val parentAmbients = ambientsOf<ForApp>(ambientsOf())
    withInstances(ambientsFromFactoryOf<ForChild>()) {
      current<Int>() shouldBe 42
    }
  }

  @Test fun testAmbientServiceCanAccessAmbients() {
    @Provide val intAmbient = ambientOf { 0 }

    @Provide @AmbientService<ForApp> class Foo(val ambients: Ambients) {
      val answer = current<Int>()
    }

    @Provide val providedInt: NamedProvidedValue<ForApp, Int> = provide(42)

    @Provide val ambients = ambientsOf<ForApp>(ambientsOf())
    val foo = current<Foo>()
    ambients shouldBe foo.ambients
    foo.answer shouldBe 42
  }

  private abstract class ForChild private constructor()

  @Test fun testScopeObserver() {
    var initCalls = 0
    var disposeCalls = 0
    @Provide val observer = object : ScopeObserver<ForApp> {
      override fun onInit() {
        initCalls++
      }

      override fun onDispose() {
        disposeCalls++
      }
    }

    @Provide val ambients = inject<AmbientsFactory<ForApp>>().create(ambientsOf())

    initCalls shouldBe 1
    disposeCalls shouldBe 0

    val scope = current<Scope>()

    (scope as DisposableScope).dispose()

    initCalls shouldBe 1
    disposeCalls shouldBe 1
  }
}
