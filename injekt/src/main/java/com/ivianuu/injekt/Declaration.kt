package com.ivianuu.injekt

import kotlin.reflect.KClass

/**
 * Represents a dependency declaration.
 */
data class Declaration<T : Any> private constructor(
    val primaryType: KClass<T>,
    val name: String?
) {

    lateinit var module: Module

    var attributes = Attributes()
    var options = Options()
    var secondaryTypes: List<KClass<*>> = emptyList()

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

    override fun toString(): String {
        val kindString = kind.toString()
        val nameString = name?.let { "name:'$name', " } ?: ""
        val typeString = "kind:'${primaryType.getFullName()}'"
        val secondaryTypesString = if (secondaryTypes.isNotEmpty()) {
            val typesAsString = secondaryTypes.joinToString(", ") { it.getFullName() }
            ", secondary types:$typesAsString"
        } else ""
        return "$kindString[$nameString$typeString$secondaryTypesString]"
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