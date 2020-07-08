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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.transform.component.ComponentReaderTransformer
import com.ivianuu.injekt.compiler.transform.component.ComponentTransformer
import com.ivianuu.injekt.compiler.transform.reader.ReaderTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbolRemapper = DeepCopySymbolRemapper()
        val pluginContext = InjektPluginContext(moduleFragment, pluginContext, symbolRemapper)

        ReaderTransformer(pluginContext, symbolRemapper).doLower(moduleFragment)

        InfoPackageDeclarationTransformer(pluginContext).doLower(moduleFragment)

        ComponentReaderTransformer(pluginContext).doLower(moduleFragment)

        println(moduleFragment.dumpSrc())

        ComponentTransformer(pluginContext).doLower(moduleFragment)

        TmpMetadataPatcher(pluginContext).doLower(moduleFragment)

        println(moduleFragment.dumpSrc())
    }

}

