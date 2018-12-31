package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val type: KClass<T>,
    val name: String?
) {

    var override: Boolean = false
    var createOnStart: Boolean = false
    var attributes = Attributes()

    lateinit var kind: Kind
    lateinit var definition: Definition<T>
    lateinit var instance: Instance<T>
    lateinit var module: Module

    /**
     * Resolves the instance
     */
    fun resolveInstance(
        params: ParamsDefinition? = null
    ) = instance.get(params)

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "type:'${type.getFullName()}'"
        return "$kindString[$nameString$typeString $attributes]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> create(
            type: KClass<T>,
            name: String? = null,
            kind: Kind,
            definition: Definition<T>
        ): Declaration<T> {
            val declaration = Declaration(type, name)
            declaration.kind = kind
            declaration.definition = definition
            declaration.instance = when (kind) {
                Kind.FACTORY -> FactoryInstance(declaration)
                Kind.SINGLE -> SingleInstance(declaration)
            }
            return declaration
        }

    }
}

/**
 * Binds this [Declaration] to [type]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(type: KClass<T>) = apply {
    val newDeclaration = copy(type = type as KClass<S>, name = null)
    newDeclaration.module = module
    newDeclaration.instance = instance
    module.declare(newDeclaration)
}

internal fun <T : Any> Declaration<T>.copyIdentity() = copy().also {
    it.kind = kind
    it.override = override
    it.createOnStart = createOnStart
    it.attributes = attributes
    it.definition = definition
    it.instance = when (kind) {
        Declaration.Kind.FACTORY -> FactoryInstance(this)
        Declaration.Kind.SINGLE -> SingleInstance(this)
    }
    it.module = module
}