package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [Declaration]
 */
abstract class Instance<T : Any>(val declaration: Declaration<T>) {

    lateinit var component: Component
        internal set

    abstract val isCreated: Boolean

    fun get(params: ParamsDefinition?): T {
        logger?.let { logger ->
            val msg = when {
                isCreated -> "${component.nameString()}Return existing instance for $declaration"
                declaration.options.createOnStart -> "${component.nameString()}Create instance on start up $declaration"
                else -> "${component.nameString()}Create instance $declaration"
            }

            logger.info(msg)
        }

        return getInternal(params)
    }

    fun create(params: ParamsDefinition?): T {
        return try {
            get(params)
        } catch (e: InjektException) {
            throw e
        } catch (e: Exception) {
            throw InstanceCreationException(
                "${component.nameString()}Could not instantiate $declaration",
                e
            )
        }
    }

    protected abstract fun getInternal(params: ParamsDefinition?): T

}

internal class FactoryInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    override val isCreated: Boolean
        get() = false

    override fun getInternal(params: ParamsDefinition?) =
        declaration.definition.invoke(params?.invoke() ?: emptyParameters())

}

internal class SingleInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    private var _value: T? = null

    override val isCreated: Boolean
        get() = _value != null

    override fun getInternal(params: ParamsDefinition?): T {
        val value = _value

        return if (value != null) {
            return value
        } else {
            val typedValue = declaration.definition
                .invoke(params?.invoke() ?: emptyParameters())
            _value = typedValue
            typedValue
        }
    }

}