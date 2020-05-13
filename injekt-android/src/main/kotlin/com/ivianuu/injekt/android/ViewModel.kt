package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.classOf
import com.ivianuu.injekt.transient

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
