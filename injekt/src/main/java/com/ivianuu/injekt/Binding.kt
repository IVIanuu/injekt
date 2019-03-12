package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency binding.
 */
data class Binding<T>(
    val key: Key,
    val type: KClass<*>,
    val qualifier: Qualifier?,
    val kind: Kind,
    val definition: Definition<T>,
    val attributes: Attributes,
    val scope: Scope?,
    val override: Boolean,
    val eager: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Binding<*>) return false

        if (key != other.key) return false
        if (kind != other.kind) return false
        if (attributes != other.attributes) return false
        if (scope != other.scope) return false
        if (override != other.override) return false
        if (eager != other.eager) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + (scope?.hashCode() ?: 0)
        result = 31 * result + override.hashCode()
        result = 31 * result + eager.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind.asString()}(" +
                "type=${type.java.name}, " +
                "qualifier=$qualifier, " +
                "scope=$scope, " +
                ")"
    }

    companion object
}

fun <T> Binding.Companion.create(
    type: KClass<*>,
    qualifier: Qualifier? = null,
    kind: Kind,
    scope: Scope? = null,
    attributes: Attributes = attributesOf(),
    override: Boolean = false,
    eager: Boolean = false,
    definition: Definition<T>
): Binding<T> {
    return Binding(
        Key.of(type, qualifier), type, qualifier, kind,
        definition, attributes, scope, override, eager
    )
}

/**
 * Defines a [Binding]
 */
typealias Definition<T> = DefinitionContext.(parameters: Parameters) -> T