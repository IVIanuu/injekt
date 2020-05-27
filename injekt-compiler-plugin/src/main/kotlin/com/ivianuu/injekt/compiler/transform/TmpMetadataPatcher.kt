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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.QualifierPreservingTypeRemapper
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.DescriptorsRemapper
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils

// todo once we can use FIR
class TmpMetadataPatcher(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        (declaration as IrFileImpl).metadata =
            MetadataSource.File(declaration.declarations.map { it.descriptor })
        return super.visitFile(declaration)
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val declaration = super.visitClass(declaration) as IrClass

        if (declaration.metadata == null ||
            declaration.visibility == Visibilities.LOCAL
        ) return declaration

        val descriptor = declaration.descriptor
        if (descriptor is WrappedDeclarationDescriptor<*>)
            return declaration

        val additionalFunctions = declaration.declarations
            .filterIsInstance<IrFunction>()
            .filter { it.descriptor is WrappedDeclarationDescriptor<*> }
            .filter { it.metadata != null }
            .filter {
                it.descriptor !in descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
            }
            .map { it.descriptor }

        val additionalInnerClasses = declaration.declarations
            .filterIsInstance<IrClass>()
            .filter { it.descriptor is WrappedDeclarationDescriptor<*> }
            .filter { it.metadata != null }
            .filter {
                it.descriptor !in descriptor.unsubstitutedInnerClassesScope
                    .getContributedDescriptors()
            }
            .map { it.descriptor }

        if (additionalFunctions.isEmpty() && additionalInnerClasses.isEmpty()) return declaration

        return declaration.copyWithAdditionalDescriptor(
            additionalFunctions, additionalInnerClasses
        )
    }

    private class WrappedClassDescriptorWithDelegate(
        private val delegate: ClassDescriptor,
        private val additionalFunctions: List<FunctionDescriptor>,
        private val additionalInnerClasses: List<ClassDescriptor>
    ) : WrappedDeclarationDescriptor<IrClass>(delegate.annotations), ClassDescriptor by delegate {

        private val memberScope = if (additionalFunctions.isNotEmpty()) {
            AdditionalDescriptorMemberScope(
                additionalFunctions,
                delegate.unsubstitutedMemberScope
            )
        } else delegate.unsubstitutedMemberScope

        private val innerClassesScope = if (additionalInnerClasses.isNotEmpty()) {
            AdditionalDescriptorMemberScope(
                additionalInnerClasses,
                delegate.unsubstitutedInnerClassesScope
            )
        } else delegate.unsubstitutedInnerClassesScope

        private val _defaultType: SimpleType by lazy {
            TypeUtils.makeUnsubstitutedType(
                this,
                unsubstitutedMemberScope,
                KotlinTypeFactory.EMPTY_REFINED_TYPE_FACTORY
            )
        }

        override fun getDefaultType(): SimpleType = _defaultType

        override fun getCompanionObjectDescriptor(): ClassDescriptor? {
            return additionalInnerClasses.firstOrNull {
                it.isCompanionObject
            } ?: delegate.companionObjectDescriptor
        }

        override fun getOriginal(): ClassDescriptor = this

        override fun getUnsubstitutedMemberScope(): MemberScope =
            memberScope

        override fun getUnsubstitutedInnerClassesScope(): MemberScope =
            innerClassesScope

        private class AdditionalDescriptorMemberScope(
            private val additionalDescriptors: List<DeclarationDescriptor>,
            private val delegate: MemberScope
        ) : MemberScope by delegate {
            override fun getClassifierNames(): Set<Name>? {
                return (delegate.getClassifierNames() ?: emptySet()) + additionalDescriptors
                    .filterIsInstance<ClassifierDescriptor>()
                    .map { it.name }
            }

            override fun getContributedClassifier(
                name: Name,
                location: LookupLocation
            ): ClassifierDescriptor? {
                return (additionalDescriptors.singleOrNull { it.name == name && it is ClassifierDescriptor } as? ClassifierDescriptor)
                    ?: delegate.getContributedClassifier(name, location)
            }

            override fun getContributedDescriptors(
                kindFilter: DescriptorKindFilter,
                nameFilter: (Name) -> Boolean
            ): Collection<DeclarationDescriptor> {
                return additionalDescriptors + delegate.getContributedDescriptors(
                    kindFilter,
                    nameFilter
                )
                    .filter { original ->
                        when (original) {
                            is ClassifierDescriptor -> {
                                additionalDescriptors
                                    .filterIsInstance<ClassifierDescriptor>()
                                    .none { it.name == original.name }
                            }
                            is FunctionDescriptor -> {
                                additionalDescriptors
                                    .filterIsInstance<FunctionDescriptor>()
                                    .none {
                                        it.name == original.name &&
                                                it.returnType?.constructor?.declarationDescriptor ==
                                                original.returnType?.constructor?.declarationDescriptor
                                        it.valueParameters.size == original.valueParameters.size &&
                                                it.valueParameters.all { p ->
                                                    p.type.constructor.declarationDescriptor ==
                                                            original.valueParameters[p.index].type.constructor.declarationDescriptor
                                                }
                                    }
                            }
                            else -> true
                        }
                    }
            }

            override fun getContributedFunctions(
                name: Name,
                location: LookupLocation
            ): Collection<SimpleFunctionDescriptor> {
                return additionalDescriptors.filterIsInstance<SimpleFunctionDescriptor>() + delegate.getContributedFunctions(
                    name,
                    location
                )
                    .filter { original ->
                        additionalDescriptors
                            .filterIsInstance<FunctionDescriptor>()
                            .none {
                                it.name == original.name &&
                                        it.returnType?.constructor?.declarationDescriptor ==
                                        original.returnType?.constructor?.declarationDescriptor
                                it.valueParameters.size == original.valueParameters.size &&
                                        it.valueParameters.all { p ->
                                            p.type.constructor.declarationDescriptor ==
                                                    original.valueParameters[p.index].type.constructor.declarationDescriptor
                                        }
                            }
                    }
            }

            override fun getFunctionNames(): Set<Name> {
                return delegate.getFunctionNames() +
                        additionalDescriptors.filterIsInstance<SimpleFunctionDescriptor>()
                            .map { it.name }
            }

        }
    }

    private fun IrClass.copyWithAdditionalDescriptor(
        additionalFunctions: List<FunctionDescriptor>,
        additionalInnerClasses: List<ClassDescriptor>
    ): IrClass {
        val mappedDescriptors = mutableSetOf<WrappedClassDescriptorWithDelegate>()
        val symbolRemapper = DeepCopySymbolRemapper(
            object : DescriptorsRemapper {
                override fun remapDeclaredClass(descriptor: ClassDescriptor): ClassDescriptor {
                    return if (descriptor == this@copyWithAdditionalDescriptor.descriptor) {
                        WrappedClassDescriptorWithDelegate(
                            descriptor,
                            additionalFunctions,
                            additionalInnerClasses
                        ).also { mappedDescriptors += it }
                    } else descriptor
                }
            }
        )
        acceptVoid(symbolRemapper)
        val typeRemapper = QualifierPreservingTypeRemapper(symbolRemapper)
        return (transform(
            object : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {
                override fun visitClass(declaration: IrClass): IrClass {
                    return super.visitClass(declaration).also {
                        if (declaration.metadata != null) {
                            (it as IrClassImpl).metadata = MetadataSource.Class(it.descriptor)
                        }
                    }
                }
            }.also { typeRemapper.deepCopy = it }, null
        ).patchDeclarationParents(parent) as IrClass)
    }

}
