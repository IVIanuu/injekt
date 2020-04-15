package com.ivianuu.injekt

@Module
private val ComponentModule = Module(scope = AnyScope, invokeOnInit = true) {
    factory(
        behavior = Bound,
        duplicateStrategy = DuplicateStrategy.Override
    ) { this }

    onScopeAdded { scope ->
        factory(
            qualifier = scope,
            behavior = Bound,
            duplicateStrategy = DuplicateStrategy.Override
        ) { this }
    }
}