package com.ivianuu.injekt.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingDefinition
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.LinkedBinding
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.UnlinkedBinding
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.internal.injektIntrinsic
import kotlin.reflect.KClass

@Module
inline fun <reified T : ViewModel> viewModel(
    qualifier: KClass<*>? = null,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
inline fun <reified T : ViewModel> viewModel(
    qualifier: KClass<*>? = null,
    binding: Binding<T>
): Unit = injektIntrinsic()

@Module
inline fun <reified T : ViewModel> viewModel(
    key: Key<T>,
    bindingDefinition: BindingDefinition<T>
): Unit = injektIntrinsic()

@Module
fun <T : ViewModel> viewModel(
    key: Key<T>,
    binding: Binding<T>
) {
    factory(
        key = key,
        binding = ViewModelBinding(
            key.classifier.java as Class<T>,
            key.qualifier?.java?.name,
            binding
        )
    )
}

private class ViewModelBinding<T : ViewModel>(
    private val viewModelClass: Class<T>,
    private val viewModelKey: String?,
    private val viewModelBinding: Binding<T>
) : UnlinkedBinding<T>() {
    override fun link(linker: Linker): LinkedBinding<T> =
        Linked(
            viewModelClass,
            viewModelKey,
            viewModelBinding.link(linker),
            linker.get()
        )

    private class Linked<T : ViewModel>(
        private val viewModelClass: Class<T>,
        private val viewModelKey: String?,
        private val viewModelProvider: Provider<T>,
        private val viewModelStoreProvider: Provider<ViewModelStore>
    ) : LinkedBinding<T>() {
        private var viewModelFactory: ViewModelProvider.Factory? = null
        override fun invoke(parameters: Parameters): T {
            if (viewModelFactory == null) {
                viewModelFactory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                        viewModelProvider(parameters) as T
                }
            }

            val viewModelProvider = ViewModelProvider(viewModelStoreProvider(), viewModelFactory!!)

            return if (viewModelKey != null) {
                viewModelProvider[viewModelKey, viewModelClass]
            } else {
                viewModelProvider[viewModelClass]
            }
        }
    }
}
