package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency beanDefinition.
 */
data class BeanDefinition<T : Any> private constructor(
    val type: KClass<T>,
    val name: String?
) {

    /**
     * Whether or not this definitions can override existing definitions
     */
    var override: Boolean = false

    /**
     * Whether or not this beanDefinition should be created on start
     */
    var createOnStart: Boolean = false

    /**
     * Extras
     */
    var attributes = Attributes()

    /**
     * The kind of this beanDefinition
     */
    lateinit var kind: Kind

    /**
     * The definition of this beanDefinition
     */
    lateinit var definition: Definition<T>

    /**
     * The instance of this beanDefinition
     */
    lateinit var instance: Instance<T>

    /**
     * The module context this beanDefinition lives in
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
            BeanDefinition.Kind.FACTORY -> FactoryInstance(this)
            BeanDefinition.Kind.SINGLE -> SingleInstance(this)
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
        ): BeanDefinition<T> {
            val beanDefinition = BeanDefinition(type, name)
            beanDefinition.kind = kind
            beanDefinition.definition = definition
            beanDefinition.createInstanceHolder()
            return beanDefinition
        }

    }
}

/**
 * Defines a beanDefinition
 */
typealias Definition<T> = DefinitionContext.(params: Parameters) -> T

/**
 * Binds this [BeanDefinition] to [type]
 */
infix fun <T : Any, S : T> BeanDefinition<S>.bind(type: KClass<T>) = apply {
    val copy = copy(type = type as KClass<S>, name = null)
    copy.kind = kind
    copy.override = override
    copy.createOnStart = createOnStart
    copy.attributes = attributes
    copy.definition = definition
    copy.instance = instance
    copy.moduleContext = moduleContext
    moduleContext.declare(copy)
}

/**
 * Binds this [BeanDefinition] to [name]
 */
infix fun <T : Any, S : T> BeanDefinition<S>.bind(name: String) = apply {
    val copy = copy(type = type, name = name)
    copy.kind = kind
    copy.override = override
    copy.createOnStart = createOnStart
    copy.attributes = attributes
    copy.definition = definition
    copy.instance = instance
    copy.moduleContext = moduleContext
    moduleContext.declare(copy)
}