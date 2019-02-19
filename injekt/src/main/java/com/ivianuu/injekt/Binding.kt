package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency binding.
 */
data class Binding<T>(
    val key: Key,
    val type: KClass<*>,
    val name: String?,
    val kind: String?,
    val instanceFactory: InstanceFactory,
    val definition: Definition<T>,
    val attributes: Attributes,
    val scopeName: String?,
    val override: Boolean,
    val eager: Boolean
) {

    override fun toString(): String {
        val kindString = kind ?: "UNKNOWN_KIND"
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "type:'${type.java.name}'"
        val attributesString = "attributes: '$attributes'"
        val optionsString = "scopeName:$scopeName,override:$override,eager:$eager"
        return "$kindString[$nameString$typeString$attributesString$optionsString]"
    }

    companion object
}

const val FACTORY_KIND = "FACTORY"
const val SINGLE_KIND = "SINGLE"

fun <T> Binding.Companion.create(
    type: KClass<*>,
    name: String? = null,
    kind: String?,
    instanceFactory: InstanceFactory,
    scopeName: String? = null,
    attributes: Attributes = Attributes(),
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    return Binding(
        Key(type, name), type, name, kind, instanceFactory,
        definition, attributes, scopeName, override, eager
    )
}

fun <T> Binding.Companion.createFactory(
    type: KClass<*>,
    name: String? = null,
    scopeName: String? = null,
    attributes: Attributes = Attributes(),
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> =
    Binding.create(
        type, name, FACTORY_KIND, FactoryInstanceFactory,
        scopeName, attributes, override, false, definition
    )

fun <T> Binding.Companion.createSingle(
    type: KClass<*>,
    name: String? = null,
    scopeName: String? = null,
    attributes: Attributes = Attributes(),
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> =
    Binding.create(
        type, name, SINGLE_KIND, SingleInstanceFactory,
        scopeName, attributes, override, eager, definition
    )

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T