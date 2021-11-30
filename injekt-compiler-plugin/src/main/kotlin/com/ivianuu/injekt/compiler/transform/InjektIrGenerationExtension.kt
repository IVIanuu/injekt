/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.shaded_injekt.Inject
import com.ivianuu.shaded_injekt.Provide
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

var dumpAllFiles = false

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjektIrGenerationExtension(
  private val dumpDir: File,
  @Inject private val injektFqNames: InjektFqNames
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, @Provide pluginContext: IrPluginContext) {
    @Provide val ctx = Context(
      pluginContext.moduleDescriptor,
      injektFqNames,
      DelegatingBindingTrace(pluginContext.bindingContext, "IR trace")
    )
    @Provide val localClassCollector = LocalDeclarations()
    moduleFragment.transform(localClassCollector, null)

    moduleFragment.transform(InjectCallTransformer(), null)

    moduleFragment.patchDeclarationParents()
    moduleFragment.dumpToFiles(dumpDir)
    moduleFragment.fixComposeFunInterfacesPreCompose()
  }

  @OptIn(ObsoleteDescriptorBasedAPI::class)
  override fun resolveSymbol(symbol: IrSymbol, context: TranslationPluginContext): IrDeclaration? {
    val descriptor = symbol.descriptor
    return if (descriptor is TypeParameterDescriptor)
      IrTypeParameterImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        IrDeclarationOrigin.DEFINED,
        symbol.cast(),
        descriptor.name,
        descriptor.index,
        descriptor.isReified,
        descriptor.variance
      ) else null
  }
}
