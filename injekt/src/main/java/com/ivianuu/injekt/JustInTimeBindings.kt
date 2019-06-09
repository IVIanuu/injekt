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

import kotlin.reflect.KClass

data class JustInTimeLookup<T>(
    val binding: Binding<T>,
    val scope: KClass<out Annotation>?
)

interface JustInTimeLookupFactory {
    fun <T> create(key: Key): JustInTimeLookup<T>?
}

internal object DefaultJustInTimeLookupFactory : JustInTimeLookupFactory {
    override fun <T> create(key: Key): JustInTimeLookup<T>? =
        CodegenJustInTimeLookupFactory.create(key)
            ?: ReflectiveJustInTimeLookupFactory.create(key)
}


internal object CodegenJustInTimeLookupFactory : JustInTimeLookupFactory {

    private val lookups = hashMapOf<Key, JustInTimeLookup<*>>()

    override fun <T> create(key: Key): JustInTimeLookup<T>? {
        if (key.name != null) return null

        var lookup = lookups[key]

        if (lookup == null) {
            lookup = findLookup(key.type.rawJava)
            if (lookup != null) lookups[key] = lookup
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

internal object ReflectiveJustInTimeLookupFactory : JustInTimeLookupFactory {
    override fun <T> create(key: Key): JustInTimeLookup<T>? = null // todo implement
}

/*
internal class UnlinkedJustInTimeBinding<T>(private val type: Type<T>) : UnlinkedBinding<T>() {

    override fun link(linker: Linker): LinkedBinding<T> {
        val constructor = type.rawJava.constructors.first()
        val parameterTypes = constructor.genericParameterTypes
        val parameterAnnotations = constructor.parameterAnnotations

        val bindings = arrayOfNulls<LinkedBinding<*>>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val key = Key.of(findQualifier(parameterAnnotations[i]), parameterTypes[i])
            bindings[i] = linker.get<*>(key)
        }

        val membersInjector = ReflectiveMembersInjector.create(cls, scope)

        return LinkedJustInTimeBinding(constructor, bindings, membersInjector)
    }

}*/