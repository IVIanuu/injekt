package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency binding.
 */
data class Binding<T>(
    val key: Key,
    val type: KClass<*>,
    val qualifier: Qualifier?,
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
        return "${kind ?: "Unknown"}(" +
                "type=${type.java.name}, " +
                "qualifier=$qualifier, " +
                "scopeName=$scopeName, " +
                ")"
    }

    companion object
}

const val FACTORY_KIND = "Factory"
const val SINGLE_KIND = "Single"

fun <T> Binding.Companion.create(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    kind: String?,
    instanceFactory: InstanceFactory,
    scopeName: String? = null,
    attributes: Attributes = attributesOf(),
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    return Binding(
        Key.of(type, qualifier), type, qualifier, kind, instanceFactory,
        definition, attributes, scopeName, override, eager
    )
}

fun <T> Binding.Companion.createFactory(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    scopeName: String? = null,
    override: Boolean = false,
    definition: Definition<T>
): Binding<T> =
    Binding.create(
        type, qualifier, FACTORY_KIND, FactoryInstanceFactory,
        scopeName, attributesOf(), override, false, definition
    )

fun <T> Binding.Companion.createSingle(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    scopeName: String? = null,
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> =
    Binding.create(
        type, qualifier, SINGLE_KIND, SingleInstanceFactory,
        scopeName, attributesOf(), override, eager, definition
    )

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T