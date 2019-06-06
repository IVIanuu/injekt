/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.comparison.injektoptimized

fun main() {
    println(createModule(100))
}

fun createFibBindings(): String = buildString {
    (3..100).forEach {
        append(binding(it))
        append("\n")
    }
}

private fun binding(n: Int): String {
    val b1 = n - 1
    val b2 = n - 2

    return "class Fib${n}Binding : Binding<Fib${n}>() {\n" +
            "    private lateinit var fib${b1}Binding: Binding<Fib${b1}>\n" +
            "    private lateinit var fib${b2}Binding: Binding<Fib${b2}>\n" +
            "    override fun attach(component: Component) {\n" +
            "        fib${b1}Binding = component.getBinding()\n" +
            "        fib${b2}Binding = component.getBinding()\n" +
            "    }\n" +
            "\n" +
            "    override fun get(parameters: ParametersDefinition?) = Fib${n}(fib${b1}Binding(), fib${b2}Binding())\n" +
            "}"
}

private fun createModule(n: Int): String {
    return buildString {
        append("fun createModule() = module {")
        for (i in 1..n) {
            append("\n")
            append("bind(Fib${i}Binding())")
        }
        append("\n}")
    }
}