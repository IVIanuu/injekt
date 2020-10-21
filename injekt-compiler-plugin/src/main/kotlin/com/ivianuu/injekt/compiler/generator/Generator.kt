/*
 * Copyright 2020 Manuel Wrage
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

package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.ChildComponent
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

interface Generator {
    fun preProcess(files: List<KtFile>) {
    }
    fun generate(files: List<KtFile>)
}

@ChildComponent
abstract class GenerationComponent(
    @Binding protected val moduleDescriptor: ModuleDescriptor,
    @Binding protected val bindingContext: BindingContext
) {
    abstract val errorCollector: ErrorCollector
    abstract val funBindingGenerator: FunBindingGenerator
    abstract val fileManager: FileManager
    abstract val bindingModuleGenerator: BindingModuleGenerator
    abstract val componentGenerator: ComponentGenerator
    abstract val implBindingGenerator: ImplBindingGenerator
    abstract val indexGenerator: IndexGenerator
}
