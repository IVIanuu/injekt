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
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingFactory
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.FactoryFinder
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.annotations.Factory
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.annotations.Raw
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.get
import com.ivianuu.injekt.getProvider
import com.ivianuu.injekt.inject
import kotlin.reflect.KClass

/**
 * Creates [BindingFactory]s via reflection
*/
// todo wip
class ReflectFactoryFinder : FactoryFinder {

private val factories = mutableMapOf<KClass<*>, BindingFactory<*>>()
private val failedTypes = mutableSetOf<KClass<*>>()

override fun <T> find(type: KClass<*>): BindingFactory<T>? {
if (failedTypes.contains(type)) return null
var factory = factories[type]
if (factory == null) {
try {
factory = createFactory(type)
if (factory != null) {
factories[type] = factory
} else {
failedTypes.add(type)
}
} catch (e: Exception) {
failedTypes.add(type)
}
}

return factory as BindingFactory<T>
}

private fun createFactory(type: KClass<*>): BindingFactory<*>? {
val factoryAnnotation: Factory? = type.java.getAnnotation(Factory::class.java)

var kind: Binding.Kind? = null
var name: String? = null
var scope: Scope? = null
var override = false
var eager = false

if (factoryAnnotation != null) {
kind = Binding.Kind.FACTORY

name = if (factoryAnnotation.name.isNotEmpty()) {
factoryAnnotation.name
} else {
null
}

scope = if (factoryAnnotation.scope.isNotEmpty()) {
factoryAnnotation.scope
} else {
null
}
override = factoryAnnotation.override
} else {
val singleAnnotation: Single? = type.java.getAnnotation(Single::class.java)

if (singleAnnotation != null) {
kind = Binding.Kind.SINGLE

name = if (singleAnnotation.name.isNotEmpty()) {
singleAnnotation.name
} else {
null
}

scope = if (singleAnnotation.scope.isNotEmpty()) {
singleAnnotation.scope
} else {
null
}

override = singleAnnotation.override
eager = singleAnnotation.eager
}
}

if (kind == null) {
return null
}

val constructor = type.java.constructors.first()

var paramIndex = 0
val constructorParams: List<ParamDescriptor> = (0 until constructor.parameterTypes.size)
.map { i ->
val parameterType = constructor.parameterTypes[i]
val parameterAnnotations = constructor.parameterAnnotations[i]
val isParam = parameterAnnotations.any { it is Param }
if (!isParam) {
val name = (parameterAnnotations.firstOrNull { it is Name } as? Name)?.name
val isRaw = parameterAnnotations.any { it is Raw }
if (isRaw) {
ParamDescriptor.Dependency(
parameterType.kotlin,
ParamDescriptor.Dependency.Kind.VALUE,
name
)
} else {
val isLazy = parameterType.isAssignableFrom(Lazy::class.java)
val kind = if (isLazy) {
ParamDescriptor.Dependency.Kind.LAZY
} else {
val isProvider = parameterType.isAssignableFrom(Provider::class.java)
if (isProvider) {
ParamDescriptor.Dependency.Kind.PROVIDER
} else {
ParamDescriptor.Dependency.Kind.VALUE
}
}

ParamDescriptor.Dependency(
parameterType.kotlin,
kind,
name
)
}
} else {
val thisIndex = paramIndex
paramIndex++
ParamDescriptor.Parameter(
thisIndex
)
}
}

val definition: Definition<Any> = { params ->
val args = constructorParams
.map { descriptor ->
when (descriptor) {
is ParamDescriptor.Dependency -> {
when (descriptor.kind) {
ParamDescriptor.Dependency.Kind.VALUE -> {
get<Any?>(descriptor.type, descriptor.name)
}
ParamDescriptor.Dependency.Kind.LAZY -> {
inject<Any?>(descriptor.type, descriptor.name)
}
ParamDescriptor.Dependency.Kind.PROVIDER -> {
getProvider<Any?>(descriptor.type, descriptor.name)
}
}
}
is ParamDescriptor.Parameter -> {
params.get<Any?>(descriptor.index)
}
}
}
.toTypedArray()

constructor.newInstance(*args)
}

return object : BindingFactory<Any> {
override fun create(): Binding<Any> {
return Binding.create(
type = type,
name = name,
kind = kind,
scope = scope,
override = override,
eager = eager,
definition = definition
)
}
}
}

sealed class ParamDescriptor {
data class Dependency(
val type: KClass<*>,
val kind: Kind,
val name: String?
) : ParamDescriptor() {
enum class Kind { VALUE, LAZY, PROVIDER }
}

data class Parameter(val index: Int) : ParamDescriptor()
}

}*/