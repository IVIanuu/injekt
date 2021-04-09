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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.HierarchicalResolutionScope
import com.ivianuu.injekt.compiler.resolution.isGiven
import com.ivianuu.injekt.compiler.resolution.resolveRequests
import com.ivianuu.injekt.compiler.resolution.substitute
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.utils.addToStdlib.cast

class GivenCallChecker(
    private val context: InjektContext,
    private val allowGivenCalls: (KtElement) -> Boolean
) : CallChecker {
    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor) return

        if (resultingDescriptor.valueParameters.none {
                it.isGiven(this.context, context.trace)
        }) return

        val substitutionMap = resolvedCall.typeArguments
            .mapKeys { it.key.toClassifierRef(this.context, context.trace) }
            .mapValues { it.value.toTypeRef(this.context, context.trace) }
            .filter { it.key != it.value.classifier }

        val callable = resultingDescriptor.toCallableRef(this.context, context.trace)
            .substitute(substitutionMap)

        val requests = callable.givenParameters
            .asSequence()
            .map { parameterName ->
                callable.callable.valueParameters.single {
                    it.name.asString() == parameterName
                }
            }
            .filter { resolvedCall.valueArguments[it] is DefaultValueArgument }
            .map { parameterDescriptor ->
                GivenRequest(
                    type = callable.parameterTypes[parameterDescriptor.name.asString()]!!,
                    isRequired = !parameterDescriptor.hasDefaultValueIgnoringGiven,
                    callableFqName = resultingDescriptor.fqNameSafe,
                    parameterName = parameterDescriptor.name,
                    isInline = InlineUtil.isInlineParameter(parameterDescriptor),
                    isLazy = false,
                    requestDescriptor = context.scope.ownerDescriptor.cast()
                )
            }
            .toList()

        if (requests.isEmpty()) return

        val callExpression = resolvedCall.call.callElement

        if (!allowGivenCalls(callExpression) &&
                requests.any { it.isRequired }) {
            context.trace.report(
                InjektErrors.GIVEN_CALLS_NOT_ALLOWED
                    .on(callExpression)
            )
            return
        }

        // todo
        if (Project::class.java.name == "com.intellij.openapi.project.Project") return

        val scope = HierarchicalResolutionScope(this.context, context.scope, context.trace)
        scope.recordLookup(KotlinLookupLocation(callExpression))

        when (val graph = scope.resolveRequests(requests) {
            if (it.candidate is CallableGivenNode) {
                context.trace.record(
                    InjektWritableSlices.USED_GIVEN,
                    it.candidate.callable.callable,
                    Unit
                )
                val existingUsedGivensForFile =
                    context.trace.bindingContext[InjektWritableSlices.USED_GIVENS_FOR_FILE,
                            callExpression.containingKtFile.virtualFilePath] ?: emptyList()
                context.trace.record(
                    InjektWritableSlices.USED_GIVENS_FOR_FILE,
                    callExpression.containingKtFile.virtualFilePath,
                    existingUsedGivensForFile + it.candidate.callable.callable
                )
            }
        }) {
            is GivenGraph.Success -> {
                context.trace.record(
                    InjektWritableSlices.FILE_HAS_GIVEN_CALLS,
                    callExpression.containingKtFile.virtualFilePath,
                    Unit
                )
                context.trace.record(
                    InjektWritableSlices.GIVEN_GRAPH,
                    SourcePosition(
                        callExpression.containingKtFile.virtualFilePath,
                        callExpression.startOffset,
                        callExpression.endOffset
                    ),
                    graph
                )
            }
            is GivenGraph.Error -> context.trace.report(
                InjektErrors.UNRESOLVED_GIVEN
                    .on(reportOn, graph)
            )
        }
    }
}