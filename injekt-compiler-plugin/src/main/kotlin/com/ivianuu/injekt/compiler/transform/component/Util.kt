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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.uniqueName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun FqName?.orUnknown(): String = this?.asString() ?: "unknown origin"

fun IrClass.getReaderSignature() = functions
    .single { it.name.asString() == "signature" }

fun IrPluginContext.getReaderInfo(declaration: IrDeclarationWithName): IrClass? {
    val declaration = if (declaration is IrConstructor)
        declaration.constructedClass else declaration

    return if (declaration.isExternalDeclaration()) {
        moduleDescriptor.getPackage(declaration.getPackageFragment()!!.fqName)
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.hasAnnotation(InjektFqNames.Name) }
            .singleOrNull {
                (it.annotations.findAnnotation(InjektFqNames.Name)!!
                    .argumentValue("value")
                    .let { it as StringValue }
                    .value == declaration.uniqueName())
            }
            ?.let { referenceClass(it.fqNameSafe)?.owner }
    } else {
        declaration.getPackageFragment()!!
            .declarations
            .filterIsInstance<IrClass>()
            .filter { it.hasAnnotation(InjektFqNames.Name) }
            .singleOrNull { clazz ->
                val name = clazz.annotations.findAnnotation(InjektFqNames.Name)!!
                    .getValueArgument(0)!!
                    .let { it as IrConst<String> }
                    .value
                (name == declaration.uniqueName())
            }
    }
}
