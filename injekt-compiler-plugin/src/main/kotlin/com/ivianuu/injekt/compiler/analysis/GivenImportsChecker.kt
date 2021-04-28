/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class GivenImportsChecker(
    private val context: InjektContext
) : DeclarationChecker, CallChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        val annotation = declaration.findAnnotation(InjektFqNames.GivenImports)
            ?: return
        val imports = annotation
            .valueArguments
            .map { argument ->
                (argument.asElement() to argument.getArgumentExpression()
                    ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                    ?.toConstantValue(this.context.module.builtIns.stringType)
                    ?.value
                    ?.cast<String>())
            }
        checkImports(imports, context.trace)
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe !=
            InjektFqNames.withGivenImports) return
        resolvedCall.valueArguments
            .values
            .firstOrNull()
            ?.arguments
            ?.map { argument ->
                (argument.asElement() to argument.getArgumentExpression()
                    ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                    ?.toConstantValue(this.context.module.builtIns.stringType)
                    ?.value
                    ?.cast<String>())
            }
            ?.let { checkImports(it, context.trace) }

    }

    private fun checkImports(
        imports: List<Pair<KtElement, String?>>,
        trace: BindingTrace
    ) {
        if (imports.isEmpty()) return
        imports.forEach { (element, import) ->
            if (import == null) {
                trace.report(
                    InjektErrors.GIVEN_IMPORT_MUST_BE_CONSTANT
                        .on(element)
                )
                return@forEach
            }
            if (import
                    .any {
                        !it.isLetter() &&
                                it != '.' &&
                                it != '*'
                    }
            ) {
                trace.report(
                    InjektErrors.MALFORMED_GIVEN_IMPORT
                        .on(element)
                )
                return@forEach
            }
            if (import.endsWith(".*")) {
                val packageFqName = FqName(import.removeSuffix(".*"))
                if (context.memberScopeForFqName(packageFqName) == null) {
                    trace.report(
                        InjektErrors.UNRESOLVED_GIVEN_IMPORT
                            .on(element)
                    )
                }
            } else {
                val fqName = FqName(import.removeSuffix(".*"))
                val shortName = fqName.shortName()
                val parentFqName = fqName.parent()
                val importedDeclarations = context.memberScopeForFqName(parentFqName)
                    ?.getContributedDescriptors()
                    ?.filter {
                        it.name == shortName ||
                                (it is ClassConstructorDescriptor &&
                                        it.constructedClass.name == shortName)
                    }
                if (importedDeclarations == null || importedDeclarations.isEmpty()) {
                    trace.report(
                        InjektErrors.UNRESOLVED_GIVEN_IMPORT
                            .on(element)
                    )
                }
            }
        }
    }
}
