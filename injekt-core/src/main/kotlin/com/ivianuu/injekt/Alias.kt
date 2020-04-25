package com.ivianuu.injekt

import kotlin.reflect.KClass

inline fun <reified T> ModuleDsl.alias(
    aliasQualifier: KClass<*>?,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    alias<T, T>(
        aliasQualifier = aliasQualifier,
        duplicateStrategy = duplicateStrategy
    )
}

fun <T> ModuleDsl.alias(
    originalKey: Key<T>,
    aliasQualifier: KClass<*>?,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    alias(
        originalKey = originalKey,
        aliasKey = originalKey.copy(qualifier = aliasQualifier),
        duplicateStrategy = duplicateStrategy
    )
}

inline fun <reified S : T, reified T> ModuleDsl.alias(
    originalQualifier: KClass<*>? = null,
    aliasQualifier: KClass<*>? = null,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    alias(
        originalKey = keyOf<S>(originalQualifier),
        aliasKey = keyOf<T>(aliasQualifier),
        duplicateStrategy = duplicateStrategy
    )
}

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
    aliasKey: Key<T>,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail
) {
    add(Binding(aliasKey as Key<S>, duplicateStrategy, AliasProvider(originalKey)))
}

private class AliasProvider<T>(private val originalKey: Key<T>) : AbstractProvider<T>() {
    private lateinit var originalProvider: Provider<T>
    override fun link(linker: Linker) {
        originalProvider = linker.get(originalKey)
    }

    override fun invoke(parameters: Parameters) = originalProvider(parameters)
}
