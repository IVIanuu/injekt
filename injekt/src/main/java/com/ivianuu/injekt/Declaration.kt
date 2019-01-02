package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val key: Key,
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

    internal fun createInstanceHolder() {
        instance = when (kind) {
            Declaration.Kind.FACTORY -> FactoryInstance(this)
            Declaration.Kind.SINGLE -> SingleInstance(this)
        }
    }

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "type:'${type.java.name}'"
        val optionsString = "override:$override, createOnStart:$createOnStart"
        return "$kindString[$nameString$typeString $attributes$optionsString$attributes]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> create(
            type: KClass<T>,
            name: String? = null,
            kind: Kind,
            definition: Definition<T>
        ): Declaration<T> {
            val declaration = Declaration(Key.of(type, name), type, name)
            declaration.kind = kind
            declaration.definition = definition
            declaration.createInstanceHolder()
            return declaration
        }

    }
}

/**
 * Binds this [Declaration] to [type]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(type: KClass<T>) = apply {
    val newDeclaration = copy(key = Key.of(type, null), type = type as KClass<S>, name = null)
    newDeclaration.kind = kind
    newDeclaration.override = override
    newDeclaration.createOnStart = createOnStart
    newDeclaration.attributes = attributes
    newDeclaration.definition = definition
    newDeclaration.instance = instance
    newDeclaration.module = module
    module.declare(newDeclaration)
}

/**
 * Binds this [Declaration] to [name]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(name: String) = apply {
    val newDeclaration = copy(key = Key.of(type, name), type = type, name = name)
    newDeclaration.kind = kind
    newDeclaration.override = override
    newDeclaration.createOnStart = createOnStart
    newDeclaration.attributes = attributes
    newDeclaration.definition = definition
    newDeclaration.instance = instance
    newDeclaration.module = module
    module.declare(newDeclaration)
}

internal fun <T : Any> Declaration<T>.copyIdentity() = copy().also {
    it.kind = kind
    it.override = override
    it.createOnStart = createOnStart
    it.attributes = attributes
    it.definition = definition
    it.createInstanceHolder()
    it.module = module
}