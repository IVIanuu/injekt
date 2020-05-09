package com.ivianuu.injekt.android

/*
@BindingAdapter
annotation class BindViewModel {
    companion object {
        @Module
        inline fun <reified T : ViewModel> bind() {
            viewModel(T::class)
        }
    }
}

@Qualifier
private annotation class OriginalViewModel

@Module
fun <T : ViewModel> viewModel(viewModelClass: KClass<T>) {
    transient<@OriginalViewModel T>()
    transient {
        val viewModelStoreOwner = get<ViewModelStoreOwner>()
        val viewModelProvider = get<@Provider () -> @OriginalViewModel T>()
        ViewModelProvider(
            viewModelStoreOwner,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                    viewModelProvider() as T
            }
        ).get(viewModelClass.java)
    }
}
*/
