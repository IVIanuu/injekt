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

import com.ivianuu.injekt.compiler.DISPATCH_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.EXTENSION_RECEIVER_INDEX
import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.injektIndex
import com.ivianuu.injekt.compiler.resolution.CallContext
import com.ivianuu.injekt.compiler.resolution.CallableInjectable
import com.ivianuu.injekt.compiler.resolution.CallableRef
import com.ivianuu.injekt.compiler.resolution.ComponentInjectable
import com.ivianuu.injekt.compiler.resolution.Injectable
import com.ivianuu.injekt.compiler.resolution.InjectableRequest
import com.ivianuu.injekt.compiler.resolution.InjectablesScope
import com.ivianuu.injekt.compiler.resolution.InjectionGraph
import com.ivianuu.injekt.compiler.resolution.ListInjectable
import com.ivianuu.injekt.compiler.resolution.ProviderInjectable
import com.ivianuu.injekt.compiler.resolution.ResolutionResult
import com.ivianuu.injekt.compiler.resolution.SourceKeyInjectable
import com.ivianuu.injekt.compiler.resolution.TypeKeyInjectable
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.callContext
import com.ivianuu.injekt.compiler.resolution.isSubTypeOf
import com.ivianuu.injekt.compiler.resolution.render
import com.ivianuu.injekt.compiler.uniqueKey
import com.ivianuu.injekt_shaded.Inject
import com.ivianuu.injekt_shaded.Provide
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableAccessorDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@OptIn(ObsoleteDescriptorBasedAPI::class)
class InjectCallTransformer(
  @Inject private val context: InjektContext,
  @Inject private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
  private inner class GraphContext(
    val graph: InjectionGraph.Success,
    val startOffset: Int
  ) {
    val statements = mutableListOf<IrStatement>()

    var variableIndex = 0

    @OptIn(ExperimentalStdlibApi::class)
    private val graphContextParents = buildList<InjectablesScope> {
      var current: InjectablesScope? = graph.scope.parent
      while (current != null) {
        this += current
        current = current.parent
      }
    }

    private val isInBetweenCircularDependency =
      mutableMapOf<ResolutionResult.Success.WithCandidate.Value, Boolean>()

    fun isInBetweenCircularDependency(result: ResolutionResult.Success.WithCandidate.Value): Boolean =
      isInBetweenCircularDependency.getOrPut(result) {
        val allResults = mutableListOf<ResolutionResult.Success.WithCandidate.Value>()
        var isInBetweenCircularDependency = false
        fun ResolutionResult.Success.WithCandidate.visit() {
          if (this is ResolutionResult.Success.WithCandidate.CircularDependency &&
            allResults.none { it.candidate == candidate }
          ) {
            isInBetweenCircularDependency = true
            return
          }
          if (this is ResolutionResult.Success.WithCandidate.Value) {
            allResults += this
            dependencyResults.forEach { (_, dependencyResult) ->
              if (!isInBetweenCircularDependency &&
                dependencyResult is ResolutionResult.Success.WithCandidate
              )
                dependencyResult.visit()
            }
          }
        }
        result.dependencyResults.forEach {
          if (!isInBetweenCircularDependency)
            it.value.safeAs<ResolutionResult.Success.WithCandidate>()?.visit()
        }
        isInBetweenCircularDependency
      }

    fun mapScopeIfNeeded(scope: InjectablesScope) =
      if (scope in graphContextParents) graph.scope else scope
        .allScopes.last { !it.isTypeScope }
  }

  private inner class ScopeContext(
    val parent: ScopeContext?,
    val graphContext: GraphContext,
    val scope: InjectablesScope,
    val irScope: Scope,
    val component: IrClass?
  ) {
    val symbol = irScope.scopeOwnerSymbol
    val functionWrappedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
    val cachedExpressions = mutableMapOf<TypeRef, ScopeContext.() -> IrExpression>()
    val scopedExpressions = mutableMapOf<Any, ScopeContext.() -> IrExpression>()
    val statements =
      if (scope == graphContext.graph.scope) graphContext.statements else mutableListOf()
    val initializingExpressions: MutableMap<Injectable, InjectableExpression> =
      parent?.initializingExpressions ?: mutableMapOf()
    val parameterMap: MutableMap<ParameterDescriptor, IrValueParameter> =
      parent?.parameterMap ?: mutableMapOf()

    fun findScopeContext(scopeToFind: InjectablesScope): ScopeContext {
      val finalScope = graphContext.mapScopeIfNeeded(scopeToFind)
      if (finalScope == scope) return this@ScopeContext
      return parent?.findScopeContext(finalScope)
        ?: error("wtf 1")
    }

    fun expressionFor(result: ResolutionResult.Success.WithCandidate): IrExpression {
      val scopeContext = findScopeContext(result.scope)
      return scopeContext.expressionForImpl(result)
    }

    private fun expressionForImpl(result: ResolutionResult.Success.WithCandidate): IrExpression {
      initializingExpressions[result.candidate]?.run { return get() }
      val expression = InjectableExpression(result)
      initializingExpressions[result.candidate] = expression
      val irExpression = expression.run { get() }
      initializingExpressions -= result.candidate
      return irExpression
    }

    fun pushComponentReceivers(expression: () -> IrExpression) {
      component!!.superTypes.dropLast(1).forEach {
        receiverAccessors.push(it.classOrNull!!.owner to expression)
      }
    }

    fun popComponentReceivers() {
      repeat(component!!.superTypes.size - 1) {
        receiverAccessors.pop()
      }
    }
  }

  private fun IrFunctionAccessExpression.inject(
    context: ScopeContext,
    results: Map<InjectableRequest, ResolutionResult.Success>
  ) {
    results
      .forEach { (request, result) ->
        if (result !is ResolutionResult.Success.WithCandidate) return@forEach
        val expression = context.expressionFor(result)
        when (request.parameterIndex) {
          DISPATCH_RECEIVER_INDEX -> dispatchReceiver = expression
          EXTENSION_RECEIVER_INDEX -> extensionReceiver = expression
          else -> {
            putValueArgument(
              symbol.owner
                .valueParameters
                .first { it.descriptor.injektIndex() == request.parameterIndex }
                .index,
              expression
            )
          }
        }
      }
  }

  private inner class InjectableExpression(private val result: ResolutionResult.Success.WithCandidate) {
    private var block: IrBlock? = null
    private var tmpVariable: IrVariable? = null
    private var finalExpression: IrExpression? = null

    private var initializing = false

    fun ScopeContext.get(): IrExpression {
      if (initializing) {
        if (block == null) {
          val resultType = result.candidate.type.toIrType()
            .typeOrNull!!
          block = DeclarationIrBuilder(pluginContext, symbol)
            .irBlock(resultType = resultType) {
              tmpVariable = irTemporary(
                value = irNull(),
                isMutable = true,
                irType = resultType.makeNullable(),
                nameHint = "${graphContext.variableIndex++}"
              )
            } as IrBlock
        }
        return DeclarationIrBuilder(pluginContext, symbol)
          .irGet(tmpVariable!!)
      }

      result as ResolutionResult.Success.WithCandidate.Value

      finalExpression?.let { return it }

      initializing = true

      val rawExpression = cacheExpressionIfNeeded(result) {
        wrapExpressionInFunctionIfNeeded(result) {
          scopeExpressionIfNeeded(result) {
            when (result.candidate) {
              is CallableInjectable -> callableExpression(result, result.candidate.cast())
              is ComponentInjectable -> componentExpression(result, result.candidate.cast())
              is ProviderInjectable -> providerExpression(result, result.candidate.cast())
              is ListInjectable -> listExpression(result, result.candidate.cast())
              is SourceKeyInjectable -> sourceKeyExpression()
              is TypeKeyInjectable -> typeKeyExpression(result, result.candidate.cast())
            }
          }
        }
      }

      initializing = false

      finalExpression = if (block == null) rawExpression else {
        with(DeclarationIrBuilder(pluginContext, symbol)) {
          block!!.statements += irSet(tmpVariable!!.symbol, rawExpression)
          block!!.statements += irGet(tmpVariable!!)
        }
        block!!
      }

      return finalExpression!!
    }
  }

  private fun ScopeContext.scopeExpressionIfNeeded(
    result: ResolutionResult.Success.WithCandidate.Value,
    rawExpressionProvider: () -> IrExpression
  ): IrExpression {
    val scopeComponent = result.candidate.scopeComponentType
      ?: return rawExpressionProvider()
    val scope = scope.allScopes.last { it.componentType == scopeComponent }
    return with(findScopeContext(scope)) {
      scopedExpressions.getOrPut(result.candidate.usageKey) {
        val index = graphContext.variableIndex++
        val lockField = component!!.addField(
          "_${index}Lock",
          pluginContext.irBuiltIns.anyType,
          DescriptorVisibilities.PRIVATE
        ).apply {
          initializer = DeclarationIrBuilder(pluginContext, symbol).run {
            irExprBody(irCall(pluginContext.irBuiltIns.anyClass.constructors.single()))
          }
        }
        val instanceField = component.addField(
          "_${index}Instance",
          pluginContext.irBuiltIns.anyNType,
          DescriptorVisibilities.PRIVATE
        ).apply {
          initializer = DeclarationIrBuilder(pluginContext, symbol).run {
            irExprBody(irGetField(irGet(component.thisReceiver!!), lockField))
          }
        }
        component.declarations.remove(lockField)
        component.declarations.remove(instanceField)
        component.declarations.add(0, instanceField)
        component.declarations.add(0, lockField)

        val scopeReceiverParameter =
          scopeComponent.classifier.descriptor!!.cast<ClassDescriptor>().thisAsReceiverParameter

        val expression: ScopeContext.() -> IrExpression = {
          DeclarationIrBuilder(pluginContext, symbol).run {
            irBlock {
              val tmp = irTemporary(
                value = irGetField(receiverExpression(scopeReceiverParameter), instanceField),
                nameHint = "${graphContext.variableIndex++}",
                isMutable = true
              )

              +irIfThenElse(
                result.candidate.type.toIrType().typeOrNull!!,
                irEqeqeq(irGet(tmp), irGetField(receiverExpression(scopeReceiverParameter), lockField)),
                irCall(
                  pluginContext.referenceFunctions(
                    injektFqNames().commonPackage.child("synchronized".asNameId())
                  ).single()
                ).apply {
                  putTypeArgument(0, result.candidate.type.toIrType().typeOrNull!!)
                  putValueArgument(0, irGetField(receiverExpression(scopeReceiverParameter), lockField))
                  putValueArgument(
                    1,
                    irLambda(
                      pluginContext.irBuiltIns.function(0)
                        .typeWith(result.candidate.type.toIrType().typeOrNull!!),
                      parameterNameProvider = { "p${graphContext.variableIndex++}" }
                    ) {
                      irBlock {
                        +irSet(tmp.symbol, irGetField(receiverExpression(scopeReceiverParameter), instanceField))

                        +irIfThen(
                          irEqeqeq(irGet(tmp), irGetField(receiverExpression(scopeReceiverParameter), lockField)),
                          irBlock {
                            +irSet(tmp.symbol, rawExpressionProvider())
                            +irSetField(receiverExpression(scopeReceiverParameter), instanceField, irGet(tmp))
                          }
                        )

                        +irGet(tmp)
                      }
                    }
                  )
                },
                irGet(tmp)
              )
            }
          }
        }

        expression
      }
    }.invoke(this)
  }

  private fun ResolutionResult.Success.WithCandidate.Value.shouldWrap(
    context: GraphContext
  ): Boolean = candidate !is ProviderInjectable &&
      (dependencyResults.isNotEmpty() || candidate.scopeComponentType != null) &&
      context.graph.usages[this.usageKey]!!.size > 1 &&
      !context.isInBetweenCircularDependency(this)

  private fun ScopeContext.wrapExpressionInFunctionIfNeeded(
    result: ResolutionResult.Success.WithCandidate.Value,
    rawExpressionProvider: () -> IrExpression
  ): IrExpression = if (!result.shouldWrap(graphContext)) rawExpressionProvider()
  else with(result.safeAs<ResolutionResult.Success.WithCandidate.Value>()
    ?.highestScope?.let { findScopeContext(it) } ?: this) {
    functionWrappedExpressions.getOrPut(result.candidate.type) {
      val function = IrFactoryImpl.buildFun {
        origin = IrDeclarationOrigin.DEFINED
        name = "function${graphContext.variableIndex++}".asNameId()
        returnType = result.candidate.type.toIrType()
          .typeOrNull!!
        visibility = if (component != null) DescriptorVisibilities.PUBLIC
        else DescriptorVisibilities.LOCAL
        isSuspend = scope.callContext == CallContext.SUSPEND
      }.apply {
        parent = component ?: irScope.getLocalDeclarationParent()

        if (result.candidate.callContext == CallContext.COMPOSABLE) {
          annotations = annotations + DeclarationIrBuilder(pluginContext, symbol)
            .irCallConstructor(
              pluginContext.referenceConstructors(injektFqNames().composable)
                .single(),
              emptyList()
            )
        }

        if (component != null)
          addDispatchReceiver { type = component.defaultType }

        this.body = DeclarationIrBuilder(pluginContext, symbol).run {
          irBlockBody {
            if (dispatchReceiverParameter != null)
              pushComponentReceivers { irGet(dispatchReceiverParameter!!) }
            +irReturn(rawExpressionProvider())
            if (dispatchReceiverParameter != null)
              popComponentReceivers()
          }
        }

        if (component != null) component.declarations += this
        else statements += this
      }

      val expression: ScopeContext.() -> IrExpression = {
        DeclarationIrBuilder(pluginContext, symbol)
          .irCall(
            function.symbol,
            result.candidate.type.toIrType().typeOrNull!!
          ).apply {
            if (this@with.component != null)
              dispatchReceiver = receiverExpression(
                this@with.scope.componentType!!.classifier.descriptor!!
                  .cast<ClassDescriptor>().thisAsReceiverParameter
              )
          }
      }
      expression
    }
  }.invoke(this)

  private fun ResolutionResult.Success.WithCandidate.Value.shouldCache(
    context: ScopeContext
  ): Boolean = candidate is ProviderInjectable &&
      context.graphContext.graph.usages[this.usageKey]!!.count { !it.isInline } > 1 &&
      !context.graphContext.isInBetweenCircularDependency(this) &&
      context.findScopeContext(highestScope).component == null

  private fun ScopeContext.cacheExpressionIfNeeded(
    result: ResolutionResult.Success.WithCandidate.Value,
    rawExpressionProvider: () -> IrExpression
  ): IrExpression {
    if (!result.shouldCache(this)) return rawExpressionProvider()
    return with(findScopeContext(result.highestScope)) {
      cachedExpressions.getOrPut(result.candidate.type) {
        val variable = irScope.createTemporaryVariable(
          rawExpressionProvider(),
          nameHint = "${graphContext.variableIndex++}"
        )
        statements += variable
        val expression: ScopeContext.() -> IrExpression = {
          DeclarationIrBuilder(pluginContext, symbol)
            .irGet(variable)
        }
        expression
      }
    }.invoke(this)
  }

  private fun ScopeContext.objectExpression(type: TypeRef): IrExpression =
    DeclarationIrBuilder(pluginContext, symbol)
      .irGetObject(pluginContext.referenceClass(type.classifier.fqName)!!)

  private fun ScopeContext.componentExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ComponentInjectable
  ): IrExpression = DeclarationIrBuilder(pluginContext, symbol).irBlock {
    val clazz = IrFactoryImpl.buildClass {
      name = injectable.callableFqName.shortName()
      visibility = DescriptorVisibilities.LOCAL
    }.apply clazz@{
      parent = scope.getLocalDeclarationParent()
      createImplicitParameterDeclarationWithWrappedDescriptor()
      superTypes += injectable.type.toIrType().typeOrNull!!
      superTypes += injectable.entryPoints.map { it.toIrType().typeOrNull!! }
      superTypes += this@InjectCallTransformer.context.disposableType.defaultType
        .toIrType().typeOrNull!!

      val componentScope = ScopeContext(
        this@componentExpression,
        graphContext, injectable.componentScope, scope, this@clazz
      )

      componentScope.pushComponentReceivers { irGet(thisReceiver!!) }

      injectable.requestCallables.forEach { requestCallable ->
        fun IrSimpleFunction.setupFunction() {
          if (requestCallable.callable.callContext() == CallContext.COMPOSABLE) {
            annotations += DeclarationIrBuilder(pluginContext, symbol)
              .irCallConstructor(
                pluginContext.referenceConstructors(injektFqNames().composable)
                  .single(),
                emptyList()
              )
          }

          addDispatchReceiver { type = defaultType }
          if (requestCallable.callable.extensionReceiverParameter != null) {
            addExtensionReceiver(
              requestCallable.parameterTypes[EXTENSION_RECEIVER_INDEX]!!
                .toIrType().typeOrNull!!
            )
          }

          requestCallable.callable.valueParameters.forEach { parameter ->
            addValueParameter(
              parameter.name.asString(),
              requestCallable.parameterTypes[parameter.injektIndex()]!!
                .toIrType().typeOrNull!!
            )
          }

          body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
            val dependencyScopeContext = ScopeContext(
              componentScope,
              graphContext, injectable.dependencyScopesByRequestCallable[requestCallable]!!, scope,
              null
            )
            componentScope.pushComponentReceivers { irGet(dispatchReceiverParameter!!) }
            val expression = with(dependencyScopeContext) {
              val request = injectable.requestsByRequestCallables[requestCallable]!!
              val requestResult = result.dependencyResults[request]!!
              if (requestResult is ResolutionResult.Success.WithCandidate.Value) {
                dependencyScopeContext.scope.initialInjectables
                  .map { it.callable as ParameterDescriptor }
                  .zip(listOfNotNull(extensionReceiverParameter) + valueParameters)
                  .forEach { (a, b) -> parameterMap[a] = b }
                expressionFor(requestResult)
                  .also {
                    dependencyScopeContext.scope.initialInjectables.forEach {
                      parameterMap -= it.callable.cast<ParameterDescriptor>()
                    }
                  }
              } else {
                irCall(
                  overriddenSymbols.single().owner,
                  null,
                  superTypes.first().classOrNull!!
                ).apply {
                  dispatchReceiver = irGet(dispatchReceiverParameter!!)
                  extensionReceiverParameter?.let {
                    extensionReceiver = irGet(it)
                  }
                  valueParameters.forEach {
                    putValueArgument(it.index, irGet(it))
                  }
                }
              }
            }
            componentScope.popComponentReceivers()
            dependencyScopeContext.statements.forEach { +it }
            +irReturn(expression)
          }
        }

        if (requestCallable.callable is PropertyDescriptor) {
          addProperty {
            name = requestCallable.callable.name
            visibility = requestCallable.callable.visibility
          }.apply {
            addGetter {
              returnType = requestCallable.type.toIrType().typeOrNull!!
              visibility = requestCallable.callable.getter!!.visibility
            }.apply {
              overriddenSymbols = overriddenSymbols + requestCallable.callable.cast<PropertyDescriptor>()
                .irProperty()
                .getter!!
                .symbol
                .cast<IrSimpleFunctionSymbol>()
              setupFunction()
            }
          }
        } else {
          addFunction {
            returnType = requestCallable.type.toIrType().typeOrNull!!
            name = requestCallable.callable.name
            isSuspend = requestCallable.callable.callContext() == CallContext.SUSPEND
            visibility = requestCallable.callable.visibility
          }.apply {
            overriddenSymbols = overriddenSymbols + requestCallable.callable.cast<FunctionDescriptor>()
              .irFunction()
              .symbol
              .cast<IrSimpleFunctionSymbol>()
            setupFunction()
          }
        }
      }

      val observersResult = result.dependencyResults[injectable.componentObserversRequest]
        .safeAs<ResolutionResult.Success.WithCandidate>()

      val observersField = if (observersResult == null) null
      else addField(
        "${injectable.componentObserversRequest.parameterName}${graphContext.variableIndex++}",
        injectable.componentObserversRequest.type.toIrType().typeOrNull!!,
        DescriptorVisibilities.PRIVATE
      ).apply {
        initializer = DeclarationIrBuilder(pluginContext, symbol).run {

          irExprBody(
            with(componentScope) {
              expressionFor(observersResult)
            }
          )
        }
      }
      fun forEachObserver(
        observerFunctionName: String,
        thisExpression: () -> IrExpression
      ): IrExpression = irCall(
        pluginContext.referenceFunctions(
          FqName("kotlin.collections.forEach")
        ).first {
          it.owner.extensionReceiverParameter?.type?.classFqName ==
              StandardNames.FqNames.iterable
        }
      ).apply {
        putTypeArgument(
          0,
          observersField!!.type.cast<IrSimpleType>().arguments.single().typeOrNull
        )

        extensionReceiver = irGetField(thisExpression(), observersField)

        putValueArgument(
          0,
          irLambda(
            pluginContext.irBuiltIns.function(1)
              .typeWith(
                observersField.type.cast<IrSimpleType>().arguments.single().typeOrNull!!,
                pluginContext.irBuiltIns.unitType
              ),
            parameterNameProvider = { "p${graphContext.variableIndex++}" }
          ) {
            irCall(
              observersField.type.cast<IrSimpleType>().arguments.single().typeOrNull!!
                .classOrNull!!
                .functions
                .single { it.owner.name.asString() == observerFunctionName }
            ).apply {
              dispatchReceiver = irGet(it.valueParameters.single())
            }
          }
        )
      }

      addConstructor {
        returnType = defaultType
        isPrimary = true
        visibility = DescriptorVisibilities.PUBLIC
      }.apply {
        body = DeclarationIrBuilder(
          pluginContext,
          symbol
        ).irBlockBody {
          +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
          +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            this@clazz.symbol,
            context.irBuiltIns.unitType
          )

          if (observersField != null) {
            +forEachObserver(
              observerFunctionName = "init",
              thisExpression = { irGet(thisReceiver!!) }
            )
          }
        }
      }

      addFunction {
        returnType = pluginContext.irBuiltIns.unitType
        name = "dispose".asNameId()
      }.apply {
        addDispatchReceiver { type = defaultType }
        overriddenSymbols = overriddenSymbols + this@InjectCallTransformer.context.disposableType
          .defaultType.toIrType().typeOrNull!!.classOrNull!!
          .functions
          .single { it.owner.name == name }
        body = DeclarationIrBuilder(pluginContext, symbol).run {
          irBlockBody {
            if (observersField != null) {
              +forEachObserver(
                observerFunctionName = "dispose",
                thisExpression = { irGet(dispatchReceiverParameter!!) }
              )
              irSetField(irGet(dispatchReceiverParameter!!), observersField, irNull())
            }

            val disposableClass = pluginContext.referenceClass(injektFqNames().disposable)!!
            fields
              .filter { it.name.asString().endsWith("Instance") }
              .forEach { field ->
                +irIfThen(
                  irIs(
                    irGetField(irGet(dispatchReceiverParameter!!), field),
                    disposableClass.defaultType
                  ),
                  irCall(
                    disposableClass
                      .functions
                      .single { it.owner.name.asString() == "dispose" }
                  ).apply {
                    dispatchReceiver = irAs(
                      irGetField(irGet(dispatchReceiverParameter!!), field),
                      disposableClass.defaultType
                    )
                  }
                )
                +irSetField(irGet(dispatchReceiverParameter!!), field, irNull())
              }
          }
        }
      }

      componentScope.popComponentReceivers()
    }

    +clazz
    +irCall(clazz.constructors.single())
  }

  private fun ScopeContext.providerExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ProviderInjectable
  ): IrExpression = DeclarationIrBuilder(pluginContext, symbol)
    .irLambda(
      injectable.type.toIrType().typeOrNull!!,
      parameterNameProvider = { "p${graphContext.variableIndex++}" }
    ) { function ->
      when (val dependencyResult = result.dependencyResults.values.single()) {
        is ResolutionResult.Success.DefaultValue -> return@irLambda irNull()
        is ResolutionResult.Success.WithCandidate -> {
          val dependencyScopeContext = ScopeContext(
            this@providerExpression, graphContext,
            injectable.dependencyScopes.values.single(), scope, null
          )
          val expression = with(dependencyScopeContext) {
            injectable.parameterDescriptors
              .zip(function.valueParameters)
              .forEach { (a, b) -> parameterMap[a] = b }
            expressionFor(dependencyResult)
              .also {
                injectable.parameterDescriptors.forEach {
                  parameterMap -= it
                }
              }
          }
          if (dependencyScopeContext.statements.isEmpty()) expression
          else {
            irBlock {
              dependencyScopeContext.statements.forEach { +it }
              +expression
            }
          }
        }
      }
    }

  private val mutableListOf = pluginContext.referenceFunctions(
    FqName("kotlin.collections.mutableListOf")
  ).single { it.owner.valueParameters.isEmpty() }
  private val listAdd = mutableListOf.owner.returnType
    .classOrNull!!
    .owner
    .functions
    .single { it.name.asString() == "add" && it.valueParameters.size == 1 }
  private val listAddAll = mutableListOf.owner.returnType
    .classOrNull!!
    .owner
    .functions
    .single { it.name.asString() == "addAll" && it.valueParameters.size == 1 }
  private val listOf = pluginContext.referenceFunctions(
    FqName("kotlin.collections.listOf")
  ).single { it.owner.valueParameters.singleOrNull()?.isVararg == false }
  private val iterableToList = pluginContext.referenceFunctions(
    FqName("kotlin.collections.toList")
  ).single {
    it.owner.extensionReceiverParameter?.type?.classifierOrNull?.descriptor ==
        pluginContext.builtIns.iterable
  }

  private fun ScopeContext.listExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: ListInjectable
  ): IrExpression = when (injectable.dependencies.size) {
    1 -> {
      val singleDependency = result.dependencyResults.values.single()
        .cast<ResolutionResult.Success.WithCandidate.Value>()
      when {
        singleDependency.candidate.type.isSubTypeOf(injectable.type) ->
          expressionFor(result.dependencyResults.values.single().cast())
        singleDependency.candidate.type.isSubTypeOf(injectable.collectionElementType) -> {
          DeclarationIrBuilder(pluginContext, symbol)
            .irCall(iterableToList)
            .apply {
              extensionReceiver =
                expressionFor(result.dependencyResults.values.single().cast())
              putTypeArgument(
                0,
                injectable.singleElementType.toIrType().typeOrNull
              )
            }
        }
        else -> DeclarationIrBuilder(pluginContext, symbol)
          .irCall(listOf)
          .apply {
            putTypeArgument(
              0,
              injectable.singleElementType.toIrType().typeOrNull
            )
            putValueArgument(
              0,
              expressionFor(result.dependencyResults.values.single().cast())
            )
          }
      }
    }
    else -> {
      DeclarationIrBuilder(pluginContext, symbol).irBlock {
        val tmpSet = irTemporary(
          irCall(mutableListOf)
            .apply {
              putTypeArgument(
                0,
                injectable.singleElementType.toIrType().typeOrNull
              )
            },
          nameHint = "${graphContext.variableIndex++}"
        )

        result.dependencyResults
          .forEach { (_, dependency) ->
            if (dependency !is ResolutionResult.Success.WithCandidate.Value)
              return@forEach
            if (dependency.candidate.type.isSubTypeOf(injectable.collectionElementType)) {
              +irCall(listAddAll).apply {
                dispatchReceiver = irGet(tmpSet)
                putValueArgument(0, expressionFor(dependency))
              }
            } else {
              +irCall(listAdd).apply {
                dispatchReceiver = irGet(tmpSet)
                putValueArgument(0, expressionFor(dependency))
              }
            }
          }

        +irGet(tmpSet)
      }
    }
  }

  private val sourceKeyConstructor = pluginContext.referenceClass(injektFqNames().sourceKey)
    ?.constructors?.single()

  private fun ScopeContext.sourceKeyExpression(): IrExpression =
    DeclarationIrBuilder(pluginContext, symbol).run {
      irCall(sourceKeyConstructor!!).apply {
        putValueArgument(
          0,
          irString(
            buildString {
              append(currentFile.name)
              append(":")
              append(currentFile.fileEntry.getLineNumber(graphContext.startOffset) + 1)
              append(":")
              append(currentFile.fileEntry.getColumnNumber(graphContext.startOffset))
            }
          )
        )
      }
    }

  private val typeKey = pluginContext.referenceClass(injektFqNames().typeKey)
  private val typeKeyValue = typeKey?.owner?.properties
    ?.single { it.name.asString() == "value" }
  private val typeKeyConstructor = typeKey?.constructors?.single()

  private fun ScopeContext.typeKeyExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: TypeKeyInjectable
  ): IrExpression = DeclarationIrBuilder(pluginContext, symbol).run {
    val expressions = mutableListOf<IrExpression>()
    var currentString = ""
    fun commitCurrentString() {
      if (currentString.isNotEmpty()) {
        expressions += irString(currentString)
        currentString = ""
      }
    }

    fun appendToCurrentString(value: String) {
      currentString += value
    }

    fun appendTypeParameterExpression(expression: IrExpression) {
      commitCurrentString()
      expressions += expression
    }

    injectable.type.arguments.single().render(
      renderType = { typeToRender ->
        if (!typeToRender.classifier.isTypeParameter) true else {
          appendTypeParameterExpression(
            irCall(typeKeyValue!!.getter!!).apply {
              dispatchReceiver = expressionFor(
                result.dependencyResults.values.single {
                  it is ResolutionResult.Success.WithCandidate &&
                      it.candidate.type.arguments.single().classifier == typeToRender.classifier
                }.cast()
              )
            }
          )
          false
        }
      },
      append = { appendToCurrentString(it) }
    )

    commitCurrentString()

    val stringExpression = if (expressions.size == 1) {
      expressions.single()
    } else {
      val stringPlus = pluginContext.irBuiltIns.stringClass
        .functions
        .map { it.owner }
        .first { it.name.asString() == "plus" }
      expressions.reduce { acc, expression ->
        irCall(stringPlus).apply {
          dispatchReceiver = acc
          putValueArgument(0, expression)
        }
      }
    }

    irCall(typeKeyConstructor!!).apply {
      putTypeArgument(0, injectable.type.arguments.single().toIrType().cast())
      putValueArgument(0, stringExpression)
    }
  }

  private fun ScopeContext.callableExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable
  ): IrExpression = when (injectable.callable.callable) {
    is ClassConstructorDescriptor -> classExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is PropertyDescriptor -> propertyExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is FunctionDescriptor -> functionExpression(
      result,
      injectable,
      injectable.callable.callable
    )
    is ReceiverParameterDescriptor -> if (injectable.callable.type.classifier.isObject)
      objectExpression(injectable.callable.type)
    else parameterExpression(injectable.callable.callable, injectable)
    is ValueParameterDescriptor -> parameterExpression(injectable.callable.callable, injectable)
    is VariableDescriptor -> variableExpression(injectable.callable.callable, injectable)
    else -> error("Unsupported callable $injectable")
  }

  private fun ScopeContext.classExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: ClassConstructorDescriptor
  ): IrExpression = if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
    val clazz = pluginContext.referenceClass(descriptor.constructedClass.fqNameSafe)!!
    DeclarationIrBuilder(pluginContext, symbol)
      .irGetObject(clazz)
  } else {
    val constructor = descriptor.irConstructor()
    DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        constructor.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@classExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.propertyExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: PropertyDescriptor
  ): IrExpression {
    val property = descriptor.irProperty()
    val getter = property.getter!!
    return DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        getter.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@propertyExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.functionExpression(
    result: ResolutionResult.Success.WithCandidate.Value,
    injectable: CallableInjectable,
    descriptor: FunctionDescriptor
  ): IrExpression {
    val function = descriptor.irFunction()
    return DeclarationIrBuilder(pluginContext, symbol)
      .irCall(
        function.symbol,
        injectable.type.toIrType().typeOrNull!!
      )
      .apply {
        fillTypeParameters(injectable.callable)
        inject(this@functionExpression, result.dependencyResults)
      }
  }

  private fun ScopeContext.receiverExpression(
    descriptor: ParameterDescriptor
  ) = receiverAccessors.last {
    descriptor.type.constructor.declarationDescriptor == it.first.descriptor
  }.second()

  private fun ScopeContext.parameterExpression(
    descriptor: ParameterDescriptor,
    injectable: CallableInjectable
  ): IrExpression =
    when (val containingDeclaration = descriptor.containingDeclaration) {
      is ClassDescriptor -> receiverExpression(descriptor)
      is ClassConstructorDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          containingDeclaration.irConstructor()
            .allParameters
            .single { it.name == descriptor.name }
            .symbol
        )
      is FunctionDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          (parameterMap[descriptor] ?: containingDeclaration.irFunction()
            .let { function ->
              function.allParameters
                .filter { it != function.dispatchReceiverParameter }
            }
            .single { it.index == (descriptor as? ValueParameterDescriptor)?.index ?: -1 })
            .symbol
        )
      is PropertyDescriptor -> DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          parameterMap[descriptor]?.symbol ?: containingDeclaration.irProperty()
            .getter!!.extensionReceiverParameter!!.symbol
        )
      else -> error("Unexpected parent $descriptor $containingDeclaration")
    }

  private fun IrFunctionAccessExpression.fillTypeParameters(callable: CallableRef) {
    callable
      .typeArguments
      .values
      .forEachIndexed { index, typeArgument ->
        putTypeArgument(index, typeArgument.toIrType().typeOrNull)
      }
  }

  private fun ScopeContext.variableExpression(
    descriptor: VariableDescriptor,
    injectable: CallableInjectable
  ): IrExpression {
    return if (descriptor is LocalVariableDescriptor && descriptor.isDelegated) {
      val localFunction = localFunctions.single { candidateFunction ->
        candidateFunction.descriptor
          .safeAs<LocalVariableAccessorDescriptor.Getter>()
          ?.correspondingVariable == descriptor
      }
      DeclarationIrBuilder(pluginContext, symbol)
        .irCall(
          localFunction.symbol,
          injectable.type.toIrType().typeOrNull!!
        )
    } else {
      DeclarationIrBuilder(pluginContext, symbol)
        .irGet(
          injectable.type.toIrType().typeOrNull!!,
          localVariables.single { it.descriptor == descriptor }.symbol
        )
    }
  }

  private fun ClassConstructorDescriptor.irConstructor(): IrConstructor {
    if (constructedClass.visibility == DescriptorVisibilities.LOCAL) {
      return localClasses
        .single { it.descriptor.fqNameSafe == constructedClass.fqNameSafe }
        .constructors
        .single { it.descriptor.uniqueKey() == uniqueKey() }
    }
    return pluginContext.referenceConstructors(constructedClass.fqNameSafe)
      .single { it.descriptor.uniqueKey() == uniqueKey() }
      .owner
  }

  private fun FunctionDescriptor.irFunction(): IrFunction {
    if (visibility == DescriptorVisibilities.LOCAL) {
      return localFunctions.single {
        it.descriptor.uniqueKey() == uniqueKey()
      }
    }

    if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
        ?.visibility == DescriptorVisibilities.LOCAL) {
      return localClasses.flatMap { it.declarations }
        .single { it.descriptor.uniqueKey() == uniqueKey() }
        .cast()
    }

    return pluginContext.referenceFunctions(fqNameSafe)
      .single { it.descriptor.uniqueKey() == uniqueKey() }
      .owner
  }

  private fun PropertyDescriptor.irProperty(): IrProperty {
    if (containingDeclaration.safeAs<DeclarationDescriptorWithVisibility>()
        ?.visibility == DescriptorVisibilities.LOCAL) {
      return localClasses.flatMap { it.declarations }
        .single { it.descriptor.uniqueKey() == uniqueKey() }
        .cast()
    }

    return pluginContext.referenceProperties(fqNameSafe)
      .single { it.descriptor.uniqueKey() == uniqueKey() }
      .owner
  }

  private val receiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

  private val localVariables = mutableListOf<IrVariable>()
  private val localFunctions = mutableListOf<IrFunction>()
  @Provide private val localClasses = mutableListOf<IrClass>()

  override fun visitClassNew(declaration: IrClass): IrStatement {
    receiverAccessors.push(
      declaration to {
        DeclarationIrBuilder(pluginContext, declaration.symbol)
          .irGet(declaration.thisReceiver!!)
      }
    )
    if (declaration.isLocal) localClasses += declaration
    val result = super.visitClassNew(declaration)
    receiverAccessors.pop()
    return result
  }

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
    if (dispatchReceiver != null) {
      receiverAccessors.push(
        dispatchReceiver to {
          DeclarationIrBuilder(pluginContext, declaration.symbol)
            .irGet(declaration.dispatchReceiverParameter!!)
        }
      )
    }
    val extensionReceiver = declaration.extensionReceiverParameter?.type?.classOrNull?.owner
    if (extensionReceiver != null) {
      receiverAccessors.push(
        extensionReceiver to {
          DeclarationIrBuilder(pluginContext, declaration.symbol)
            .irGet(declaration.extensionReceiverParameter!!)
        }
      )
    }
    if (declaration.isLocal) localFunctions += declaration
    val result = super.visitFunctionNew(declaration)
    if (dispatchReceiver != null) receiverAccessors.pop()
    if (extensionReceiver != null) receiverAccessors.pop()
    return result
  }

  override fun visitVariable(declaration: IrVariable): IrStatement =
    super.visitVariable(declaration)
      .also { localVariables += declaration }

  override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
    val result = super.visitFunctionAccess(expression) as IrFunctionAccessExpression

    val graph = pluginContext.bindingContext[
        InjektWritableSlices.INJECTION_GRAPH,
        SourcePosition(currentFile.fileEntry.name, result.startOffset, result.endOffset)
    ] ?: return result

    // some ir transformations reuse the start and end offsets
    // we ensure that were not transforming wrong calls
    if (graph.callee.callable.uniqueKey() != result.symbol.owner.descriptor.uniqueKey())
      return result

    return DeclarationIrBuilder(pluginContext, result.symbol)
      .irBlock {
        val graphContext = GraphContext(graph, result.startOffset)
        try {
          ScopeContext(
            parent = null,
            graphContext = graphContext,
            scope = graph.scope,
            irScope = scope,
            component = null
          ).run { result.inject(this, graph.results) }
        } catch (e: Throwable) {
          throw RuntimeException("Wtf ${expression.dump()}", e)
        }
        graphContext.statements.forEach { +it }
        +result
      }
  }
}
