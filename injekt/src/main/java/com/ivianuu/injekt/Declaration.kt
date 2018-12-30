package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val primaryType: KClass<T>,
    val name: String?,
    val kind: Kind,
    val attributes: Attributes,
    val definition: Definition<T>
) {

    lateinit var instance: Instance<T>
    lateinit var module: Module

    /**
     * Resolves the instance
     */
    fun resolveInstance(
        params: ParamsDefinition?
    ) = instance.get(params)

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "kind:'${primaryType.getFullName()}'"
        return "$kindString[$nameString$typeString $attributes]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> create(
            primaryType: KClass<T>,
            name: String? = null,
            kind: Kind,
            definition: Definition<T>
        ): Declaration<T> {
            val declaration = Declaration(primaryType, name, kind, Attributes(), definition)
            declaration.instance = when (kind) {
                Kind.FACTORY -> FactoryInstance(declaration)
                Kind.SINGLE -> SingleInstance(declaration)
            }
            return declaration
        }

    }
}

const val KEY_CREATE_ON_START = "Declaration.createOnStart"
const val KEY_OVERRIDE = "Declaration.override"

var Declaration<*>.createOnStart: Boolean
    get() = attributes[KEY_CREATE_ON_START] ?: false
    set(value) {
        attributes[KEY_CREATE_ON_START] = value
    }

var Declaration<*>.override: Boolean
    get() = attributes[KEY_OVERRIDE] ?: false
    set(value) {
        attributes[KEY_OVERRIDE] = value
    }

/**
 * Binds this [Declaration] to [type]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(type: KClass<T>) = apply {
    module.declare(
        Declaration.create(
            type,
            null,
            kind,
            definition
        )
    )
}