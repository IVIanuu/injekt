package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [Declaration]
 */
abstract class Instance<T : Any>(val declaration: Declaration<T>) {

    lateinit var component: Component

    abstract val isCreated: Boolean

    fun get(params: ParamsDefinition? = null): T {
        logger?.let { logger ->
            val msg = when {
                isCreated -> "${component.nameString()}Return existing instance for $declaration"
                declaration.createOnStart -> "${component.nameString()}Create instance on start up $declaration"
                else -> "${component.nameString()}Create instance $declaration"
            }

            logger.info(msg)
        }

        return try {
            getOrCreate(params)
        } catch (e: Exception) {
            throw InstanceCreationException("Couldn't instantiate $declaration", e)
        }
    }

    protected abstract fun getOrCreate(params: ParamsDefinition?): T

}

internal class FactoryInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    override val isCreated: Boolean
        get() = false

    override fun getOrCreate(params: ParamsDefinition?) =
        declaration.definition.invoke(component.context, params?.invoke() ?: emptyParameters())

}

internal class SingleInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun getOrCreate(params: ParamsDefinition?): T {
        val value = _value

        return if (value != null) {
            return value
        } else {
            val typedValue = declaration.definition
                .invoke(component.context, params?.invoke() ?: emptyParameters())
            _value = typedValue
            typedValue
        }
    }

}