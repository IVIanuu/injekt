package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency binding.
 */
data class Binding<T> private constructor(
    val key: Key,
    val type: KClass<*>,
    val name: String?,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val scopeName: String?,
    val override: Boolean,
    val eager: Boolean
) {

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "type:'${type.java.name}'"
        val attributesString = "attributes: '$attributes'"
        val optionsString = "scopeName:$scopeName,override:$override,eager:$eager"
        return "$kindString[$nameString$typeString$attributesString$optionsString]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T> createFactory(
            type: KClass<*>,
            name: String? = null,
            scopeName: String? = null,
            attributes: Attributes = Attributes(),
            override: Boolean = false,
            definition: Definition<T>
        ): Binding<T> =
            create(type, name, Kind.FACTORY, scopeName, attributes, override, false, definition)

        fun <T> createSingle(
            type: KClass<*>,
            name: String? = null,
            scopeName: String? = null,
            attributes: Attributes = Attributes(),
            override: Boolean = false,
            eager: Boolean = false,
            definition: Definition<T>
        ): Binding<T> =
            create(type, name, Kind.SINGLE, scopeName, attributes, override, eager, definition)

        fun <T> create(
            type: KClass<*>,
            name: String? = null,
            kind: Kind,
            scopeName: String? = null,
            attributes: Attributes = Attributes(),
            override: Boolean = false,
            eager: Boolean = false,
            definition: Definition<T>
        ): Binding<T> {
            return Binding(
                Key(type, name), type, name, kind,
                definition, attributes, scopeName, override, eager
            )
        }

    }
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T