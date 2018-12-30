package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val primaryType: KClass<T>,
    val name: String?
) {

    var attributes = Attributes()
    var options = Options()
    var secondaryTypes: List<KClass<*>> = emptyList()
    var setBindings: Set<String> = emptySet()
    var mapBindings: Map<String, Any> = emptyMap()

    lateinit var kind: Kind
    lateinit var definition: Definition<T>
    lateinit var instance: Instance<T>

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
        secondaryTypes += type
    }

    /**
     * Multi binds this [Declaration] into the set with the [setName]
     */
    infix fun intoSet(setName: String) = apply {
        setBindings += setName
    }

    /**
     * Multi binds this [Declaration] into the map with the [Pair.first] with the key [Pair.second]
     */
    infix fun intoMap(pair: Pair<String, Any>) = apply {
        mapBindings += pair
    }

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "kind:'${primaryType.getFullName()}'"
        val secondaryTypesString = if (secondaryTypes.isNotEmpty()) {
            val typesAsString = secondaryTypes.joinToString(", ") { it.getFullName() }
            ", secondary types:$typesAsString"
        } else ""
        val setBindingsString = if (setBindings.isNotEmpty()) {
            val bindingsAsString = setBindings.joinToString(", ") { it }
            ", set bindings:$bindingsAsString"
        } else ""
        val mapBindingsString = if (mapBindings.isNotEmpty()) {
            val bindingsAsString =
                mapBindings.entries.joinToString(", ") { it.key + " " + it.value }
            ", map bindings:$bindingsAsString"
        } else ""
        return "$kindString[$nameString$typeString$secondaryTypesString$setBindingsString$mapBindingsString]"
    }

    enum class Kind { FACTORY, SINGLE }

    companion object {

        fun <T : Any> create(
            primaryType: KClass<T>,
            name: String? = null,
            kind: Kind,
            definition: Definition<T>
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