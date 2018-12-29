package com.ivianuu.injekt

typealias BeanDefinition<T> = (params: Parameters) -> T

typealias ModuleDefinition = Module.() -> Unit

typealias ParamsDefinition = () -> Parameters