package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

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
        val scope: IrClassSymbol,
        val binding: IrClassSymbol
    )

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.ensureBound(context.irProviders).owner.name.asString() != "initializeEndpoint") return expression

        val jitBindings = mutableListOf<JitBindingDescriptor>()

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
                            .classOrNull!!,
                        (annotation.getValueArgument(2) as IrClassReference)
                            .classType
                            .classOrNull!!,
                    )
                }
                return super.visitClass(declaration)
            }
        })

        symbols.aggregatePackage
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .map {
                val annotationValues =
                    it.annotations.findAnnotation(InjektFqNames.JitBindingMetadata)
                        ?.allValueArguments ?: error("Lol $it")
                JitBindingDescriptor(
                    (annotationValues[Name.identifier("type")]!! as KClassValue)
                        .getArgumentType(context.moduleDescriptor)
                        .toIrType()
                        .classOrNull!!,
                    (annotationValues[Name.identifier("scope")]!! as KClassValue)
                        .getArgumentType(context.moduleDescriptor)
                        .toIrType()
                        .classOrNull!!,
                    (annotationValues[Name.identifier("provider")]!! as KClassValue)
                        .getArgumentType(context.moduleDescriptor)
                        .toIrType()
                        .classOrNull!!
                )
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

                        val factoryType = KotlinTypeFactory.simpleType(
                            context.builtIns.getFunction(0).defaultType,
                            arguments = listOf(
                                symbols.jitBindingLookup.descriptor.defaultType
                                    .asTypeProjection()
                            )
                        )
                        putValueArgument(
                            1,
                            irLambdaExpression(
                                createFunctionDescriptor(factoryType),
                                factoryType.toIrType()
                            ) {
                                val binding = jitBinding.binding.owner
                                +irReturn(
                                    irCall(symbols.jitBindingLookup.constructors.single()).apply {
                                        putTypeArgument(0, jitBinding.type.defaultType)
                                        putValueArgument(
                                            0,
                                            IrClassReferenceImpl(
                                                startOffset,
                                                endOffset,
                                                this@InjektInitTransformer.context.irBuiltIns.kClassClass.typeWith(
                                                    jitBinding.scope.defaultType
                                                ),
                                                jitBinding.scope.defaultType.classifierOrFail,
                                                jitBinding.scope.defaultType
                                            )
                                        )
                                        putValueArgument(
                                            1,
                                            when {
                                                binding.kind == ClassKind.OBJECT -> irGetObject(
                                                    binding.symbol
                                                )
                                                jitBinding.scope != symbols.factory -> irCall(
                                                    symbols.asScoped
                                                ).apply {
                                                    extensionReceiver =
                                                        irCall(binding.constructors.single())
                                                }
                                                else -> irCall(binding.constructors.single())
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}