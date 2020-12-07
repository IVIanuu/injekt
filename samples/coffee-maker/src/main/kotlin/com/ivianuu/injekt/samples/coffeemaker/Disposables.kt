package com.ivianuu.injekt.samples.coffeemaker

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Interceptor
import com.ivianuu.injekt.Scoped

@Scoped
@Binding
class Disposables {
    private val disposables = mutableListOf<Disposable>()

    fun add(disposable: Disposable) {
        disposables += disposable
    }

    fun dispose() {
        disposables.forEach { it.dispose() }
        disposables.clear()
    }
}

interface Disposable {
    fun dispose()
}

@Interceptor fun <T : Disposable> interceptDisposable(
    disposables: Disposables,
    factory: () -> T,
): () -> T {
    return {
        val instance = factory()
        disposables.add(instance)
        instance
    }
}
