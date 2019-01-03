package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val type: KClass<T>,
    val name: String?
) {

    /**
     * Whether or not this declarations can override existing declarations
     */
    var override: Boolean = false

    /**
     * Whether or not this declaration should be created on start
     */
    var createOnStart: Boolean = false

    /**
     * Extras
     */
    var attributes = Attributes()

    /**
     * The kind of this declaration
     */
    lateinit var kind: Kind

    /**
     * The definition of this declaration
     */
    lateinit var definition: Definition<T>

    /**
     * The instance of this declaration
     */
    lateinit var instance: Instance<T>

    /**
     * The module context this declaration lives in
     */
    lateinit var moduleContext: ModuleContext

    /**
     * Resolves the instance
     */
    fun resolveInstance(
        params: ParamsDefinition? = null
    ) = instance.get(params)

    /**
     * Creates the instance holder
     */
    fun createInstanceHolder() {
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
            val declaration = Declaration(type, name)
            declaration.kind = kind
            declaration.definition = definition
            declaration.createInstanceHolder()
            return declaration
        }

    }
}

/**
 * Defines a declaration
 */
typealias Definition<T> = DefinitionContext.(params: Parameters) -> T

/**
 * Binds this [Declaration] to [type]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(type: KClass<T>) = apply {
    val newDeclaration = copy(type = type as KClass<S>, name = null)
    newDeclaration.kind = kind
    newDeclaration.override = override
    newDeclaration.createOnStart = createOnStart
    newDeclaration.attributes = attributes
    newDeclaration.definition = definition
    newDeclaration.instance = instance
    newDeclaration.moduleContext = moduleContext
    moduleContext.declare(newDeclaration)
}

/**
 * Binds this [Declaration] to [name]
 */
infix fun <T : Any, S : T> Declaration<S>.bind(name: String) = apply {
    val newDeclaration = copy(type = type, name = name)
    newDeclaration.kind = kind
    newDeclaration.override = override
    newDeclaration.createOnStart = createOnStart
    newDeclaration.attributes = attributes
    newDeclaration.definition = definition
    newDeclaration.instance = instance
    newDeclaration.moduleContext = moduleContext
    moduleContext.declare(newDeclaration)
}