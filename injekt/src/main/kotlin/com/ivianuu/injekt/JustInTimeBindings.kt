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

import java.lang.reflect.Constructor

data class JustInTimeLookup<T>(
    val binding: Binding<T>,
    val scope: Any?,
    val isSingle: Boolean
)

interface JustInTimeLookupFactory {
    fun <T> findBindingForKey(key: Key): JustInTimeLookup<T>?
}

object DefaultJustInTimeLookupFactory : JustInTimeLookupFactory {
    override fun <T> findBindingForKey(key: Key): JustInTimeLookup<T>? =
        CodegenJustInTimeLookupFactory.findBindingForKey(key)
            ?: ReflectiveJustInTimeLookupFactory.findBindingForKey(key)
}

object CodegenJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = mutableMapOf<Type<*>, JustInTimeLookup<*>>()

    override fun <T> findBindingForKey(key: Key): JustInTimeLookup<T>? {
        if (key.name != null) return null
        val type = key.type

        var lookup = synchronized(lookups) { lookups[type] }

        if (lookup == null) {
            lookup = findLookup(type.rawJava)
            if (lookup != null) {
                synchronized(lookups) {
                    lookups[type] = lookup
                }
            }
        }

        return lookup as? JustInTimeLookup<T>
    }

    private fun findLookup(type: Class<*>) = try {
        val bindingClass = Class.forName(type.name + "__Binding")
        val binding = bindingClass.declaredFields
            .first { it.type == bindingClass }
            .also { it.isAccessible = true }
            .get(null) as Binding<*>
        JustInTimeLookup(
            binding = binding,
            scope = (binding as? HasScope)?.scope,
            isSingle = binding is IsSingle
        )
    } catch (e: Exception) {
        null
    }
}

object ReflectiveJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = mutableMapOf<Type<*>, JustInTimeLookup<*>>()

    override fun <T> findBindingForKey(key: Key): JustInTimeLookup<T>? {
        if (key.name != null) return null
        val type = key.type

        var lookup = synchronized(lookups) { lookups[type] }

        if (lookup == null) {
            lookup = findLookup(type.rawJava)
            if (lookup != null) {
                synchronized(lookups) {
                    lookups[type] = lookup
                }
            }
        }

        InjektPlugins.logger?.warn("Used reflection to create binding for key: $key")

        return lookup as? JustInTimeLookup<T>
    }

    private fun findLookup(type: Class<*>): JustInTimeLookup<out Any>? {
        return try {
            val isFactory = type.isAnnotationPresent(Factory::class.java)
            val isSingle = type.isAnnotationPresent(Single::class.java)
            if (!isFactory && !isSingle) return null

            val constructor = type.constructors
                .firstOrNull { it.isAnnotationPresent(InjektConstructor::class.java) }
                ?: type.constructors.first()

            val scopeAnnotation = type
                .annotations
                .mapNotNull { annotation ->
                    if (annotation.annotationClass.java.declaredAnnotations.any {
                            it.annotationClass == Scope::class
                        }) annotation else null
                }
                .firstOrNull()

            val scope = scopeAnnotation
                ?.annotationClass
                ?.java
                ?.declaredClasses
                ?.firstOrNull()
                ?.declaredFields
                ?.first { it.type == it.declaringClass }
                ?.also { it.isAccessible = true }
                ?.get(null)

            JustInTimeLookup(
                binding = UnlinkedReflectiveBinding(constructor),
                scope = scope,
                isSingle = isSingle
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private class UnlinkedReflectiveBinding<T>(
    private val constructor: Constructor<T>
) : UnlinkedBinding<T>() {

    override fun link(linker: Linker): LinkedBinding<T> {
        val parameterTypes = constructor.genericParameterTypes
        val parameterAnnotations = constructor.parameterAnnotations

        val args =
            arrayOfNulls<LinkedReflectiveBinding.Arg>(parameterTypes.size)
        var currentParamIndex = -1
        for (i in parameterTypes.indices) {
            val thisAnnotations = parameterAnnotations[i]

            if (thisAnnotations.any { it is Param }) {
                ++currentParamIndex
                args[i] = LinkedReflectiveBinding.Arg.Parameter(currentParamIndex)
            } else {
                val type = typeOf<Any?>(parameterTypes[i])
                val nameAnnotation = parameterAnnotations[i]
                    .mapNotNull { annotation ->
                        if (annotation.annotationClass.java.declaredAnnotations.any {
                                it.annotationClass == Name::class
                            }) annotation else null
                    }
                    .firstOrNull()

                val name = nameAnnotation
                    ?.annotationClass
                    ?.java
                    ?.declaredClasses
                    ?.firstOrNull()
                    ?.declaredFields
                    ?.first { it.type == it.declaringClass }
                    ?.also { it.isAccessible = true }
                    ?.get(null)

                val key = keyOf(type, name)
                args[i] = LinkedReflectiveBinding.Arg.Dependency(linker.get<Any?>(key))
            }
        }

        return LinkedReflectiveBinding(constructor, args as Array<LinkedReflectiveBinding.Arg>)
    }
}

private class LinkedReflectiveBinding<T>(
    private val constructor: Constructor<T>,
    private val args: Array<Arg>
) : LinkedBinding<T>() {
    override fun invoke(parameters: ParametersDefinition?): T {
        val initializedParameters by lazy(LazyThreadSafetyMode.NONE) {
            parameters?.invoke() ?: error("Missing required parameters")
        }
        val resolvedArgs = args
            .map { arg ->
                when (arg) {
                    is Arg.Dependency -> arg.binding()
                    is Arg.Parameter -> initializedParameters.get<Any?>(arg.index)
                }
            }
            .toTypedArray()

        return constructor.newInstance(*resolvedArgs)
    }

    sealed class Arg {
        data class Dependency(val binding: LinkedBinding<*>) : Arg()
        data class Parameter(val index: Int) : Arg()
    }
}
