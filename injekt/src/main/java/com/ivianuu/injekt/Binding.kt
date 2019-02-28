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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false

        if (key != other.key) return false
        if (kind != other.kind) return false
        if (attributes != other.attributes) return false
        if (scopeName != other.scopeName) return false
        if (override != other.override) return false
        if (eager != other.eager) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (kind?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (scopeName?.hashCode() ?: 0)
        result = 31 * result + override.hashCode()
        result = 31 * result + eager.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind ?: "UNKNOWN"}(" +
                "type=${type.java.name}, " +
                "name=$name, " +
                "scopeName=$scopeName, " +
                "attributes=$attributes, " +
                "override=$override, " +
                "eager=$eager, " +
                "instanceFactory=${instanceFactory.javaClass.name}" +
                ")"
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
    attributes: Attributes = attributesOf(),
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
    attributes: Attributes = attributesOf(),
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
    attributes: Attributes = attributesOf(),
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