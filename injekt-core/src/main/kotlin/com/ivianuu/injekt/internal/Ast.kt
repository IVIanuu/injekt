package com.ivianuu.injekt.internal

import kotlin.reflect.KClass

object InjektAst {

    annotation class Path {
        annotation class Class(val clazz: KClass<*>)
        annotation class Property(val name: String)
        annotation class TypeParameter(val name: String)
        annotation class ValueParameter(val name: String)
    }

    annotation class ChildFactory

    annotation class Dependency

    annotation class Binding

    annotation class ImplFactory

    annotation class InstanceFactory

    annotation class Alias

    annotation class Inline

    annotation class Module

    annotation class Scope

    annotation class Scoped

    annotation class Typed

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
