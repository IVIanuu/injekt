package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import kotlin.reflect.KClass

@Effect
annotation class GivenActivityViewModel {
    @GivenSet
    companion object {
        @Given
        inline fun <reified VM : S, reified S : ViewModel> viewModel(): S =
            getViewModel<VM, S, ActivityViewModelStoreOwner>(S::class)
    }
}

@Effect
annotation class GivenFragmentViewModel {
    @GivenSet
    companion object {
        @Given
        inline fun <reified VM : S, reified S : ViewModel> viewModel(): S =
            getViewModel<VM, S, FragmentViewModelStoreOwner>(S::class)
    }
}

@PublishedApi
@Reader
internal fun <VM : S, S : ViewModel, VMS : ViewModelStoreOwner> getViewModel(
    viewModelClass: KClass<S>
): S {
    return ViewModelProvider(
        given<VMS>(),
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return given<VM>() as T
            }
        }
    )[viewModelClass.java]
}
