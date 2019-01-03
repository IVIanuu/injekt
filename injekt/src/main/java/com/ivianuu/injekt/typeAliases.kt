package com.ivianuu.injekt

typealias Definition<T> = DefinitionContext.(params: Parameters) -> T

typealias ComponentDefinition = Component.() -> Unit

typealias InjektDefinition = InjektPlugins.() -> Unit

typealias ModuleDefinition = ModuleContext.() -> Unit

typealias ParamsDefinition = () -> Parameters