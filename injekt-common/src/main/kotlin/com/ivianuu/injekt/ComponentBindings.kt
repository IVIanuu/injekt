package com.ivianuu.injekt

@ModuleMarker
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