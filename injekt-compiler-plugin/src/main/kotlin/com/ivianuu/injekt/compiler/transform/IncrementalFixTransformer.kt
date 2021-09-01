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

import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.analysis.AnalysisContext
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injectablesLookupName
import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.injectableConstructors
import com.ivianuu.injekt.compiler.resolution.isProvide
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import java.util.Base64
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(ObsoleteDescriptorBasedAPI::class)
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
      visibility = DescriptorVisibilities.PUBLIC
    }.apply clazz@ {
      superTypes += pluginContext.irBuiltIns.anyType
      createImplicitParameterDeclarationWithWrappedDescriptor()

      addConstructor {
        returnType = defaultType
        isPrimary = true
        visibility = DescriptorVisibilities.PUBLIC
      }.apply {
        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
          +irDelegatingConstructorCall(
            context.irBuiltIns.anyClass.constructors.single().owner
          )
          +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            this@clazz.symbol,
            context.irBuiltIns.unitType
          )
        }
      }

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
        DescriptorVisibilities.PUBLIC,
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
          .replace("=", "")
          .replace("_", "")
          .chunked(256)
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
        .descriptors + clazz.descriptor + functions.map { it.descriptor }
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
