package com.ivianuu.injekt.compiler.backend.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.LookupManager
import com.ivianuu.injekt.compiler.UniqueNameProvider
import com.ivianuu.injekt.compiler.backend.DeclarationGraph
import com.ivianuu.injekt.compiler.backend.addChildAndUpdateMetadata
import com.ivianuu.injekt.compiler.backend.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.backend.asNameId
import com.ivianuu.injekt.compiler.backend.buildClass
import com.ivianuu.injekt.compiler.backend.irBuilder
import com.ivianuu.injekt.compiler.backend.substitute
import com.ivianuu.injekt.compiler.backend.typeArguments
import com.ivianuu.injekt.compiler.backend.typeOrFail
import com.ivianuu.injekt.compiler.backend.typeWith
import com.ivianuu.injekt.compiler.backend.uniqueTypeName
import com.ivianuu.injekt.compiler.backend.visitAllFunctionsWithSubstitutionMap
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class ReaderContextFactoryImplGenerator(
    private val name: Name,
    private val factoryInterface: IrClass,
    private val factoryType: IrType,
    private val irParent: IrDeclarationParent,
    private val parentContext: IrClass?,
    private val parentGraph: GivensGraph?,
    private val parentExpressions: GivenExpressions?
) {

    private val lookupManager = given<LookupManager>()

    fun generateFactory(): IrClass {
        val createFunction = factoryInterface.functions
            .first { it.name.asString().startsWith("create") }

        val inputTypes = createFunction
            .valueParameters
            .map { it.type }
            .map { inputType ->
                inputType.substitute(
                    factoryInterface.typeParameters
                        .map { it.symbol }
                        .zip(factoryType.typeArguments.map { it.typeOrFail })
                        .toMap()
                )
            }

        val contextIdType = createFunction.returnType
            .substitute(
                factoryInterface.typeParameters
                    .map { it.symbol }
                    .zip(factoryType.typeArguments.map { it.typeOrFail })
                    .toMap()
            )

        val factoryImpl = buildClass {
            this.name = this@ReaderContextFactoryImplGenerator.name
            if (parentContext == null) kind = ClassKind.OBJECT
            visibility = if (parentContext != null) Visibilities.PRIVATE else Visibilities.INTERNAL
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            superTypes += factoryType
        }

        val parentField = if (parentContext != null) {
            factoryImpl.addField("parent", parentContext.defaultType)
        } else null

        factoryImpl.addConstructor {
            returnType = factoryImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            addMetadataIfNotLocal()
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else null

            body = irBuilder().irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    factoryImpl.symbol,
                    context.irBuiltIns.unitType
                )
                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(factoryImpl.thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }
            }
        }

        val contextImpl = generateReaderContext(contextIdType, inputTypes, factoryImpl)
        factoryImpl.addChild(contextImpl)

        factoryImpl.addFunction {
            name = "create".asNameId()
            returnType = contextIdType
        }.apply {
            addMetadataIfNotLocal()
            dispatchReceiverParameter = factoryImpl.thisReceiver!!.copyTo(this)

            overriddenSymbols += createFunction.symbol

            val inputNameProvider = UniqueNameProvider()
            inputTypes.forEach {
                addValueParameter(
                    inputNameProvider(it.uniqueTypeName().asString()),
                    it
                )
            }

            body = irBuilder().run {
                irExprBody(
                    if (contextImpl.isObject) {
                        irGetObject(contextImpl.symbol)
                    } else {
                        val contextImplInputs = contextImpl.constructors.single()
                            .valueParameters
                            .drop(if (parentContext != null) 1 else 0)
                            .map { it.type }
                        irCall(contextImpl.constructors.single()).apply {
                            if (parentContext != null) {
                                putValueArgument(
                                    0,
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        parentField!!
                                    )
                                )
                            }
                            contextImplInputs.forEachIndexed { index, type ->
                                putValueArgument(
                                    index + if (parentContext != null) 1 else 0,
                                    irGet(valueParameters[index])
                                )
                            }
                        }
                    }
                )
            }
        }

        val initTrigger = given<InitTrigger>()
        // we add an empty copy of the context impl to the trigger file
        // this ensures that the init trigger file get's compiled every time a super type context changes
        // so that we can regenerate the context impl
        val stub = buildClass {
            this.name = Name.identifier(
                "${
                    factoryImpl.descriptor.fqNameSafe.pathSegments().joinToString("_")
                }Stubs"
            )
            kind = ClassKind.CLASS
        }.apply clazz@{
            parent = initTrigger.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            superTypes += contextImpl.superTypes
                .onEach { lookupManager.recordLookup(this, it.classOrNull!!.owner) }
            contextImpl.functions.forEach { function ->
                addFunction {
                    name = function.name
                    returnType = function.returnType
                }.apply {
                    dispatchReceiverParameter = this@clazz.thisReceiver!!.copyTo(this)
                    addMetadataIfNotLocal()
                    overriddenSymbols += function.symbol
                    body = irBuilder().irBlockBody { }
                }
            }
        }

        initTrigger.file.addChildAndUpdateMetadata(stub)
        lookupManager.recordLookup(factoryImpl, stub)

        return factoryImpl
    }

    private fun generateReaderContext(
        contextIdType: IrType,
        inputTypes: List<IrType>,
        irParent: IrDeclarationParent
    ): IrClass {
        val contextImpl = buildClass {
            this.name = "C".asNameId()
            visibility = Visibilities.PRIVATE
            if (parentContext == null && inputTypes.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += contextIdType
            addMetadataIfNotLocal()
        }

        val parentField = if (parentContext != null) {
            contextImpl.addField("parent", parentContext.defaultType)
        } else null

        val inputFieldNameProvider = UniqueNameProvider()
        val inputFields = inputTypes
            .map {
                contextImpl.addField(
                    inputFieldNameProvider(it.uniqueTypeName().asString()),
                    it
                )
            }

        contextImpl.addConstructor {
            returnType = contextImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            addMetadataIfNotLocal()
            val parentValueParameter = if (parentField != null) {
                addValueParameter(parentField.name.asString(), parentField.type)
            } else null

            val inputValueParameters = inputFields.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = irBuilder().irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    contextImpl.symbol,
                    context.irBuiltIns.unitType
                )
                if (parentValueParameter != null) {
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        parentField!!,
                        irGet(parentValueParameter)
                    )
                }
                inputValueParameters.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }
            }
        }

        val expressions = GivenExpressions(parentExpressions, contextImpl)

        val graph = GivensGraph(
            parent = parentGraph,
            expressions = expressions,
            contextImpl = contextImpl,
            inputs = inputFields
        )

        val entryPoints =
            listOf(contextIdType) + given<DeclarationGraph>().getRunReaderContexts(contextIdType.classOrNull!!.owner)
                .map { entryPoint ->
                    // this is really naive and probably error prone
                    if (factoryInterface.typeParameters.size == entryPoint.typeParameters.size &&
                        factoryInterface.typeParameters.zip(entryPoint.typeParameters).all {
                            it.first.name == it.second.name
                        }
                    ) {
                        entryPoint.typeWith(factoryType.typeArguments.map { it.typeOrFail })
                    } else entryPoint.defaultType
                }

        graph.checkEntryPoints(entryPoints)

        (entryPoints + graph.resolvedGivens.flatMap { it.value.contexts })
            .filterNot { it in contextImpl.superTypes }
            .forEach { context ->
                contextImpl.superTypes += context
                context.visitAllFunctionsWithSubstitutionMap { function, substitutionMap ->
                    val existingDeclaration = contextImpl.functions.singleOrNull {
                        it.name == function.name
                    }
                    if (existingDeclaration != null) {
                        existingDeclaration.overriddenSymbols += function.symbol as IrSimpleFunctionSymbol
                    }

                    val key = function.returnType
                        .substitute(substitutionMap)
                        .asKey()

                    expressions.getGivenExpression(graph.getGiven(key), function)
                }
            }

        return contextImpl
    }
}
