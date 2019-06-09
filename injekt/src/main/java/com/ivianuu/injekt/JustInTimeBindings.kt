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

    private val lookups = hashMapOf<Type<*>, JustInTimeLookup<*>>()

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
        val binding = bindingClass.declaredFields.last().get(null) as Binding<*>
        JustInTimeLookup(binding, (binding as? HasScope)?.scope)
    } catch (e: Exception) {
        null
    }
}

object ReflectiveJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = mutableMapOf<Type<*>, JustInTimeLookup<*>>()

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
        val constructor = type.constructors.first()
        // todo consider multiple constructors
        val scope = type.annotations.firstOrNull { annotation ->
            annotation.javaClass.annotations.any { annotatedAnnotation ->
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

        val bindings = arrayOfNulls<LinkedBinding<*>>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val type = typeOf<Any?>(parameterTypes[i])
            val name = parameterAnnotations[i].firstOrNull { annotation ->
                annotation.javaClass.annotations.any { annotatedAnnotation ->
                    annotatedAnnotation.annotationClass == Name::class
                }
            }?.annotationClass

            val key = keyOf(type, name)
            bindings[i] = linker.get<Any?>(key)
        }

        return LinkedJustInTimeBinding(constructor, bindings as Array<LinkedBinding<*>>)
    }

}

private class LinkedJustInTimeBinding<T>(
    private val constructor: Constructor<T>,
    private val bindings: Array<LinkedBinding<*>>
) : LinkedBinding<T>() {
    override fun get(parameters: ParametersDefinition?): T =
        constructor.newInstance(*bindings.map { it() }.toTypedArray())
}