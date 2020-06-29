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
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AliasChecker : DeclarationChecker {

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        if (!descriptor.hasAnnotation(InjektFqNames.Alias)) return
        descriptor as ClassDescriptor

        if (!descriptor.hasAnnotatedAnnotations(
                InjektFqNames.BindingAdapter,
                descriptor.module
            ) &&
            !descriptor.hasAnnotation(InjektFqNames.Transient) &&
            !descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope, descriptor.module)
        ) {
            context.trace.report(
                InjektErrors.ALIAS_ANNOTATION_WITHOUT_TRANSIENT_OR_SCOPED
                    .on(declaration)
            )
        }

        val aliasType = descriptor.annotations
            .findAnnotation(InjektFqNames.Alias)!!.type.arguments.single().type

        if (!descriptor.defaultType.isSubtypeOf(aliasType)) {
            context.trace.report(InjektErrors.MUST_BE_SUBTYPE_OF_ALIAS_TYPE
                .on(declaration))
        }
    }

}
