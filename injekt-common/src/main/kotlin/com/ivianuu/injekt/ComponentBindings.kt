package com.ivianuu.injekt

@ModuleMarker
private val ComponentModule = Module(scope = AnyScope, invokeOnInit = true) {
    factory(
        behavior = Bound,
        duplicateStrategy = DuplicateStrategy.Override,
        provider = ComponentProvider(null)
    )

    onScopeAdded { scope ->
        factory(
            qualifier = scope,
            behavior = Bound,
            duplicateStrategy = DuplicateStrategy.Override,
            provider = ComponentProvider(scope)
        )
    }
}

private class ComponentProvider(private val scope: Scope?) : AbstractBindingProvider<Component>() {
    private lateinit var component: Component
    override fun doLink(linker: Linker) {
        component = if (scope == null) linker.component
        else linker.getLinker(scope).component
    }

    override fun invoke(parameters: Parameters): Component = component
}