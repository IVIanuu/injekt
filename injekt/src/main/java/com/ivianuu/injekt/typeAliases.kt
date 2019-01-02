package com.ivianuu.injekt

typealias Definition<T> = ComponentContext.(params: Parameters) -> T

typealias ComponentDefinition = Component.() -> Unit

typealias InjektDefinition = InjektPlugins.() -> Unit

typealias ModuleDefinition = Module.() -> Unit

typealias ParamsDefinition = () -> Parameters