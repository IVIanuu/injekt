package com.ivianuu.injekt

class MyViewModel(private val url: String)

//@ModuleMarker
private val MyViewModelModule = Module {
    factory {
        MyViewModel(get())
    }

    factory(provider = object : AbstractBindingProvider<MyViewModel>() {
        private var urlProvider: BindingProvider<String>? = null
        private var boundComponent: Component? = null
        override fun invoke(component: Component, parameters: Parameters): MyViewModel {
            if (boundComponent !== component) urlProvider = null
            if (urlProvider == null) urlProvider = component.getBindingProvider(keyOf())
            return MyViewModel(urlProvider!!(component, parameters))
        }
    })
}
