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

package com.ivianuu.injekt.compiler.transform.android

import com.ivianuu.injekt.compiler.AndroidSymbols
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// todo check android app
class CompositionAndroidAppTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val androidSymbols = AndroidSymbols(pluginContext)

    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)

        if (!declaration.hasAnnotation(InjektFqNames.CompositionAndroidApp)) return declaration

        declaration.annotations += InjektDeclarationIrBuilder(pluginContext, declaration.symbol)
            .noArgSingleConstructorCall(androidSymbols.androidEntryPoint)

        val superClass: IrClass = declaration.superTypes
            .single { it.classOrNull != null }
            .classOrNull!!
            .owner

        val superOnCreate = superClass
            .functions
            .single { it.name.asString() == "onCreate" && it.valueParameters.isEmpty() }

        var thisOnCreate: IrSimpleFunction? = declaration
            .functions
            .singleOrNull { it.name.asString() == "onCreate" && it.valueParameters.isEmpty() }

        if (thisOnCreate?.origin == IrDeclarationOrigin.FAKE_OVERRIDE) {
            declaration.declarations -= thisOnCreate
            thisOnCreate = null
        }

        if (thisOnCreate == null) {
            thisOnCreate = declaration.addFunction {
                name = Name.identifier("onCreate")
                returnType = irBuiltIns.unitType
            }.apply {
                overrides(superOnCreate)
                dispatchReceiverParameter = declaration.thisReceiver!!.copyTo(this)
                body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irBlockBody {
                        +irCall(superOnCreate, null, superClass.symbol).apply {
                            dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        }
                    }
                }
            }
        }

        val oldBody = thisOnCreate.body

        thisOnCreate.body = DeclarationIrBuilder(pluginContext, thisOnCreate.symbol).run {
            irBlockBody {
                +IrCallImpl(
                    startOffset + 1,
                    startOffset + 2,
                    irBuiltIns.unitType,
                    pluginContext.referenceFunctions(
                        FqName("com.ivianuu.injekt.composition.initializeCompositions")
                    ).single()
                )

                oldBody?.statements?.forEach { +it }
            }
        }

        return declaration
    }

}
