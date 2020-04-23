package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getProviderFqName
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
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
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class AnnotatedBindingTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.annotations.hasAnnotation(InjektFqNames.Factory) ||
            declaration.annotations.hasAnnotation(InjektFqNames.Single)
        ) {
            declaration.addChild(
                DeclarationIrBuilder(context, declaration.symbol)
                    .provider(
                        declaration,
                        declaration.annotations.hasAnnotation(InjektFqNames.Single)
                    )
            )
        }
        return super.visitClass(declaration)
    }

    private fun IrBuilderWithScope.provider(
        clazz: IrClass,
        isSingle: Boolean
    ): IrClass {
        val constructor = clazz.constructors.single() // todo

        val dependencies = constructor.valueParameters

        return buildClass {
            kind =
                if (constructor.valueParameters.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            origin = InjektDeclarationOrigin
            this.name = getProviderFqName(clazz.descriptor).shortName()
            modality = Modality.FINAL
            visibility = Visibilities.PUBLIC
        }.apply clazz@{
            superTypes += symbols.provider.typeWith(clazz.defaultType)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            val fieldsByDependency = dependencies
                .associateWith { valueParameter ->
                    addField(
                        valueParameter.name,
                        symbols.provider.typeWith(valueParameter.type),
                        Visibilities.PRIVATE
                    )
                }

            addConstructor {
                returnType = defaultType
                visibility = Visibilities.PUBLIC
                isPrimary = true
            }.apply {
                fieldsByDependency.forEach { (_, field) ->
                    addValueParameter(
                        field.name.asString(),
                        field.type
                    ).apply {
                        annotations += bindingMetadata(emptyList()) // todo
                    }
                }

                body = irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)
                    valueParameters
                        .forEach { valueParameter ->
                            +irSetField(
                                irGet(thisReceiver!!),
                                fieldsByDependency.values.toList()[valueParameter.index],
                                irGet(valueParameter)
                            )
                        }
                }
            }

            val companion = if (dependencies.isNotEmpty()) {
                providerCompanion(
                    clazz
                ).also { addChild(it) }
            } else null

            val createFunction = if (dependencies.isEmpty()) {
                createFunction(this, constructor, clazz)
            } else {
                null
            }

            addFunction {
                this.name = Name.identifier("invoke")
                returnType = clazz.defaultType
                visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

                overriddenSymbols += symbolTable.referenceSimpleFunction(
                    symbols.provider.descriptor
                        .unsubstitutedMemberScope.findSingleFunction(Name.identifier("invoke"))
                )

                body = irExprBody(
                    irCall(companion?.functions?.single() ?: createFunction!!).apply {
                        dispatchReceiver =
                            if (companion != null) irGetObject(companion.symbol) else irGet(
                                dispatchReceiverParameter!!
                            )

                        fieldsByDependency.values.forEachIndexed { index, field ->
                            putValueArgument(
                                index,
                                irCall(
                                    symbolTable.referenceFunction(
                                        symbols.provider.descriptor
                                            .unsubstitutedMemberScope.findSingleFunction(
                                            Name.identifier("invoke")
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

            annotations += bindingMetadata(emptyList())
            annotations += providerMetadata(isSingle)
        }
    }

    private fun IrBuilderWithScope.providerCompanion(clazz: IrClass) = buildClass {
        kind = ClassKind.OBJECT
        origin = InjektDeclarationOrigin
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        modality = Modality.FINAL
        visibility = Visibilities.PUBLIC
        isCompanion = true
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        addConstructor {
            returnType = defaultType
            visibility = Visibilities.PUBLIC
            isPrimary = true
        }.apply {
            body = irBlockBody {
                +IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.unitType,
                    symbolTable.referenceConstructor(
                        context.builtIns.any
                            .unsubstitutedPrimaryConstructor!!
                    )
                )
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }

        createFunction(this, clazz.constructors.single(), clazz)
    }

    private fun IrBuilderWithScope.createFunction(
        owner: IrClass,
        constructor: IrConstructor?,
        clazz: IrClass
    ): IrFunction {
        return owner.addFunction {
            name = Name.identifier("create")
            returnType = clazz.defaultType
            visibility = Visibilities.PUBLIC
        }.apply {
            dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

            val valueParametersByDependency = constructor?.valueParameters
                ?.associateWith { valueParameter ->
                    addValueParameter {
                        this.name = valueParameter.name
                        type = valueParameter.type
                    }.apply {
                        annotations += bindingMetadata(emptyList())
                    }
                }

            body = irExprBody(
                if (clazz.kind == ClassKind.OBJECT) {
                    irGetObject(clazz.symbol)
                } else {
                    irCall(constructor!!).apply {
                        valueParametersByDependency!!.forEach { (_, valueParameter) ->
                            putValueArgument(valueParameter.index, irGet(valueParameter))
                        }
                    }
                }
            )
        }
    }
}
