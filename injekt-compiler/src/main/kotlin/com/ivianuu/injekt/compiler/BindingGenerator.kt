/*
 * Copyright 2019 Manuel Wrage
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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.Printer

class BindingGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        val descriptor = declaration.descriptor

        if (descriptor.annotations.hasAnnotation(FactoryAnnotation) ||
            descriptor.annotations.hasAnnotation(SingleAnnotation)) return

        val linkedBinding = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.UnlinkedBinding))!!

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("${descriptor.name}__Binding"),
            Modality.FINAL,
            ClassKind.OBJECT,
            listOf(linkedBinding.defaultType),
            SourceElement.NO_SOURCE,
            false,
            LockBasedStorageManager.NO_LOCKS
        ).apply {
            initialize(
                object : MemberScopeImpl() {
                    override fun printScopeStructure(p: Printer) {
                    }
                },
                emptySet(),
                null
            )
        }

        val clazz = IrClassImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrClassSymbolImpl(bindingDescriptor)
        )

        declaration.addMember(clazz)
    }
}

private object InjektOrigin : IrDeclarationOrigin

private object InjektClassNames {
    val HasScope = FqName("com.ivianuu.injekt.HasScope")
    val IsSingle = FqName("com.ivianuu.injekt.IsSingle")
    val Key = FqName("com.ivianuu.injekt.Key")
    val LinkedBinding = FqName("com.ivianuu.injekt.LinkedBinding")
    val Linker = FqName("com.ivianuu.injekt.Linker")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val UnlinkedBinding = FqName("com.ivianuu.injekt.UnlinkedBinding")
}
