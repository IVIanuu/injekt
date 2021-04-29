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
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.js.resolve.diagnostics.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.constants.evaluate.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class GivenImportsChecker(private val context: InjektContext) : DeclarationChecker, CallChecker {
    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        val annotation = declaration.findAnnotation(InjektFqNames.GivenImports)
            ?: return
        descriptor.annotations.findAnnotation(InjektFqNames.GivenImports)?.allValueArguments
        val outerImports = descriptor.parents
            .distinct()
            .flatMap {
                it.findPsi()
                    .safeAs<KtAnnotated>()
                    ?.findAnnotation(InjektFqNames.GivenImports)
                    ?.valueArguments
                    ?.map { argument ->
                        GivenImport(
                            argument.asElement(),
                            argument.getArgumentExpression()
                                ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                                ?.toConstantValue(this.context.module.builtIns.stringType)
                                ?.value
                                ?.cast()
                        )
                    } ?: emptyList()
            }
            .toList()
        val imports = annotation
            .valueArguments
            .map { argument ->
                GivenImport(
                    argument.asElement(),
                    argument.getArgumentExpression()
                        ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                        ?.toConstantValue(this.context.module.builtIns.stringType)
                        ?.value
                        ?.cast()
                )
            }
        checkImports(outerImports, imports, context.trace)
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        if (resolvedCall.resultingDescriptor.fqNameSafe !=
            InjektFqNames.withGivenImports) return
        val outerImports = context.scope.parentsWithSelf
            .filterIsInstance<LexicalScope>()
            .distinctBy { it.ownerDescriptor }
            .flatMap {
                it.ownerDescriptor.findPsi()
                    .safeAs<KtAnnotated>()
                    ?.findAnnotation(InjektFqNames.GivenImports)
                    ?.valueArguments
                    ?.map { argument ->
                        GivenImport(
                            argument.asElement(),
                            argument.getArgumentExpression()
                                ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                                ?.toConstantValue(this.context.module.builtIns.stringType)
                                ?.value
                                ?.cast()
                        )
                    } ?: emptyList()
            }
            .toList()
        resolvedCall.valueArguments
            .values
            .firstOrNull()
            ?.arguments
            ?.map { argument ->
                GivenImport(
                    argument.asElement(),
                    argument.getArgumentExpression()
                        ?.let { ConstantExpressionEvaluator.getConstant(it, context.trace.bindingContext)}
                        ?.toConstantValue(this.context.module.builtIns.stringType)
                        ?.value
                        ?.cast()
                )
            }
            ?.let {
                checkImports(outerImports, it, context.trace)
            }

    }

    private fun checkImports(
        outerImports: List<GivenImport>,
        imports: List<GivenImport>,
        trace: BindingTrace
    ) {
        if (imports.isEmpty()) return

        imports
            .filter { it.importPath != null }
            .filter { import ->
                outerImports.any {
                    it.importPath == import.importPath
                }
            }
            .forEach { (element, _) ->
                trace.report(
                    InjektErrors.DUPLICATED_GIVEN_IMPORT
                        .on(element!!)
                )
            }

        imports
            .filter { it.importPath != null }
            .groupBy { it.importPath }
            .filter { it.value.size > 1 }
            .forEach { (_, imports) ->
                imports.forEach {
                    trace.report(
                        InjektErrors.DUPLICATED_GIVEN_IMPORT
                            .on(it.element!!)
                    )
                }
            }

        imports.forEach { import ->
            val (element, importPath) = import
            if (importPath == null) {
                trace.report(
                    InjektErrors.GIVEN_IMPORT_MUST_BE_CONSTANT
                        .on(element!!)
                )
                return@forEach
            }
            if (importPath
                    .any {
                        !it.isLetter() &&
                                it != '.' &&
                                it != '*'
                    }
            ) {
                trace.report(
                    InjektErrors.MALFORMED_GIVEN_IMPORT
                        .on(element!!)
                )
                return@forEach
            }
            if (importPath.endsWith(".*")) {
                val packageFqName = FqName(importPath.removeSuffix(".*"))
                if (context.memberScopeForFqName(packageFqName) == null) {
                    trace.report(
                        InjektErrors.UNRESOLVED_GIVEN_IMPORT
                            .on(element!!)
                    )
                    return@forEach
                } else if (listOf(import).collectImportGivens(context, trace).isEmpty()) {
                    trace.report(
                        InjektErrors.NON_GIVEN_IMPORT
                            .on(element!!)
                    )
                    return@forEach
                }
            } else {
                val fqName = FqName(importPath.removeSuffix(".*"))
                val parentFqName = fqName.parent()
                if (context.memberScopeForFqName(parentFqName) == null) {
                    trace.report(
                        InjektErrors.UNRESOLVED_GIVEN_IMPORT
                            .on(element!!)
                    )
                    return@forEach
                } else {
                    val shortName = fqName.shortName()
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
                                .on(element!!)
                        )
                        return@forEach
                    } else if (listOf(import).collectImportGivens(context, trace).isEmpty()) {
                        trace.report(
                            InjektErrors.NON_GIVEN_IMPORT
                                .on(element!!)
                        )
                        return@forEach
                    }
                }
            }

            if (!isIde) {
                trace.report(
                    InjektErrors.UNUSED_GIVEN_IMPORT
                        .on(element!!)
                )
            }
        }
    }
}
