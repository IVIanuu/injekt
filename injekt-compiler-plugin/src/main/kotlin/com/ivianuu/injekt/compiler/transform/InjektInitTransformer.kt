package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.ensureBound
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
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
import org.jetbrains.kotlin.utils.addToStdlib.cast

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

        val jitBindings = mutableListOf<IrClass>()
        val modules = mutableMapOf<IrClass, MutableList<IrFunction>>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.name.asString().endsWith("\$Binding")) {
                    jitBindings += declaration
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
                val fqName = FqName(it.name.asString().replace("_", "."))
                try {
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
                } catch (e: Exception) {
                    jitBindings += symbols.getTopLevelClass(fqName.parent())
                        .descriptor
                        .unsubstitutedMemberScope
                        .getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
                        .let { context.symbolTable.referenceClass(it.cast()) }
                        .ensureBound(context.irProviders)
                        .owner
                        .let { clazz ->
                            clazz.declarations
                                .single {
                                    it.descriptor.name.asString() == "${clazz.name}\$Binding"
                                } as IrClass
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

                        val bindingType = jitBinding.superTypes
                            .first()
                            .let { it as IrSimpleType }
                            .arguments
                            .single()
                            .typeOrNull!!

                        putTypeArgument(0, bindingType)

                        putValueArgument(0, irCall(symbols.keyOf).apply {
                            putTypeArgument(0, bindingType)
                        })

                        val binding = bindingType.classOrNull!!.owner
                        putValueArgument(
                            1,
                            if (binding.kind == ClassKind.OBJECT) irGetObject(jitBinding.symbol)
                            else irCall(jitBinding.constructors.single())
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