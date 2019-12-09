/*
 * Copyright 2019 Manuel Wrage
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

import junit.framework.Assert.assertTrue
import org.junit.Test

class ProxyDep

object ProxyDep__Binding : LinkedBinding<ProxyDep>(), HasScope, IsSingle {
    override val scope: Any
        get() = TestScopeOne.Companion
    override fun invoke(parameters: ParametersDefinition?): ProxyDep = ProxyDep()
}

class ProxyTest {

    @Test
    fun testBridgingDoesNotModifyOriginalBindingState() {
        val module = Module {
            single(override = true, eager = true) { "value" }
            withBinding<String> { bindType<CharSequence>() }
        }

        val component = Component { modules(module) }

        val original = component.getBinding<String>(keyOf<String>())
        val proxy = component.getBinding<CharSequence>(keyOf<CharSequence>())

        assertTrue(original === proxy)
        assertTrue(original.scoped)
        assertTrue(original.override)
    }

    @Test
    fun testBridgingDoesNotModifyOriginalBindingStateOfLinkedJustInTimeBindings() {
        val component = Component {
            scopes(TestScopeOne)
            modules(
                Module {
                    withBinding<ProxyDep> {
                        bindType<Any>()
                    }
                }
            )
        }

        val originalBinding = component.getBinding<ProxyDep>(keyOf<ProxyDep>())
        assertTrue(originalBinding.scoped)
        component.getBinding<Any>(keyOf<Any>())
        assertTrue(originalBinding.scoped)
    }
}
