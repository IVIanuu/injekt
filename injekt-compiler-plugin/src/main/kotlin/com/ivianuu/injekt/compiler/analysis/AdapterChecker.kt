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

package com.ivianuu.injekt.compiler.analysis

import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AdapterChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (descriptor is ClassDescriptor) checkAdapter(descriptor, declaration, context)
        if (descriptor is ClassDescriptor || descriptor is FunctionDescriptor)
            checkAdapterUsage(descriptor, declaration, context)
    }

    private fun checkAdapter(
        descriptor: ClassDescriptor,
        declaration: KtDeclaration,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotation(InjektFqNames.Adapter)) return

        val companion = descriptor.companionObjectDescriptor
        if (companion == null ||
            !companion.isSubclassOf(
                companion.module.findClassAcrossModuleDependencies(
                    ClassId.topLevel(InjektFqNames.Adapter)
                )!!.unsubstitutedInnerClassesScope
                    .getContributedDescriptors()
                    .filterIsInstance<ClassDescriptor>()
                    .single()
            )
        ) {
            context.trace.report(
                InjektErrors.ADAPTER_WITHOUT_COMPANION
                    .on(declaration)
            )
            return
        }
    }

    private fun checkAdapterUsage(
        descriptor: DeclarationDescriptor,
        declaration: KtDeclaration,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotatedAnnotations(InjektFqNames.Adapter, descriptor.module)) return

        val adapterAnnotations = listOfNotNull(
            descriptor.getAnnotatedAnnotations(InjektFqNames.Adapter, descriptor.module)
                .singleOrNull()
        )

        val upperBounds = adapterAnnotations
            .mapNotNull {
                it.type
                    .constructor
                    .declarationDescriptor
                    .let { it as ClassDescriptor }
                    .companionObjectDescriptor
                    ?.unsubstitutedMemberScope
                    ?.getContributedDescriptors()
                    ?.filterIsInstance<FunctionDescriptor>()
                    ?.firstOrNull()
                    ?.typeParameters
                    ?.singleOrNull()
                    ?.upperBounds
                    ?.singleOrNull()
            }

        val declarationType = when (descriptor) {
            is ClassDescriptor -> descriptor.defaultType
            is FunctionDescriptor -> descriptor.getFunctionType()
            else -> error("Unexpected descriptor $descriptor")
        }

        upperBounds.forEach { upperBound ->
            if (!declarationType.isSubtypeOf(upperBound)) {
                context.trace.report(
                    InjektErrors.NOT_IN_ADAPTER_BOUNDS
                        .on(declaration)
                )
            }
        }
    }

}

fun FunctionDescriptor.getFunctionType(): KotlinType {
    return (if (isSuspend) builtIns.getSuspendFunction(valueParameters.size)
    else builtIns.getFunction(valueParameters.size))
        .defaultType
        .replace(newArguments = valueParameters.map { it.type.asTypeProjection() } + returnType!!.asTypeProjection())
}
