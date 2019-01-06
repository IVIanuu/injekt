package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency definition.
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
     * Whether or not this definition should be created on start
     */
    var createOnStart: Boolean = false

    /**
     * Extras
     */
    var attributes = Attributes()

    /**
     * The kind of this definition
     */
    lateinit var kind: Kind

    /**
     * The definition of this definition
     */
    lateinit var definition: Definition<T>

    /**
     * The instance of this definition
     */
    lateinit var instance: Instance<T>

    /**
     * The module context this definition lives in
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
        val attributesString = "attributes: '$attributes'"
        val optionsString = "override:$override,createOnStart:$createOnStart"
        return "$kindString[$nameString$typeString$attributesString$optionsString]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> createFactory(
            type: KClass<T>,
            name: String? = null,
            definition: Definition<T>
        ) = create(type, name, Kind.FACTORY, definition)

        fun <T : Any> createSingle(
            type: KClass<T>,
            name: String? = null,
            definition: Definition<T>
        ) = create(type, name, Kind.SINGLE, definition)

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
 * Defines a [BeanDefinition]
 */
typealias Definition<T> = DefinitionContext.(params: Parameters) -> T

/**
 * Binds this [BeanDefinition] to [type]
 */
infix fun <T : Any> BeanDefinition<T>.bind(type: KClass<*>) = apply {
    val copy = (this as BeanDefinition<Any>).copy(type = type as KClass<Any>, name = null)
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
 * Binds this [BeanDefinition] to [types]
 */
infix fun <T : Any> BeanDefinition<T>.bind(types: Array<KClass<*>>) = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [types]
 */
infix fun <T : Any> BeanDefinition<T>.bind(types: Iterable<KClass<*>>) = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [name]
 */
infix fun <T : Any> BeanDefinition<T>.bind(name: String) = apply {
    val copy = (this as BeanDefinition<Any>).copy(name = name)
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
 * Binds this [BeanDefinition] to [names]
 */
infix fun <T : Any> BeanDefinition<T>.bind(types: Array<String>) = apply {
    types.forEach { bind(it) }
}

/**
 * Binds this [BeanDefinition] to [names]
 */
@JvmName("bindNames")
infix fun <T : Any> BeanDefinition<T>.bind(types: Iterable<String>) = apply {
    types.forEach { bind(it) }
}