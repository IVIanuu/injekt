package com.ivianuu.injekt

typealias Definition<T> = (params: Parameters) -> T

typealias ModuleDefinition = Module.() -> Unit

typealias ParamsDefinition = () -> Parameters