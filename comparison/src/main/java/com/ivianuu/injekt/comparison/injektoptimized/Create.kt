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
    println(create())
}

fun create(): String = buildString {
    (3..100).forEach {
        append(factoryBlock(it))
        append("\n")
    }
}

private fun factoryBlock(n: Int): String {
    val binding1 = n - 1
    val binding2 = n - 2

    return "factory(binding = object : Binding<Fib$n> {\n" +
            "        private lateinit var fib${binding1}Binding: Binding<Fib$binding1>\n" +
            "        private lateinit var fib${binding2}Binding: Binding<Fib$binding2>\n" +
            "        override fun link(context: DefinitionContext) {\n" +
            "            fib${binding1}Binding = context.getBinding()\n" +
            "            fib${binding2}Binding = context.getBinding()\n" +
            "        }\n" +
            "        override fun get(parameters: ParametersDefinition?) = Fib$n(fib${binding1}Binding(), fib${binding2}Binding())\n" +
            "    })"
}