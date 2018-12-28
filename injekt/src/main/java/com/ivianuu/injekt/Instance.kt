package com.ivianuu.injekt

import com.ivianuu.injekt.InjektPlugins.logger

/**
 * The [Instance] of an [Declaration]
 */
abstract class Instance<T : Any>(val declaration: Declaration<T>) {

    val component get() = _component ?: error("Component not initialized")
    private var _component: Component? = null

    internal fun setComponent(component: Component) {
        if (_component != null) {
            error("Instances cannot be reused $declaration")
        }

        _component = component
    }

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

private object UNINITIALIZED_VALUE

internal class SingleInstance<T : Any>(
    declaration: Declaration<T>
) : Instance<T>(declaration) {

    private var _value: Any? = UNINITIALIZED_VALUE

    override val isCreated: Boolean
        get() = _value !== UNINITIALIZED_VALUE

    override fun getInternal(params: ParamsDefinition?): T {
        val value = _value
        return if (value !== UNINITIALIZED_VALUE) {
            @Suppress("UNCHECKED_CAST")
            return value as T
        } else {
            val typedValue = declaration.definition
                .invoke(params?.invoke() ?: emptyParameters())
            _value = typedValue
            typedValue
        }
    }

}