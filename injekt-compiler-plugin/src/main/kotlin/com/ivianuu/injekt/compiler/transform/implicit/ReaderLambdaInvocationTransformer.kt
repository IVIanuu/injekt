/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.canUseImplicits
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getContext
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.irClassReference
import com.ivianuu.injekt.compiler.isReaderLambdaInvoke
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReaderLambdaInvocationTransformer(
    pluginContext: IrPluginContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(pluginContext) {

    private val nameProvider = NameProvider()
    private val newDeclarations = mutableListOf<IrDeclaration>()

    private sealed class Scope {
        abstract val file: IrFile
        abstract val fqName: FqName
        abstract val invocationContext: IrClass

        class Reader(
            val declaration: IrDeclaration,
            override val invocationContext: IrClass
        ) : Scope() {
            override val file: IrFile
                get() = declaration.file
            override val fqName: FqName
                get() = declaration.descriptor.fqNameSafe
        }

        class RunReader(
            val call: IrCall,
            override val invocationContext: IrClass,
            override val file: IrFile,
            override val fqName: FqName
        ) : Scope() {
            fun isBlock(function: IrFunction): Boolean =
                call.getValueArgument(0).let {
                    it is IrFunctionExpression &&
                            it.function == function
                }
        }
    }

    private var currentReaderScope: Scope? = null

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val previousScope = currentReaderScope
        currentReaderScope = scope
        val result = block()
        currentReaderScope = previousScope
        return result
    }

    override fun lower() {
        module.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitClassNew(declaration: IrClass): IrStatement {
                return if (declaration.canUseImplicits(pluginContext)) {
                    inScope(
                        Scope.Reader(
                            declaration,
                            declaration.getReaderConstructor(pluginContext)!!
                                .getContext()!!
                        )
                    ) {
                        super.visitClassNew(declaration)
                    }
                } else super.visitClassNew(declaration)
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                return if (declaration.canUseImplicits(pluginContext) &&
                    currentReaderScope.let {
                        it == null || it !is Scope.RunReader ||
                                !it.isBlock(declaration)
                    }
                ) {
                    inScope(
                        Scope.Reader(
                            declaration,
                            declaration.getContext()!!
                        )
                    ) {
                        super.visitFunctionNew(declaration)
                    }
                } else super.visitFunctionNew(declaration)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                return if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runReader"
                ) {
                    currentScope!!.scope.scopeOwner.fqNameSafe
                    inScope(
                        Scope.RunReader(
                            expression,
                            expression.extensionReceiver!!.type.classOrNull!!.owner,
                            currentFile,
                            currentScope!!.scope.scopeOwner.fqNameSafe
                        )
                    ) {
                        super.visitCall(expression)
                    }
                } else {
                    if (expression.isReaderLambdaInvoke(pluginContext)) {
                        visitReaderLambdaInvoke(expression)
                    }
                    super.visitCall(expression)
                }
            }
        })

        newDeclarations.forEach {
            it.file.addChild(it)
            indexer.index(it as IrDeclarationWithName)
            println("index ${it.dumpSrc()}")
        }
    }

    private fun visitReaderLambdaInvoke(call: IrCall) {
        val scope = currentReaderScope!!

        val lambdaContext = try {
            call.dispatchReceiver!!.type.typeArguments.single {
                it.typeOrNull?.classOrNull?.owner?.hasAnnotation(InjektFqNames.Context) == true
            }.typeOrFail.classOrNull!!.owner
        } catch (e: Exception) {
            error("Failed to get context from lambda ${call.dump()}")
        }

        newDeclarations += buildClass {
            name = nameProvider.allocateForGroup(
                "${scope.fqName.pathSegments()
                    .joinToString("_")}ReaderInvocation".asNameId()
            )
            kind = ClassKind.INTERFACE
            visibility = Visibilities.INTERNAL
        }.apply {
            parent = scope.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            annotations +=
                DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(symbols.readerInvocation.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irClassReference(lambdaContext)
                        )
                        putValueArgument(
                            1,
                            irClassReference(scope.invocationContext)
                        )
                    }
                }
        }
    }

}
