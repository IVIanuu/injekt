/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.android

/**
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.DefinitionContext
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.typeOf

/**
 * Declares a new [ViewModel] binding which will be scoped by the [ViewModelStore]
*/
inline fun <reified T : ViewModel> ModuleBuilder.viewModel(
name: Qualifier? = null,
viewModelStoreName: Qualifier? = null,
override: Boolean = false,
noinline definition: Definition<T>
): Binding<T> = viewModel(typeOf(), name, viewModelStoreName, override, definition)

/**
 * Declares a new [ViewModel] binding which will be scoped by the [ViewModelStore]
*/
fun <T : ViewModel> ModuleBuilder.viewModel(
type: Type<T>,
name: Qualifier? = null,
viewModelStoreName: Qualifier? = null,
override: Boolean = false,
definition: Definition<T>
): Binding<T> =
factory(
type,
name,
override,
ViewModelDefinition(type, name?.toString(), viewModelStoreName, definition)
)

private class ViewModelBinding<T : ViewModel>(
private val type: Type<T>,
private val key: String?,
private val viewModelStoreName: Qualifier?,
private val definition: Definition<T>
) : Binding<T> {

}

private class ViewModelDefinition<T : ViewModel>(
private val type: Type<T>,
private val key: String?,
private val viewModelStoreName: Qualifier?,
private val definition: Definition<T>
) : (DefinitionContext, Parameters) -> T {
override fun invoke(context: DefinitionContext, parameters: Parameters): T = with(context) {
val store = get<ViewModelStore>(viewModelStoreName)

val factory = Factory(context, parameters, definition)
val provider = ViewModelProvider(store, factory)
return@with if (key != null) {
provider.get(key, type.raw.java as Class<T>)
} else {
provider.get(type.raw.java as Class<T>)
}
}

private class Factory<T : ViewModel>(
private val context: DefinitionContext,
private val parameters: Parameters,
private val definition: Definition<T>
) : ViewModelProvider.Factory {
override fun <T : ViewModel?> create(modelClass: Class<T>): T =
definition(context, parameters) as T
}
}*/