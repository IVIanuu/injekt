package com.ivianuu.injekt.internal

@Target(AnnotationTarget.TYPE)
annotation class Qualified<T>

annotation class ComponentDescriptor

annotation class Subcomponent

annotation class ModuleDescriptor

annotation class Binding

annotation class Alias

annotation class Include

annotation class MapDeclaration
annotation class MapEntry
annotation class ClassKey<T>
annotation class IntKey(val value: Int)
annotation class LongKey(val value: Long)
annotation class StringKey(val value: String)

annotation class SetDeclaration
annotation class SetElement
