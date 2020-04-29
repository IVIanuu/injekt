package com.ivianuu.injekt

import com.ivianuu.injekt.internal.AliasBinding
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

@JvmName("aliasQualifier")
@Module
inline fun <reified T> alias(
    originalQualifier: KClass<*>? = null,
    aliasQualifier: KClass<*>
): Unit = injektIntrinsic()

@Module
fun <T> alias(
    originalKey: Key<T>,
    aliasQualifier: KClass<*>?
) {
    alias(
        originalKey = originalKey,
        aliasKey = originalKey.copy(qualifier = aliasQualifier)
    )
}

@Module
inline fun <reified S : T, reified T> alias(
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
@Module
fun <S : T, T> alias(
    originalKey: Key<S>,
    aliasKey: Key<T>
) {
    addBinding(aliasKey as Key<S>, AliasBinding(originalKey))
}

