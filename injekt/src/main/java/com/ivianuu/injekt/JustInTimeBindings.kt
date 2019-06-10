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

package com.ivianuu.injekt

import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

data class JustInTimeLookup<T>(
    val binding: Binding<T>,
    val scope: KClass<out Annotation>?
)

interface JustInTimeLookupFactory {
    fun <T> create(key: Key): JustInTimeLookup<T>?
}

object DefaultJustInTimeLookupFactory : JustInTimeLookupFactory {
    override fun <T> create(key: Key): JustInTimeLookup<T>? =
        CodegenJustInTimeLookupFactory.create(key)
            ?: ReflectiveJustInTimeLookupFactory.create(key)
}


object CodegenJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = ConcurrentHashMap<Type<*>, JustInTimeLookup<*>>()

    override fun <T> create(key: Key): JustInTimeLookup<T>? {
        if (key.name != null) return null
        val type = key.type

        var lookup = lookups[type]

        if (lookup == null) {
            lookup = findLookup(type.rawJava)
            if (lookup != null) lookups[type] = lookup
        }

        return lookup as? JustInTimeLookup<T>
    }

    private fun findLookup(type: Class<*>) = try {
        val bindingClass = Class.forName(type.name + "__Binding")
        // get the INSTANCE field
        val binding = bindingClass.declaredFields.first().get(null) as Binding<*>
        JustInTimeLookup(binding, (binding as? HasScope)?.scope)
    } catch (e: Exception) {
        null
    }
}

object ReflectiveJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = ConcurrentHashMap<Type<*>, JustInTimeLookup<*>>()

    override fun <T> create(key: Key): JustInTimeLookup<T>? {
        if (key.name != null) return null
        val type = key.type

        var lookup = lookups[type]

        if (lookup == null) {
            lookup = findLookup(type.rawJava)
            if (lookup != null) lookups[type] = lookup
        }

        return lookup as? JustInTimeLookup<T>
    }

    private fun findLookup(type: Class<*>) = try {
        val constructor = if (type.isAnnotationPresent(Inject::class.java)) {
            type.constructors.first()
        } else {
            type.constructors
                .firstOrNull { it.isAnnotationPresent(Inject::class.java) }
                ?: type.constructors.first()
        }

        val scope = type.annotations.firstOrNull { annotation ->
            annotation.annotationClass.java.annotations.any { annotatedAnnotation ->
                annotatedAnnotation.annotationClass == Scope::class
            }
        }?.annotationClass

        JustInTimeLookup(
            UnlinkedJustInTimeBinding(constructor),
            scope
        )
    } catch (e: Exception) {
        null
    }
}

private class UnlinkedJustInTimeBinding<T>(
    private val constructor: Constructor<T>
) : UnlinkedBinding<T>() {

    override fun link(linker: Linker): LinkedBinding<T> {
        val parameterTypes = constructor.genericParameterTypes
        val parameterAnnotations = constructor.parameterAnnotations

        val args =
            arrayOfNulls<LinkedJustInTimeBinding.Arg>(parameterTypes.size)
        var currentParamIndex = -1
        for (i in parameterTypes.indices) {
            val thisAnnotations = parameterAnnotations[i]

            if (thisAnnotations.any { it is Param }) {
                ++currentParamIndex
                args[i] = LinkedJustInTimeBinding.Arg.Parameter(currentParamIndex)
            } else {
                val type = typeOf<Any?>(parameterTypes[i])
                val nameAnnotation = parameterAnnotations[i]
                    .mapNotNull { annotation ->
                        annotation.annotationClass.java.declaredAnnotations.firstOrNull { annotatedAnnotation ->
                            annotatedAnnotation.annotationClass == Name::class
                        }
                    }
                    .firstOrNull()

                val name = (nameAnnotation as? Name)
                    ?.name?.java?.declaredFields
                    ?.last()
                    ?.also { it.isAccessible = true }
                    ?.get(null)

                val key = keyOf(type, name)
                args[i] = LinkedJustInTimeBinding.Arg.Dependency(linker.get<Any?>(key))
            }
        }

        return LinkedJustInTimeBinding(constructor, args as Array<LinkedJustInTimeBinding.Arg>)
    }

}

private class LinkedJustInTimeBinding<T>(
    private val constructor: Constructor<T>,
    private val args: Array<Arg>
) : LinkedBinding<T>() {
    override fun get(parameters: ParametersDefinition?): T {
        val initializedParameters by lazy(LazyThreadSafetyMode.NONE) { parameters!!.invoke() }
        val resolvedArgs = args
            .map { arg ->
                when (arg) {
                    is Arg.Dependency -> arg.binding.get()
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