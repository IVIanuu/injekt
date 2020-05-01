package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace

class FactoryTransformer(
    context: IrPluginContext,
    bindingTrace: BindingTrace,
    private val declarationStore: InjektDeclarationStore
) :
    AbstractInjektTransformer(context, bindingTrace) {

    private val factoryFunctions = mutableListOf<IrFunction>()
    private val transformedFactories = mutableMapOf<IrFunction, IrClass>()
    private val transformingFactories = mutableSetOf<FqName>()
    private var computedFactoryFunctions = false

    fun getImplementationClassForFactory(fqName: FqName): IrClass? {
        transformedFactories.values.firstOrNull {
            it.fqNameForIrSerialization == fqName
        }?.let { return it }

        val function = factoryFunctions.firstOrNull {
            val packageName = it.fqNameForIrSerialization.parent()
            packageName.child(
                InjektNameConventions.getModuleNameForModuleFunction(it.name)
            ) == fqName
        } ?: return null

        return getImplementationClassForFactory(function)
    }

    fun getImplementationClassForFactory(function: IrFunction): IrClass? {
        computeFactoryFunctionsIfNeeded()

        check(function in factoryFunctions) {
            "Unknown function $function"
        }
        transformedFactories[function]?.let { return it }
        return DeclarationIrBuilder(context, function.symbol).run {
            val packageName = function.fqNameForIrSerialization.parent()
            val implementationName =
                InjektNameConventions.getImplementationNameForFactoryFunction(function.name)
            val implementationFqName = packageName.child(implementationName)
            check(implementationFqName !in transformingFactories) {
                "Circular dependency for factory $implementationFqName"
            }
            transformingFactories += implementationFqName
            val implementationClass = implementationClass(function)
            println(implementationClass.dump())
            function.file.addChild(implementationClass)
            function.body = irExprBody(irInjektIntrinsicUnit())
            transformedFactories[function] = implementationClass
            transformingFactories -= implementationFqName
            implementationClass
        }
    }

    private fun computeFactoryFunctionsIfNeeded() {
        if (computedFactoryFunctions) return
        computedFactoryFunctions = true
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.annotations.hasAnnotation(InjektFqNames.ChildFactory)
                ) {
                    factoryFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })
    }

    private fun IrBuilderWithScope.implementationClass(
        function: IrFunction
    ) = buildClass {
        name = InjektNameConventions.getImplementationNameForFactoryFunction(function.name)
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        superTypes += function.returnType

        val moduleCall = function.body?.statements?.single()
            ?.let { it as? IrCall }
            ?.getValueArgument(0) as? IrCall

        val moduleFqName = moduleCall?.symbol?.owner?.fqNameForIrSerialization
            ?.parent()
            ?.child(Name.identifier("${moduleCall.symbol.owner.name}\$Impl"))

        val module = if (moduleFqName != null) declarationStore.getModule(moduleFqName)
        else null

        println("module for impl $module")

        val dependencyRequests = mutableListOf<IrDeclaration>()

        /*var superType: IrClass? = superTypes.single() as IrClass
        while (superType != null) {
            superType.declarations.forEach { declaration ->
                when (declaration) {
                    is IrFunction -> {
                        declaration.
                        if (declaration.ki)
                    }
                    is IrProperty -> {

                    }
                }
            }
        }*/

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            copyTypeParametersFrom(this@clazz)

            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }
    }

}
