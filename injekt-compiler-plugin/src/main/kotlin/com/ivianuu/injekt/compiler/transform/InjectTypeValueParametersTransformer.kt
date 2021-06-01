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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class InjectTypeValueParametersTransformer(
  private val context: InjektContext,
  private val trace: BindingTrace,
  private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {
  val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
  private val transformedClasses = mutableSetOf<IrClass>()

  override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
    super.visitModuleFragment(declaration)
    declaration.transformCallsWithForTypeKey()
    return declaration
  }

  override fun visitClass(declaration: IrClass): IrStatement =
    super.visitClass(transformClassIfNeeded(declaration))

  override fun visitFunction(declaration: IrFunction): IrStatement =
    super.visitFunction(transformFunctionIfNeeded(declaration))

  private fun transformClassIfNeeded(clazz: IrClass): IrClass {
    if (clazz in transformedClasses) return clazz
    transformedClasses += clazz

    val info = clazz.descriptor.classifierInfo(context, trace)

    if (info.injectTypes.isEmpty()) return clazz

    var index = 0
    val injectTypeFields = info.injectTypes
      .associateWith { injectType ->
        val field = clazz.addField(
          "\$injectType${index++}",
          injectType.toIrType(pluginContext, emptyList(), context).typeOrNull!!
        )
        clazz.declarations -= field
        clazz.declarations.add(0, field)
        field
      }

    clazz.constructors.forEach { constructor ->
      val injectTypeParamsWithFields = injectTypeFields.values.associateWith { field ->
        constructor.addValueParameter(
          field.name.asString(),
          field.type
        )
      }
      (constructor.body!! as IrBlockBody).run {
        injectTypeParamsWithFields
          .toList()
          .forEachIndexed { index, (field, param) ->
            statements.add(
              index + 1,
              DeclarationIrBuilder(pluginContext, constructor.symbol).run {
                irSetField(
                  irGet(clazz.thisReceiver!!),
                  field,
                  irGet(param)
                )
              }
            )
          }
      }
    }

    return clazz
  }

  private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
    transformedFunctions[function]?.let { return it }
    if (function in transformedFunctions.values) return function

    val info = function.descriptor.callableInfo(context, trace)

    if (info.injectTypes.isEmpty()) return function

    if (function is IrConstructor) {
      if (!function.descriptor.isDeserializedDeclaration()) {
        transformClassIfNeeded(function.constructedClass)
        return function
      }

      info.injectTypes.forEachIndexed { index, injectType ->
        function.addValueParameter(
          "\$injectType${index}",
          injectType.toIrType(pluginContext, emptyList(), context).typeOrNull!!
        )
      }
      transformedFunctions[function] = function
      return function
    }

    if (function.descriptor.isDeserializedDeclaration()) {
      info.injectTypes.forEachIndexed { index, injectType ->
        function.addValueParameter(
          "\$injectType${index}",
          injectType.toIrType(pluginContext, emptyList(), context).typeOrNull!!
        )
      }
      transformedFunctions[function] = function
      return function
    }

    val transformedFunction = function.copyWithInjectTypeParams()
    transformedFunctions[function] = transformedFunction

    info.injectTypes.forEachIndexed { index, injectType ->
      transformedFunction.addValueParameter(
        "\$injectType${index}",
        injectType.toIrType(pluginContext, emptyList(), context).typeOrNull!!
      )
    }

    return transformedFunction
  }

  private fun IrFunction.copyWithInjectTypeParams(): IrFunction {
    return copy(pluginContext).apply {
      val descriptor = descriptor
      if (descriptor is PropertyGetterDescriptor &&
        annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
      ) {
        val name = JvmAbi.getterName(descriptor.correspondingProperty.name.identifier)
        annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
          .jvmNameAnnotation(name, pluginContext)
        correspondingPropertySymbol?.owner?.getter = this
      }
      if (descriptor is PropertySetterDescriptor &&
        annotations.findAnnotation(DescriptorUtils.JVM_NAME) == null
      ) {
        val name = JvmAbi.setterName(descriptor.correspondingProperty.name.identifier)
        annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
          .jvmNameAnnotation(name, pluginContext)
        correspondingPropertySymbol?.owner?.setter = this
      }

      if (this@copyWithInjectTypeParams is IrOverridableDeclaration<*>) {
        overriddenSymbols = this@copyWithInjectTypeParams.overriddenSymbols.map {
          transformFunctionIfNeeded(it.owner as IrFunction).symbol as IrSimpleFunctionSymbol
        }
      }
    }
  }

  private fun IrElement.transformCallsWithForTypeKey() {
    transform(object : IrElementTransformerVoid() {
      override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression =
        super.visitFunctionAccess(transformCallIfNeeded(expression))
    }, null)
  }

  private fun transformCallIfNeeded(
    expression: IrFunctionAccessExpression
  ): IrFunctionAccessExpression {
    val callee = expression.symbol.owner
    val transformedCallee = transformFunctionIfNeeded(callee)
    return when (expression) {
      is IrCall -> {
        IrCallImpl(
          expression.startOffset,
          expression.endOffset,
          transformedCallee.returnType,
          transformedCallee.symbol as IrSimpleFunctionSymbol,
          transformedCallee.typeParameters.size,
          transformedCallee.valueParameters.size,
          expression.origin,
          expression.superQualifierSymbol
        ).apply {
          copyTypeAndValueArgumentsFrom(expression)
        }
      }
      is IrDelegatingConstructorCallImpl -> {
        if (expression.valueArgumentsCount == transformedCallee.valueParameters.size)
          return expression
        IrDelegatingConstructorCallImpl(
          expression.startOffset,
          expression.endOffset,
          expression.type,
          expression.symbol,
          expression.typeArgumentsCount,
          transformedCallee.valueParameters.size
        ).apply {
          copyTypeAndValueArgumentsFrom(expression)
        }
      }
      is IrConstructorCall -> {
        if (expression.valueArgumentsCount == transformedCallee.valueParameters.size)
          return expression
        IrConstructorCallImpl(
          expression.startOffset,
          expression.endOffset,
          expression.type,
          expression.symbol,
          expression.typeArgumentsCount,
          expression.constructorTypeArgumentsCount,
          transformedCallee.valueParameters.size,
          expression.origin
        ).apply {
          copyTypeAndValueArgumentsFrom(expression)
        }
      }
      else -> expression
    }
  }
}
