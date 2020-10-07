package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.FunBinding
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.merge.BindingModule
import kotlin.reflect.KClass

@BindingModule(ActivityComponent::class)
annotation class ActivityViewModelBinding {
    @Module
    class ModuleImpl<VM : S, S : ViewModel>(
        private val viewModelClass: KClass<S>
    ) {
        @Module
        fun viewModel(getViewModel: getViewModel<VM, S, ActivityViewModelStoreOwner>): S =
            getViewModel(viewModelClass)

        companion object {
            inline operator fun <reified VM : S, reified S : ViewModel> invoke() =
                ModuleImpl<VM, S>(S::class)
        }
    }
}

@BindingModule(FragmentComponent::class)
annotation class FragmentViewModelBinding {
    @Module
    class ModuleImpl<VM : S, S : ViewModel>(
        private val viewModelClass: KClass<S>
    ) {
        @Module
        fun viewModel(getViewModel: getViewModel<VM, S, FragmentViewModelStoreOwner>): S =
            getViewModel(viewModelClass)

        companion object {
            inline operator fun <reified VM : S, reified S : ViewModel> invoke() =
                ModuleImpl<VM, S>(S::class)
        }
    }
}

@FunBinding
fun <VM : S, S : ViewModel, VMSO : ViewModelStoreOwner> getViewModel(
    viewModelStoreOwner: VMSO,
    viewModelFactory: () -> VM,
    viewModelClass: @Assisted KClass<S>
): S {
    return ViewModelProvider(
        viewModelStoreOwner,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                viewModelFactory() as T
        }
    )[viewModelClass.java]
}
