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

package com.ivianuu.injekt.sample.test

/**
import com.ivianuu.injekt.Attributes
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingContext
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.android.APPLICATION_SCOPE
import com.ivianuu.injekt.get
import com.ivianuu.injekt.module
import com.ivianuu.injekt.sample.AppDependency
import kotlin.reflect.KClass

class BindingBuilder<T>(
val type: KClass<*>,
val name: String? = null
) {

var kind: Binding.Kind? = null
var definition: Definition<T>? = null
val attributes = Attributes()
var scope: Scope? = null
var override = false
var eager = false

fun build(): Binding<T> {
return Binding.create(
type,
name,
kind ?: error("kind must be specified"),
scope,
attributes,
override,
eager,
definition ?: error("definition must be specified")
)
}

}

fun <T> BindingBuilder<T>.factory(definition: Definition<T>) {

}

fun <T> BindingBuilder<T>.single(definition: Definition<T>) {

}

fun <T> BindingBuilder<T>.definition(definition: Definition<T>) {

}

fun BindingBuilder<*>.kind(kind: Binding.Kind) {

}

fun BindingBuilder<*>.override(override: Boolean = true) {

}

fun BindingBuilder<*>.eager(eager: Boolean = true) {

}

fun BindingBuilder<*>.scope(scope: Scope?) {

}

fun BindingBuilder<*>.attributes(attrs: Iterable<Pair<String, Any>>) {

}

fun BindingBuilder<*>.attributes(vararg attrs: Pair<String, Any>) {

}

fun BindingBuilder<*>.attribute(key: String, value: Any) {

}

inline fun <reified T> BindingBuilder<*>.bindType() {

}

fun BindingBuilder<*>.bindType(type: KClass<*>) {

}

fun BindingBuilder<*>.bindTypes(vararg types: KClass<*>) {

}

fun BindingBuilder<*>.bindTypes(vararg types: Array<KClass<*>>) {

}

fun BindingBuilder<*>.bindTypes(types: Iterable<KClass<*>>) {

}

fun BindingBuilder<*>.bindQualifier(name: String) {

}

fun BindingBuilder<*>.bindQualifiers(vararg names: String) {

}

fun BindingBuilder<*>.bindQualifiers(vararg names: Array<String>) {

}

fun BindingBuilder<*>.bindQualifiers(names: Iterable<String>) {

}

fun BindingBuilder<*>.bindIntoMap(mapName: String, mapKey: Any, override: Boolean = false) {

}

fun BindingBuilder<*>.bindIntoSet(setName: String, override: Boolean = false) {

}

inline fun <reified T> Module.bind(qualifier: Qualifier? = null, body: BindingBuilder<T>.() -> Unit): BindingContext<T> {
return add(BindingBuilder<T>(T::class, name).apply(body).build())
}

inline fun <reified T> Module.bindSingle(qualifier: Qualifier? = null, body: BindingBuilder<T>.() -> Unit): BindingContext<T> {
return bind(name) {
kind(Binding.Kind.SINGLE)
body()
}
}

/**
 * Provides scoped dependency which will be created once for each component
*/
inline fun <reified T> Module.single2(
qualifier: Qualifier? = null,
scope: Scope? = null,
override: Boolean = false,
eager: Boolean = false,
noinline definition: Definition<T>
): BindingContext<T> {
return bind(name) {
single(definition)
scope(scope)
override(override)
eager(eager)
}
}

//
// USAGE
//

val testModule = module {
single2 { "my_string" }

bind<String> {
factory { "my_string" }
override()
attributes("key" to "value")
bindQualifier("alias")
bindIntoMap("map_name", Any::class)
bindType<CharSequence>()
}

bind<AppDependency>("my_name") {
single { AppDependency(get()) }
scope(APPLICATION_SCOPE)
override()
eager()
bindIntoSet("my_set")
bindTypes(Any::class)
}

bind<Int> {
single { 0 }
bindQualifiers("name_one")
attribute("my_key", "my_value")
}
}*/