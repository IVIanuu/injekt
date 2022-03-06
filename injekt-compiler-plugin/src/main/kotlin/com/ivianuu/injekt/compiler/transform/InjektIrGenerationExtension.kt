/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.*
import java.io.*

var dumpAllFiles = false

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjektIrGenerationExtension(private val dumpDir: File) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val ctx = Context(
      pluginContext.moduleDescriptor,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )
    val localDeclarations = LocalDeclarations()
    moduleFragment.transform(localDeclarations, null)

    moduleFragment.transform(InjectCallTransformer(localDeclarations, pluginContext, ctx), null)

    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir, pluginContext)
    moduleFragment.fixComposeFunInterfacesPreCompose(pluginContext)
  }
}
