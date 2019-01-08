package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency definition.
 */
data class BeanDefinition<T : Any> private constructor(
    val key: Key,
    val type: KClass<T>,
    val name: String?,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val scopeId: String?,
    val override: Boolean,
    val eager: Boolean
) {

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "type:'${type.java.name}'"
        val attributesString = "attributes: '$attributes'"
        val optionsString = "scopeId:$scopeId,override:$override,eager:$eager"
        return "$kindString[$nameString$typeString$attributesString$optionsString]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> createFactory(
            type: KClass<T>,
            name: String? = null,
            scopeId: String? = null,
            override: Boolean = false,
            eager: Boolean = false,
            definition: Definition<T>
        ): BeanDefinition<T> =
            create(type, name, Kind.FACTORY, scopeId, override, eager, definition)

        fun <T : Any> createSingle(
            type: KClass<T>,
            name: String? = null,
            scopeId: String? = null,
            override: Boolean = false,
            eager: Boolean = false,
            definition: Definition<T>
        ): BeanDefinition<T> = create(type, name, Kind.SINGLE, scopeId, override, eager, definition)

        fun <T : Any> create(
            type: KClass<T>,
            name: String? = null,
            kind: Kind,
            scopeId: String? = null,
            override: Boolean = false,
            eager: Boolean = false,
            definition: Definition<T>
        ): BeanDefinition<T> = BeanDefinition(
            Key(type, name), type, name, kind,
            definition, Attributes(), scopeId, override, eager
        )

    }
}

/**
 * Defines a [BeanDefinition]
 */
typealias Definition<T> = Component.(parameters: Parameters) -> T