package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.getNearestDeclarationContainer
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ClassFactoryTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    private val factoriesByClass = mutableMapOf<IrClass, IrClass>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.hasAnnotatedAnnotations(
                        InjektFqNames.Scope, declaration.module
                    )
                ) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { getFactoryForClass(it) }

        factoriesByClass
            .keys
            .map { it.file }
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

    fun getFactoryForClass(clazz: IrClass): IrClass {
        factoriesByClass[clazz]?.let { return it }
        val constructor = clazz.constructors.singleOrNull()
        val factory = InjektDeclarationIrBuilder(pluginContext, clazz.symbol).run {
            factory(
                name = InjektNameConventions.getFactoryNameForClass(clazz.name),
                visibility = clazz.visibility,
                parameters = constructor?.valueParameters
                    ?.map { valueParameter ->
                        InjektDeclarationIrBuilder.FactoryParameter(
                            name = valueParameter.name.asString(),
                            type = valueParameter.type,
                            assisted = valueParameter.hasAnnotation(InjektFqNames.Assisted),
                            requirement = false
                        )
                    } ?: emptyList(),
                returnType = clazz.defaultType,
                createBody = { createFunction ->
                    builder.irExprBody(
                        if (clazz.kind == ClassKind.OBJECT) {
                            builder.irGetObject(clazz.symbol)
                        } else {
                            builder.irCall(constructor ?: error("Wtf ${clazz.dump()}"))
                                .apply {
                                    createFunction.valueParameters.forEach { valueParameter ->
                                        putValueArgument(
                                            valueParameter.index,
                                            irGet(valueParameter)
                                        )
                                    }
                                }
                        }
                    )
                }
            )
        }
        clazz.getNearestDeclarationContainer(includeThis = false).addChild(factory)
        factoriesByClass[clazz] = factory
        return factory
    }

}
