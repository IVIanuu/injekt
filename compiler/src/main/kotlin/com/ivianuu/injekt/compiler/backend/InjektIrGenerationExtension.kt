/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.compiler.Context
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import java.io.File

var dumpAllFiles = false

class InjektIrGenerationExtension(private val dumpDir: File) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val ctx = Context(
      pluginContext.moduleDescriptor,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )

    moduleFragment.transform(InjectCallTransformer(pluginContext, ctx), null)

    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}
