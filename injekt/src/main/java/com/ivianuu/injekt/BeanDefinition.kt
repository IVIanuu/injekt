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
     * The target scope id
     */
    var scopeId: String? = null

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
        val optionsString = "scopeId:$scopeId,override:$override,createOnStart:$createOnStart"
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

fun <T : Any> BeanDefinition<T>.clone() = copy().also {
    it.kind = kind
    it.definition = definition
    it.scopeId = scopeId
    it.override = override
    it.createOnStart = createOnStart
    it.attributes = attributes
    it.createInstanceHolder()
}