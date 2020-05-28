/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

object InjektAst {

    annotation class Path {
        annotation class Class(val clazz: KClass<*>)
        annotation class Property(val name: String)
        annotation class TypeParameter(val name: String)
    }

    annotation class ChildFactory

    annotation class Dependency

    // todo move to composition
    annotation class EntryPoints(val values: Array<KClass<*>>)

    annotation class Binding

    annotation class Alias

    annotation class Instance

    // todo move to composition
    annotation class ObjectGraph

    annotation class Module

    annotation class Name(val name: String)

    annotation class Parents(val values: Array<KClass<*>>)

    annotation class Scope

    annotation class Scoped

    annotation class Map {
        annotation class Entry

        annotation class ClassKey(val value: KClass<*>)
        annotation class TypeParameterClassKey(val name: String)
        annotation class IntKey(val value: Int)
        annotation class LongKey(val value: Long)
        annotation class StringKey(val value: String)
    }

    annotation class Set {
        annotation class Element
    }
}
