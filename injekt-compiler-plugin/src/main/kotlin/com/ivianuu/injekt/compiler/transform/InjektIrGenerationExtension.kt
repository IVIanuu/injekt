/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.utils.addToStdlib.*
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

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun resolveSymbol(symbol: IrSymbol, context: TranslationPluginContext): IrDeclaration? {
    // this is needed to fix a unbound error which seems to occur
    // when using type parameters in injections
    val descriptor = symbol.descriptor as? TypeParameterDescriptor ?: return null
    return IrTypeParameterImpl(
      UNDEFINED_OFFSET,
      UNDEFINED_OFFSET,
      IrDeclarationOrigin.DEFINED,
      symbol.cast(),
      descriptor.name,
      descriptor.index,
      descriptor.isReified,
      descriptor.variance
    )
  }
}
