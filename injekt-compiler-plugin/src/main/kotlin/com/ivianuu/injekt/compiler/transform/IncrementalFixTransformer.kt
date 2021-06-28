/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.analysis.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.*

class IncrementalFixTransformer(
  @Inject private val pluginContext: IrPluginContext,
  @Inject private val analysisContext: AnalysisContext
) : IrElementTransformerVoid() {
  private val injectablesByFile = mutableMapOf<IrFile, MutableSet<CallableRef>>()
  override fun visitFile(declaration: IrFile): IrFile {
    super.visitFile(declaration)
    val injectables = injectablesByFile[declaration] ?: return declaration

    val clazz = IrFactoryImpl.buildClass {
      name = "${
        pluginContext.moduleDescriptor.name
          .asString().replace("<", "")
          .replace("-", "")
          .replace(">", "")
      }_${
        declaration.fileEntry.name.removeSuffix(".kt")
          .substringAfterLast(".")
          .substringAfterLast("/")
      }_ProvidersMarker".asNameId()
      visibility = DescriptorVisibilities.PRIVATE
    }.apply {
      createImplicitParameterDeclarationWithWrappedDescriptor()
      parent = declaration
      declaration.addChild(this)
    }

    val functions = injectables.mapIndexed { index, callable ->
      IrFunctionImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        IrDeclarationOrigin.DEFINED,
        IrSimpleFunctionSymbolImpl(
          IncrementalFixFunctionDescriptor(declaration.packageFragmentDescriptor)
        ),
        injectablesLookupName(
          callable.callable.fqNameSafe.parent(),
          declaration.fqName
        ),
        DescriptorVisibilities.INTERNAL,
        Modality.FINAL,
        pluginContext.irBuiltIns.unitType,
        false,
        false,
        false,
        false,
        false,
        false,
        false
      ).apply {
        descriptor.cast<IncrementalFixFunctionDescriptor>().bind(this)
        parent = declaration
        declaration.addChild(this)
        addValueParameter("marker", clazz.defaultType)
        repeat(index) {
          addValueParameter("index$it", pluginContext.irBuiltIns.byteType)
        }
        val callableInfo = callable.callable
          .annotations
          .findAnnotation(InjektFqNames.CallableInfo)!!
          .allValueArguments
          .values
          .single()
          .value
          .cast<String>()
        val classifierInfo = callable
          .takeIf { it.callable is ClassConstructorDescriptor }
          ?.type
          ?.classifier
          ?.descriptor
          ?.annotations
          ?.findAnnotation(InjektFqNames.ClassifierInfo)
          ?.allValueArguments
          ?.values
          ?.single()
          ?.value
          ?.cast<String>()

        val finalHash = String(
          Base64.getEncoder()
            .encode((callableInfo + classifierInfo.orEmpty()).toByteArray())
        )

        finalHash
          .replace("/", "")
          .chunked(100)
          .forEachIndexed { index, value ->
            addValueParameter(
              "hash_${index}_$value",
              pluginContext.irBuiltIns.intType
            )
          }
        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
        }
      }
    }

    declaration.metadata = DescriptorMetadataSource.File(
      declaration.metadata.cast<DescriptorMetadataSource.File>()
        .descriptors + functions.map { it.descriptor }
    )

    return declaration
  }

  override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
    if (declaration is IrDeclarationWithVisibility &&
      (declaration.visibility == DescriptorVisibilities.PUBLIC ||
          declaration.visibility == DescriptorVisibilities.INTERNAL ||
          declaration.visibility == DescriptorVisibilities.PROTECTED) &&
      (declaration !is IrConstructor ||
          (declaration.constructedClass.visibility == DescriptorVisibilities.PUBLIC ||
              declaration.constructedClass.visibility == DescriptorVisibilities.INTERNAL ||
              declaration.constructedClass.visibility == DescriptorVisibilities.PROTECTED)) &&
      declaration.descriptor.isProvide()
    ) {
      injectablesByFile.getOrPut(declaration.file) { mutableSetOf() } += when (declaration) {
        is IrClass -> declaration.descriptor.injectableConstructors()
        is IrFunction -> listOf(declaration.descriptor.toCallableRef())
        is IrProperty -> listOf(declaration.descriptor.toCallableRef())
        else -> return super.visitDeclaration(declaration)
      }
    }
    return super.visitDeclaration(declaration)
  }
}
