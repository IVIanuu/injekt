package com.ivianuu.injekt

import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

@JvmName("aliasQualifier")
inline fun <reified T> ModuleDsl.alias(
    originalQualifier: KClass<*>? = null,
    aliasQualifier: KClass<*>
): Unit = injektIntrinsic()

fun <T> ModuleDsl.alias(
    originalKey: Key<T>,
    aliasQualifier: KClass<*>?
) {
    alias(
        originalKey = originalKey,
        aliasKey = originalKey.copy(qualifier = aliasQualifier)
    )
}

inline fun <reified S : T, reified T> ModuleDsl.alias(
    originalQualifier: KClass<*>? = null,
    aliasQualifier: KClass<*>? = null
): Unit = injektIntrinsic()

/**
 * Makes the [Binding] for [originalKey] retrievable via [aliasKey]
 *
 * For example the following code delegates the Repository request to RepositoryImpl
 *
 * ´´´
 * val component = Component {
 *     single { RepositoryImpl() }
 *     alias<RepositoryImpl, Repository>()
 * }
 *
 * val repositoryA = component.get<RepositoryImpl>()
 * val repositoryB = component.get<Repository>()
 * assertSame(repositoryA, repositoryB) // true
 * ´´´
 *
 */
fun <S : T, T> ModuleDsl.alias(
    originalKey: Key<S>,
    aliasKey: Key<T>
) {
    add(aliasKey as Key<S>, AliasProvider(originalKey))
}

private class AliasProvider<T>(private val originalKey: Key<T>) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> = linker.get(originalKey)
}
