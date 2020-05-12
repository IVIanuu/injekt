package com.ivianuu.injekt.android

/*
@InstallIn<RetainedActivityComponent>
@BindingAdapter
annotation class BindViewModel {
    companion object {
        @Module
        inline fun <T : ViewModel> bind() {
            viewModel<T>()
        }
    }
}*/

/*
@BindViewModel
class MyViewModel : ViewModel() {
    companion object {
        @InstallIn<RetainedActivityScoped>
        @Module
        fun bindingModule() {
            BindViewModel.bind<MyViewModel>()
        }
    }
}*/

/*
@Qualifier
private annotation class UnscopedViewModel

@Module
inline fun <T : ViewModel> viewModel() {
    transient<@UnscopedViewModel T>()

    val clazz = classOf<T>()
    transient {
        val viewModelStoreOwner = get<ViewModelStoreOwner>()
        val viewModelProvider = get<@Provider () -> @UnscopedViewModel T>()
        ViewModelProvider(
            viewModelStoreOwner,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    viewModelProvider() as T
            }
        ).get(clazz.java)
    }
}
*/