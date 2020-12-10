package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.extractGivensOfCallable
import com.ivianuu.injekt.compiler.extractGivensOfDeclaration
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class GivenCallTransformer(
    private val declarationStore: DeclarationStore,
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    private abstract inner class Scope(private val parent: Scope?) {

        private val expressionsByType = mutableMapOf<TypeRef, () -> IrExpression>()

        fun fillGivens(call: IrFunctionAccessExpression) {
            val callee = call.symbol.owner
            val calleeDescriptor = callee.descriptor
            val givenInfo = declarationStore.givenInfoForCallable(calleeDescriptor)

            if (givenInfo.requiredGivens.isNotEmpty() || givenInfo.givensWithDefault.isNotEmpty()) {
                val substitutionMap = callee.typeParameters
                    .map { it.symbol }
                    .zip((0 until call.typeArgumentsCount).map { call.getTypeArgument(it)!! })
                    .toMap()
                callee
                    .valueParameters
                    .filter { it.type.hasAnnotation(InjektFqNames.Given) }
                    .filter { call.getValueArgument(it.index) == null }
                    .map {
                        it to getExpressionForType(
                            it.type.substitute(substitutionMap)
                                .toTypeRef(),
                            call.symbol
                        )
                    }
                    .forEach { call.putValueArgument(it.first.index, it.second) }
            }
        }

        private fun getExpressionForType(type: TypeRef, symbol: IrSymbol): IrExpression {
            return expressionsByType.getOrPut(type) {
                val callable = givensFor(type).singleOrNull() ?: error("Wtf $type")
                val expression: () -> IrExpression = {
                    when (callable) {
                        is ConstructorDescriptor -> classExpression(type, callable, symbol)
                        is PropertyDescriptor -> propertyExpression(type, callable, symbol)
                        is FunctionDescriptor -> functionExpression(type, callable, symbol)
                        is ReceiverParameterDescriptor -> parameterExpression(callable, symbol)
                        is ValueParameterDescriptor -> parameterExpression(callable, symbol)
                        else -> error("Unsupported callable $callable")
                    }
                }
                expression
            }()
        }

        private fun classExpression(
            type: TypeRef,
            descriptor: ConstructorDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            return if (descriptor.constructedClass.kind == ClassKind.OBJECT) {
                val clazz =
                    pluginContext.referenceClass(descriptor.constructedClass.fqNameSafe)!!
                DeclarationIrBuilder(pluginContext, symbol)
                    .irGetObject(clazz)
            } else {
                val constructor =
                    pluginContext.referenceConstructors(descriptor.constructedClass.fqNameSafe)
                        .single()
                        .owner
                DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(constructor.symbol)
                    .apply {
                        val substitutionMap = getSubstitutionMap(
                            listOf(type to constructor.returnType.toTypeRef()),
                            constructor.typeParameters.map { it.descriptor.toClassifierRef() }
                        )

                        constructor.typeParameters
                            .map {
                                substitutionMap[it.descriptor.toClassifierRef()]
                                    ?: error("No substitution found for ${it.dump()}")
                            }
                            .forEachIndexed { index, typeArgument ->
                                putTypeArgument(index, typeArgument.toIrType(pluginContext))
                            }

                        fillGivens(this)
                    }
            }
        }

        private fun propertyExpression(
            type: TypeRef,
            descriptor: PropertyDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val property = pluginContext.referenceProperties(descriptor.fqNameSafe)
                .single()
            val getter = property.owner.getter!!
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(getter.symbol)
                .apply {
                    val dispatchReceiverParameter = getter.dispatchReceiverParameter
                    if (dispatchReceiverParameter != null) {
                        dispatchReceiver =
                            if (dispatchReceiverParameter.type.classOrNull?.owner?.kind == ClassKind.OBJECT) {
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGetObject(dispatchReceiverParameter.type.classOrNull!!)
                            } else {
                                dispatchReceiverAccessors
                                    .last { it.first == dispatchReceiverParameter.type.classOrNull?.owner }
                                    .second()
                            }
                    }
                    val substitutionMap = getSubstitutionMap(
                        listOf(type to getter.returnType.toTypeRef()),
                        getter.typeParameters.map { it.descriptor.toClassifierRef() }
                    )

                    getter.typeParameters
                        .map {
                            substitutionMap[it.descriptor.toClassifierRef()]
                                ?: error("No substitution found for ${it.dump()}")
                        }
                        .forEachIndexed { index, typeArgument ->
                            putTypeArgument(index, typeArgument.toIrType(pluginContext))
                        }

                    fillGivens(this)
                }
        }

        private fun functionExpression(
            type: TypeRef,
            descriptor: FunctionDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val function = descriptor.irFunction()
            return DeclarationIrBuilder(pluginContext, symbol)
                .irCall(function.symbol)
                .apply {
                    val dispatchReceiverParameter = function.dispatchReceiverParameter
                    if (dispatchReceiverParameter != null) {
                        dispatchReceiver =
                            if (dispatchReceiverParameter.type.classOrNull?.owner?.kind == ClassKind.OBJECT) {
                                DeclarationIrBuilder(pluginContext, symbol)
                                    .irGetObject(dispatchReceiverParameter.type.classOrNull!!)
                            } else {
                                dispatchReceiverAccessors.reversed()
                                    .first { it.first == dispatchReceiverParameter.type.classOrNull?.owner }
                                    .second()
                            }
                    }

                    val substitutionMap = getSubstitutionMap(
                        listOf(type to function.returnType.toTypeRef()),
                        function.typeParameters.map { it.descriptor.toClassifierRef() }
                    )

                    function.typeParameters
                        .map {
                            substitutionMap[it.descriptor.toClassifierRef()]
                                ?: error("No substitution found for ${it.dump()}")
                        }
                        .forEachIndexed { index, typeArgument ->
                            putTypeArgument(index, typeArgument.toIrType(pluginContext))
                        }

                    fillGivens(this)
                }
        }

        private fun parameterExpression(
            descriptor: ParameterDescriptor,
            symbol: IrSymbol,
        ): IrExpression {
            val valueParameter =
                when (val containingDeclaration = descriptor.containingDeclaration) {
                    is ConstructorDescriptor -> containingDeclaration.irConstructor()
                        .allParameters
                        .single { it.name == descriptor.name }
                    is FunctionDescriptor -> containingDeclaration.irFunction()
                        .let { function ->
                            function.allParameters
                                .filter { it != function.dispatchReceiverParameter }
                        }
                        .single { it.name == descriptor.name }
                    else -> error("Unexpected parent $descriptor $containingDeclaration")
                }

            return DeclarationIrBuilder(pluginContext, symbol)
                .irGet(valueParameter)
        }

        private fun ConstructorDescriptor.irConstructor() =
            pluginContext.referenceConstructors(constructedClass.fqNameSafe).first {
                it.descriptor.original == this.original
            }.owner

        private fun FunctionDescriptor.irFunction() = pluginContext.referenceFunctions(fqNameSafe)
            .singleOrNull()?.owner ?: error("Nothing found for $this")

        private fun givensFor(type: TypeRef): List<CallableDescriptor> {
            val givens = givensForInThisScope(type)
            return when {
                givens.isNotEmpty() -> givens
                parent != null -> parent.givensFor(type)
                else -> emptyList()
            }
        }

        protected abstract fun givensForInThisScope(type: TypeRef): List<CallableDescriptor>
    }

    private inner class ExternalScope : Scope(null) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
    }

    private inner class InternalScope(parent: Scope?) : Scope(parent) {
        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            declarationStore.givensForType(type)
                .filterNot { it.isExternalDeclaration() }
    }

    private inner class ClassScope(
        private val declaration: IrClass,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens =
            declaration.descriptor.extractGivensOfDeclaration(pluginContext.bindingContext)

        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivens.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
    }

    private inner class FunctionScope(
        private val declaration: IrFunction,
        parent: Scope?,
    ) : Scope(parent) {
        private val allGivens =
            declaration.descriptor.extractGivensOfCallable()

        override fun givensForInThisScope(type: TypeRef): List<CallableDescriptor> =
            allGivens.filter { it.returnType!!.toTypeRef().isAssignableTo(type) }
    }

    private var scope: Scope = InternalScope(ExternalScope())

    private val dispatchReceiverAccessors = mutableListOf<Pair<IrClass, () -> IrExpression>>()

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val prevScope = this.scope
        this.scope = scope
        val result = block()
        this.scope = prevScope
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        dispatchReceiverAccessors.push(
            declaration to {
                DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .irGet(declaration.thisReceiver!!)
            }
        )
        val result = if (declaration.kind == ClassKind.OBJECT) {
            inScope(ClassScope(declaration, scope)) { super.visitClass(declaration) }
        } else {
            val parentScope = declaration.companionObject()
                ?.let { it as? IrClass }
                ?.let { ClassScope(it, scope) } ?: scope
            inScope(ClassScope(declaration, parentScope)) { super.visitClass(declaration) }
        }
        dispatchReceiverAccessors.pop()
        return result
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val dispatchReceiver = declaration.dispatchReceiverParameter?.type?.classOrNull?.owner
        if (dispatchReceiver != null) {
            dispatchReceiverAccessors.push(
                dispatchReceiver to {
                    DeclarationIrBuilder(pluginContext, declaration.symbol)
                        .irGet(declaration.dispatchReceiverParameter!!)
                }
            )
        }
        val result = inScope(FunctionScope(declaration, scope)) {
            super.visitFunction(declaration)
        }
        if (dispatchReceiver != null) {
            dispatchReceiverAccessors.pop()
        }
        return result
    }

    override fun visitCall(expression: IrCall): IrExpression =
        super.visitCall(expression.apply { scope.fillGivens(this) })

}
