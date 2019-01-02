package com.ivianuu.injekt

/**
 * The [Instance] of an [Declaration]
 */
abstract class Instance<T : Any>(val declaration: Declaration<T>) {

    lateinit var component: Component

    /**
     * Whether or not this instance is created
     */
    abstract val isCreated: Boolean

    /**
     * Returns a instance of [T]
     */
    abstract fun get(params: ParamsDefinition? = null): T

    protected open fun create(params: ParamsDefinition?): T {
        return try {
            declaration.definition.invoke(
                component.context,
                params?.invoke() ?: emptyParameters()
            )
        } catch (e: Exception) {
            throw InstanceCreationException(
                "${component.name} Couldn't instantiate $declaration",
                e
            )
        }
    }

}

/**
 * A [Instance] which creates a new value on every [get] call
 */
class FactoryInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    override val isCreated: Boolean
        get() = false

    override fun get(params: ParamsDefinition?) = create(params)

}

/**
 * A [Instance] which creates the value 1 time and caches the result
 */
class SingleInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun get(params: ParamsDefinition?): T {
        val value = _value

        return if (value != null) {
            return value
        } else {
            create(params).also { _value = it }
        }
    }

}