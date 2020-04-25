package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class ClassProviderGenerator(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableMapOf<IrClass, IrConstructor>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                        .singleOrNull() != null
                ) {
                    classes[declaration] = declaration.primaryConstructor!!
                }
                return super.visitClass(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor): IrStatement {
                if (declaration.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                        .singleOrNull() != null
                ) {
                    classes[declaration.constructedClass] = declaration
                }
                return super.visitConstructor(declaration)
            }
        })

        classes.forEach { (clazz, constructor) ->
            clazz.addChild(
                DeclarationIrBuilder(context, clazz.symbol)
                    .provider(clazz, constructor)
            )
        }

        return super.visitModuleFragment(declaration)
    }

    private fun IrBuilderWithScope.provider(
        clazz: IrClass,
        constructor: IrConstructor
    ) = buildClass {
        kind = if (constructor.valueParameters.isEmpty() && clazz.kind == ClassKind.OBJECT)
            ClassKind.OBJECT else ClassKind.CLASS
        this.name = Name.identifier("${clazz.name}\$Provider")
    }.apply clazz@{
        superTypes += symbols.provider.typeWith(clazz.defaultType)
        createImplicitParameterDeclarationWithWrappedDescriptor()

        val providerFieldsByConstructorParam = constructor.valueParameters
            .associateWith { valueParameter ->
                addField(
                    "${valueParameter.name}Provider",
                    symbols.provider.typeWith(valueParameter.type)
                )
            }

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            copyTypeParametersFrom(this@clazz)

            body = irBlockBody {
                +IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.unitType,
                    context.irBuiltIns.anyClass
                        .constructors.single()
                )
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }

        if (providerFieldsByConstructorParam.isNotEmpty()) {
            addFunction {
                this.name = Name.identifier("link")
                returnType = this@ClassProviderGenerator.context.irBuiltIns.unitType
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += symbols.linkable
                    .functions
                    .single { it.owner.name.asString() == "link" }

                val linkerParameter = addValueParameter(
                    "linker",
                    symbols.linkable.defaultType
                )

                body = irBlockBody {
                    providerFieldsByConstructorParam.forEach { (constructorParam, providerField) ->
                        +irSetField(
                            irGet(dispatchReceiverParameter!!),
                            providerField,
                            irCall(
                                symbolTable.referenceFunction(
                                    symbols.injektPackage.memberScope
                                        .findFirstFunction("get") {
                                            it.extensionReceiverParameter?.type?.constructor?.declarationDescriptor ==
                                                    symbols.linker.descriptor
                                        }
                                ),
                                providerField.type
                            ).apply {
                                dispatchReceiver = irGet(linkerParameter)
                                putTypeArgument(0, constructorParam.type)
                                val qualifier = constructorParam.descriptor.getAnnotatedAnnotations(
                                        InjektFqNames.Qualifier
                                    )
                                    .singleOrNull()?.fqName
                                    ?.let { symbols.getTopLevelClass(it) }

                                if (qualifier != null) {
                                    putValueArgument(
                                        0,
                                        IrClassReferenceImpl(
                                            startOffset,
                                            endOffset,
                                            this@ClassProviderGenerator.context.irBuiltIns.kClassClass.typeWith(
                                                qualifier.defaultType
                                            ),
                                            qualifier.defaultType.classifierOrFail,
                                            qualifier.defaultType
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        addFunction {
            this.name = Name.identifier("invoke")
            returnType = clazz.defaultType
        }.apply func@{
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

            overriddenSymbols += symbols.provider
                .functions
                .single {
                    it.descriptor.valueParameters.size == 1 &&
                            it.owner.name.asString() == "invoke"
                }

            body = irExprBody(
                irCall(constructor).apply {
                    providerFieldsByConstructorParam.values.forEachIndexed { index, field ->
                        putValueArgument(
                            index,
                            irCall(
                                symbolTable.referenceFunction(
                                    symbols.provider.descriptor.unsubstitutedMemberScope.findSingleFunction(
                                        Name.identifier(
                                            "invoke"
                                        )
                                    )
                                ),
                                (field.type as IrSimpleType).arguments.single().typeOrNull!!
                            ).apply {
                                dispatchReceiver = irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    field
                                )
                            }
                        )
                    }
                }
            )
        }
    }

}
