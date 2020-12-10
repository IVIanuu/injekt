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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.ChildComponent
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer

@ChildComponent
abstract class GenerationComponent(
    @Binding protected val moduleDescriptor: ModuleDescriptor,
    @Binding protected val bindingContext: BindingContext,
    @Binding protected val bindingTrace: BindingTrace,
    @Binding protected val lazyTopDownAnalyzer: LazyTopDownAnalyzer,
) {
    abstract val declarationStore: DeclarationStore
    abstract val fileManager: FileManager
    abstract val givenCallChecker: GivenCallChecker
    abstract val givenInfoGenerator: GivenInfoGenerator
    abstract val indexGenerator: IndexGenerator
}
