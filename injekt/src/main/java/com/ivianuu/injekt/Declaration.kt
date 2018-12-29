package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val primaryType: KClass<T>,
    val name: String?
) {

    var options = Options()
    var secondaryTypes: List<KClass<*>> = emptyList()
    var setBindings: Set<String> = emptySet()
    var mapBindings: Map<String, Any> = emptyMap()

    lateinit var kind: Kind
    lateinit var definition: BeanDefinition<T>
    lateinit var instance: Instance<T>

    internal val classes: List<KClass<*>> get() = listOf(primaryType) + secondaryTypes

    /**
     * Resolves the instance
     */
    fun resolveInstance(
        params: ParamsDefinition?
    ) = instance.get(params)

    /**
     * Binds this [Declaration] to [type]
     */
    infix fun bind(type: KClass<*>) = apply {
        if (secondaryTypes.contains(type)) return@apply

        if (!type.java.isAssignableFrom(this.primaryType.java)) {
            throw IllegalArgumentException("Can't bind kind '$type' for declaration $this")
        } else {
            secondaryTypes += type
        }
    }

    infix fun intoSet(setName: String) = apply {
        setBindings += setName
    }

    infix fun intoMap(pair: Pair<String, Any>) = apply {
        mapBindings += pair
    }

    override fun toString(): String {
        val kind = kind.toString()
        val name = name?.let { "name:'$name', " } ?: ""
        val type = "type:'${primaryType.getFullName()}'"
        val secondaryTypes = if (secondaryTypes.isNotEmpty()) {
            val typesAsString = secondaryTypes.joinToString(", ") { it.getFullName() }
            ", secondary types:$typesAsString"
        } else ""
        return "$kind[$name$type$secondaryTypes]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> create(
            primaryType: KClass<T>,
            name: String? = null,
            kind: Kind,
            definition: BeanDefinition<T>
        ): Declaration<T> {
            val declaration = Declaration(primaryType, name)

            declaration.kind = kind

            declaration.instance = when (kind) {
                Kind.FACTORY -> FactoryInstance(declaration)
                Kind.SINGLE -> SingleInstance(declaration)
            }

            declaration.definition = definition

            return declaration
        }

    }
}

/**
 * Binds this [Declaration] to [types]
 */
infix fun Declaration<*>.binds(types: Array<KClass<*>>) = apply {
    types.forEach { bind(it) }
}