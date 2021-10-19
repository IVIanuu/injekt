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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irUnit
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@OptIn(ObsoleteDescriptorBasedAPI::class) class EntryPointTransformer(
  @Inject private val context: InjektContext,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
  private val entryPointClassByFunction = mutableMapOf<IrFunction, IrClass>()

  override fun visitFile(declaration: IrFile): IrFile {
    return super.visitFile(declaration)
      .also {
        entryPointClassByFunction.values
          .filter { it.parent == declaration }
          .forEach { declaration.addChild(it) }
      }
  }

  fun entryPointClassFor(function: IrFunction): IrClass =
    entryPointClassByFunction.getOrPut(function) {
      function.createEntryPointClass()
    }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    if (declaration.hasAnnotation(injektFqNames().entryPoint)) {
      val entryPointClass = entryPointClassFor(declaration)

      declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).run {
        irBlockBody {
          +irReturn(irUnit())
        }
      }
    }
    return super.visitFunction(declaration)
  }

  private fun IrFunction.createEntryPointClass(): IrClass = IrFactoryImpl.buildClass {
    origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
    name = entryPointClassName()
    kind = ClassKind.INTERFACE
    modality = Modality.ABSTRACT
  }.apply {
    parent = this@createEntryPointClass.file
    createImplicitParameterDeclarationWithWrappedDescriptor()

    val typeParameters = this@createEntryPointClass.typeParameters
      .map { functionTypeParameter ->
        addTypeParameter {
          name = functionTypeParameter.name
          variance = functionTypeParameter.variance
        }
      }

    val substitutionMap = this@createEntryPointClass.typeParameters
      .map { it.symbol }
      .zip(typeParameters.map { it.defaultType })
      .toMap()

    this@createEntryPointClass.typeParameters
      .zip(typeParameters)
      .forEach { (functionTypeParameter, classTypeParameter) ->
        functionTypeParameter.superTypes += classTypeParameter.superTypes
          .map { it.substitute(substitutionMap) }
      }

    addFunction {

      name = entryPointClassName()

    }
  }

  private fun IrFunction.entryPointClassName(): Name =
    descriptor.fqNameSafe.pathSegments().joinToString("_") + "$uni"
}
