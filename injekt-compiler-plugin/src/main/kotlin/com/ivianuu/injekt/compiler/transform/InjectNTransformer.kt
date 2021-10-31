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

import com.ivianuu.injekt.compiler.WithInjektContext
import com.ivianuu.injekt.compiler.analysis.InjectNParameterDescriptor
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.context
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(ObsoleteDescriptorBasedAPI::class)
@WithInjektContext
class InjectNTransformer(
  @Inject private val localDeclarationCollector: LocalDeclarationCollector,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
  private val transformedClasses = mutableSetOf<IrClass>()
  private val transformedFunctions = mutableSetOf<IrFunction>()

  fun transformIfNeeded(clazz: IrClass): IrClass {
    val info = clazz.descriptor.classifierInfo()
    if (info.injectNParameters.isEmpty()) return clazz
    if (clazz in transformedClasses) return clazz
    transformedClasses += clazz

    val fieldsByParameter = info.injectNParameters.associateWith { parameter ->
      clazz.addField(
        fieldName = parameter.name,
        fieldType = parameter.typeRef.toIrType().typeOrNull!!
      )
    }

    clazz.declarations
      .filterIsInstance<IrConstructor>()
      .map { transformIfNeeded(it) as IrConstructor }
      .forEach { constructor ->
        val oldBody = constructor.body!!
        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
          oldBody.statements.forEach { statement ->
            +statement
            if (statement is IrDelegatingConstructorCall) {
              val constructorInjectParameters = constructor
                .valueParameters
                .filter { it.name.asString().startsWith("_inject") }
              info.injectNParameters.forEach { parameter ->
                +irSetField(
                  irGet(clazz.thisReceiver!!),
                  fieldsByParameter[parameter]!!,
                  irGet(constructorInjectParameters[parameter.index])
                )
              }
            }
          }
        }
      }

    return clazz
  }

  fun transformIfNeeded(function: IrFunction): IrFunction {
    val injectNParameters = if (function.descriptor is PropertyAccessorDescriptor)
      function.descriptor.cast<PropertyAccessorDescriptor>().correspondingProperty.callableInfo()
        .injectNParameters
    else if (function.descriptor !is AnonymousFunctionDescriptor)
      function.descriptor.callableInfo().injectNParameters
    else function.descriptor.annotations.findAnnotation(injektFqNames.injectNInfo)
      ?.allValueArguments
      ?.values
      ?.single()
      ?.cast<ArrayValue>()
      ?.value
      ?.indices
      ?.map {
        InjectNParameterDescriptor(
          function.descriptor,
          function.valueParameters.size + it,
          context.nullableAnyType
        )
      }
      ?: emptyList()
    if (injectNParameters.isEmpty()) return function

    if (function in transformedFunctions) return function
    transformedFunctions += function

    injectNParameters.forEach { parameter ->
      function.addValueParameter(
        parameter.name,
        parameter.typeRef.toIrType().typeOrNull!!
      )
    }

    return function
  }

  override fun visitClassNew(declaration: IrClass): IrStatement =
    transformIfNeeded(super.visitClassNew(declaration) as IrClass)

  override fun visitFunctionNew(declaration: IrFunction): IrStatement =
    transformIfNeeded(super.visitFunctionNew(declaration) as IrFunction)

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val callee = if (result.symbol.descriptor.name.asString() == "invoke" &&
      result.dispatchReceiver?.type?.hasAnnotation(injektFqNames.inject2) == true) {
      val argCount = result.symbol.owner.valueParameters.size

      val extraArgsCount = result.dispatchReceiver!!.type
        .annotations.findAnnotation(injektFqNames.injectNInfo)
        ?.getValueArgument(0)
        ?.cast<IrVarargImpl>()
        ?.elements
        ?.size
        ?: error("")

      val newFnClass = if (result.dispatchReceiver!!.type.isSuspendFunction())
        pluginContext.irBuiltIns.suspendFunction(argCount + extraArgsCount).owner
      else
        pluginContext.irBuiltIns.function(argCount + extraArgsCount).owner

      newFnClass.functions.first { it.name.asString() == "invoke" }
    } else transformIfNeeded(result.symbol.owner)

    if (result.symbol.owner !in transformedFunctions &&
        callee.symbol == result.symbol) return result

    return when (result) {
      is IrCall -> IrCallImpl(
        result.startOffset,
        result.endOffset,
        result.type,
        callee.symbol.cast(),
        result.typeArgumentsCount,
        callee.valueParameters.size,
        result.origin,
        result.superQualifierSymbol
      ).apply {
        copyTypeAndValueArgumentsFrom(result)
      }
      is IrConstructorCall -> IrConstructorCallImpl(
        result.startOffset,
        result.endOffset,
        result.type,
        callee.symbol.cast(),
        result.typeArgumentsCount,
        result.constructorTypeArgumentsCount,
        callee.valueParameters.size,
        result.origin
      ).apply {
        copyTypeAndValueArgumentsFrom(result)
      }
      is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl(
        result.startOffset,
        result.endOffset,
        result.type,
        result.symbol,
        result.typeArgumentsCount,
        callee.valueParameters.size
      ).apply {
        copyTypeAndValueArgumentsFrom(result)
      }
      else -> throw AssertionError("Unexpected expression $result ${result.dump()}")
    }
  }
}
