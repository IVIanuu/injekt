package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektInitTransformer(
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    private lateinit var moduleFragment: IrModuleFragment

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        moduleFragment = declaration
        return super.visitModuleFragment(declaration)
    }

    private class JitBindingDescriptor(
        val type: IrClassSymbol,
        val binding: IrClassSymbol
    )

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.ensureBound(context.irProviders).owner.name.asString() != "initializeEndpoint") return expression

        val jitBindings = mutableListOf<JitBindingDescriptor>()
        val modules = mutableMapOf<IrClass, MutableList<IrFunction>>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                val annotation = declaration.annotations.singleOrNull {
                    it.type == symbols.jitBindingMetadata.defaultType
                }
                if (annotation != null) {
                    jitBindings += JitBindingDescriptor(
                        (annotation.getValueArgument(0) as IrClassReference)
                            .classType
                            .classOrNull!!,
                        (annotation.getValueArgument(1) as IrClassReference)
                            .classType
                            .classOrNull!!
                    )
                }
                return super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                val origin = declaration.origin
                if (origin is ModuleAccessorOrigin) {
                    val scope =
                        origin.module.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                            .single()
                            .type
                            .constructor
                            .declarationDescriptor!!
                            .let { symbolTable.referenceClass(it.cast()) }
                            .ensureBound(context.irProviders)
                            .owner
                    modules.getOrPut(scope) { mutableListOf() } += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        symbols.aggregatePackage
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .forEach {
                if (it.annotations.hasAnnotation(InjektFqNames.JitBindingMetadata)) {
                    val annotationValues =
                        it.annotations.findAnnotation(InjektFqNames.JitBindingMetadata)!!.allValueArguments
                    jitBindings += JitBindingDescriptor(
                        (annotationValues[Name.identifier("type")]!! as KClassValue)
                            .getArgumentType(context.moduleDescriptor)
                            .toIrType()
                            .classOrNull!!,
                        (annotationValues[Name.identifier("binding")]!! as KClassValue)
                            .getArgumentType(context.moduleDescriptor)
                            .toIrType()
                            .classOrNull!!
                    )
                } else {
                    val fqName = FqName(it.name.asString().replace("_", "."))
                    val name = Name.identifier(fqName.shortName().asString() + "\$ModuleAccessor")
                    val scope = it.getAnnotatedAnnotations(InjektFqNames.Scope)
                        .single()
                        .type
                        .constructor
                        .declarationDescriptor!!
                        .let { symbolTable.referenceClass(it.cast()) }
                        .ensureBound(context.irProviders)
                        .owner
                    modules.getOrPut(scope) { mutableListOf() } += symbols.getPackage(fqName.parent())
                        .memberScope
                        .findSingleFunction(name)
                        .let { context.symbolTable.referenceFunction(it) }
                        .ensureBound(context.irProviders)
                        .owner
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

                        putTypeArgument(0, jitBinding.type.defaultType)

                        putValueArgument(0, irCall(symbols.keyOf).apply {
                            putTypeArgument(0, jitBinding.type.defaultType)
                        })

                        val binding = jitBinding.binding.owner
                        putValueArgument(
                            1,
                            if (binding.kind == ClassKind.OBJECT) irGetObject(binding.symbol)
                            else irCall(binding.constructors.single())
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
                            putValueArgument(1, irCall(module))
                        }
                    }
                }
            }
        }
    }
}