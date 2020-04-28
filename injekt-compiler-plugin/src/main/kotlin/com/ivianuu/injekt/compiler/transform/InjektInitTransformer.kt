package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class InjektInitTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.ensureBound(context.irProviders).owner.name.asString() != "initializeEndpoint") return expression

        val jitBindings = mutableListOf<IrFunction>()
        val modules = mutableMapOf<IrClass, MutableList<IrFunction>>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                val origin = declaration.origin
                if (origin is ModuleAccessorOrigin) {
                    val scope =
                        origin.module.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                            .single()
                            .type
                            .constructor
                            .declarationDescriptor!!
                            .let { symbolTable.referenceClass(it as ClassDescriptor) }
                            .ensureBound(context.irProviders)
                            .owner
                    modules.getOrPut(scope) { mutableListOf() } += declaration
                } else if (origin is BindingAccessorOrigin) {
                    jitBindings += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        symbols.aggregatePackage
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .forEach {
                val fqName = FqName(it.name.asString().replace("_", "."))
                try {
                    val name = Name.identifier(fqName.shortName().asString())
                    val scope = it.getAnnotatedAnnotations(InjektFqNames.Scope)
                        .single()
                        .type
                        .constructor
                        .declarationDescriptor!!
                        .let { symbolTable.referenceClass(it as ClassDescriptor) }
                        .ensureBound(context.irProviders)
                        .owner
                    modules.getOrPut(scope) { mutableListOf() } += symbols.getPackage(fqName.parent())
                        .memberScope
                        .findSingleFunction(name)
                        .let { context.symbolTable.referenceFunction(it) }
                        .ensureBound(context.irProviders)
                        .owner
                } catch (e: Exception) {
                    try {
                        val name = Name.identifier(fqName.shortName().asString())
                        jitBindings += symbols.getPackage(fqName.parent())
                            .memberScope
                            .getContributedDescriptors()
                            .single { it.name == name }
                            .let { it as FunctionDescriptor }
                            .let { context.symbolTable.referenceFunction(it) }
                            .ensureBound(context.irProviders)
                            .owner
                    } catch (e: Exception) {
                    }
                }
            }

        return DeclarationIrBuilder(context, expression.symbol).run {
            irBlock {
                val registerJitBinding = symbols.jitBindingRegistry
                    .functions
                    .single { it.descriptor.name.asString() == "register" }

                jitBindings.forEach { jitBinding ->
                    +irCall(registerJitBinding).apply {
                        dispatchReceiver = irGetObject(symbols.jitBindingRegistry)

                        val bindingType =
                            (jitBinding.returnType as IrSimpleType).arguments.single().typeOrNull!!

                        putTypeArgument(0, bindingType)

                        putValueArgument(0, irCall(symbols.keyOf).apply {
                            putTypeArgument(0, bindingType)
                        })

                        putValueArgument(
                            1,
                            irCall(jitBinding)
                        )
                    }
                }

                val registerModule = symbols.moduleRegistry
                    .functions
                    .single { it.descriptor.name.asString() == "register" }

                modules.forEach { (scope, modulesForScope) ->
                    val scopeVar = irTemporary(
                        IrClassReferenceImpl(
                            startOffset,
                            endOffset,
                            this@InjektInitTransformer.context.irBuiltIns.kClassClass.typeWith(scope.defaultType),
                            scope.symbol,
                            scope.defaultType
                        )
                    )

                    modulesForScope.forEach { module ->
                        +irCall(registerModule).apply {
                            dispatchReceiver = irGetObject(symbols.moduleRegistry)
                            putValueArgument(0, irGet(scopeVar))
                            putValueArgument(
                                1, IrFunctionReferenceImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    symbolTable.referenceClass(context.builtIns.getFunction(1))
                                        .typeWith(
                                            symbols.componentDsl.defaultType,
                                            context.irBuiltIns.unitType
                                        ),
                                    module.symbol,
                                    0,
                                    null
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}