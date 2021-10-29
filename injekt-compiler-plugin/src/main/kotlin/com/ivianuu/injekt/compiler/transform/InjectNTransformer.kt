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

import androidx.compose.compiler.plugins.kotlin.lower.function
import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.callableInfo
import com.ivianuu.injekt.compiler.classifierInfo
import com.ivianuu.injekt.compiler.injectNTypes
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt_shaded.Inject
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.cast

@OptIn(ObsoleteDescriptorBasedAPI::class) class InjectNTransformer(
  @Inject private val context: InjektContext,
  @Inject private val localClassCollector: LocalClassCollector,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
  private val transformedClasses = mutableSetOf<IrClass>()
  private val transformedFunctions = mutableSetOf<IrFunction>()

  fun transformIfNeeded(clazz: IrClass): IrClass {
    val info = clazz.descriptor.classifierInfo()
    if (info.injectNParameters.isEmpty()) return clazz
    if (clazz in transformedClasses) return clazz
    transformedClasses += clazz

    info.injectNParameters.forEach { parameter ->
      clazz.addField(
        fieldName = parameter.name,
        fieldType = parameter.typeRef.toIrType().typeOrNull!!
      )
    }

    return clazz
  }

  fun transformIfNeeded(function: IrFunction): IrFunction {
    val info = if (function.descriptor is PropertyAccessorDescriptor)
      function.descriptor.cast<PropertyAccessorDescriptor>().correspondingProperty.callableInfo()
    else function.descriptor.callableInfo()
    if (info.injectNParameters.isEmpty()) return function

    if (function in transformedFunctions) return function
    transformedFunctions += function

    info.injectNParameters.forEach { parameter ->
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
    val callee = transformIfNeeded(expression.symbol.owner)

    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    if (result.dispatchReceiver?.type?.let {
        (it.isFunction() || it.isSuspendFunction()) &&
            it.hasAnnotation(injektFqNames().inject2)
    } == true) {
      val type = result.dispatchReceiver!!.type as IrSimpleType
      val oldIrArguments = type.arguments

      val extraArgs = type.toKotlinType().injectNTypes().indices
        .map { makeTypeProjection(pluginContext.irBuiltIns.anyNType, Variance.INVARIANT) }

      val newIrArguments =
        oldIrArguments.subList(0, oldIrArguments.size - 1) +
            extraArgs +
            oldIrArguments.last()

      val newArgSize = oldIrArguments.size - 1 + extraArgs.size
      val functionCls = pluginContext.function(newArgSize)
      val newInvoke = functionCls.owner.functions
        .first { it.name.asString() == "invoke" }
      return IrCallImpl(
        result.startOffset,
        result.endOffset,
        result.type,
        newInvoke.symbol,
        newInvoke.typeParameters.size,
        newInvoke.valueParameters.size,
        result.origin,
        result.cast<IrCall>().superQualifierSymbol
      ).apply {
        copyTypeAndValueArgumentsFrom(result)
      }
    }

    if (result.symbol.owner !in transformedFunctions) return super.visitFunctionAccess(result)

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
      )
      else -> throw AssertionError()
    }
  }
}
