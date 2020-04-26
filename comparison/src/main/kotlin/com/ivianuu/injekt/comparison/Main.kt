/*
 * Copyright 2020 Manuel Wrage<
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

package com.ivianuu.injekt.comparison

import com.ivianuu.injekt.FactoryClass
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.comparison.base.runAllInjectionTests
import com.ivianuu.injekt.comparison.fibonacci.Fib1
import com.ivianuu.injekt.comparison.fibonacci.Fib2
import com.ivianuu.injekt.comparison.fibonacci.Fib3
import com.ivianuu.injekt.comparison.fibonacci.Fib4
import com.ivianuu.injekt.comparison.fibonacci.Fib5
import com.ivianuu.injekt.comparison.fibonacci.Fib6
import com.ivianuu.injekt.comparison.fibonacci.Fib7
import com.ivianuu.injekt.comparison.fibonacci.Fib8
import com.ivianuu.injekt.get
import com.ivianuu.injekt.internal.JitBindingLookup
import com.ivianuu.injekt.internal.JitBindingRegistry
import com.ivianuu.injekt.keyOf

fun main() {
    //Injekt.initializeEndpoint()
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib1Binding
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib2Binding
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib3Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib4Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib4Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib5Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib6Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib7Binding()
        )
    }
    JitBindingRegistry.register(keyOf()) {
        JitBindingLookup(
            scope = FactoryClass,
            binding = Fib8Binding()
        )
    }
    runAllInjectionTests()
}

private object Fib1Binding : LinkedBinding<Fib1>() {
    override fun invoke(parameters: Parameters) = Fib1()
}

private object Fib2Binding : LinkedBinding<Fib2>() {
    override fun invoke(parameters: Parameters) = Fib2()
}

private class Fib3Binding : UnlinkedBinding<Fib3>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib2>,
        private val fibM2: Provider<Fib1>
    ) : LinkedBinding<Fib3>() {
        override fun invoke(parameters: Parameters) = Fib3(fibM1(), fibM2())
    }
}

private class Fib4Binding : UnlinkedBinding<Fib4>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib3>,
        private val fibM2: Provider<Fib2>
    ) : LinkedBinding<Fib4>() {
        override fun invoke(parameters: Parameters) = Fib4(fibM1(), fibM2())
    }
}

private class Fib5Binding : UnlinkedBinding<Fib5>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib4>,
        private val fibM2: Provider<Fib3>
    ) : LinkedBinding<Fib5>() {
        override fun invoke(parameters: Parameters) = Fib5(fibM1(), fibM2())
    }
}

private class Fib6Binding : UnlinkedBinding<Fib6>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib5>,
        private val fibM2: Provider<Fib4>
    ) : LinkedBinding<Fib6>() {
        override fun invoke(parameters: Parameters) = Fib6(fibM1(), fibM2())
    }
}

private class Fib7Binding : UnlinkedBinding<Fib7>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib6>,
        private val fibM2: Provider<Fib5>
    ) : LinkedBinding<Fib7>() {
        override fun invoke(parameters: Parameters) = Fib7(fibM1(), fibM2())
    }
}

private class Fib8Binding : UnlinkedBinding<Fib8>() {
    override fun link(linker: Linker) = Linked(linker.get(), linker.get())
    private class Linked(
        private val fibM1: Provider<Fib7>,
        private val fibM2: Provider<Fib6>
    ) : LinkedBinding<Fib8>() {
        override fun invoke(parameters: Parameters) = Fib8(fibM1(), fibM2())
    }
}
