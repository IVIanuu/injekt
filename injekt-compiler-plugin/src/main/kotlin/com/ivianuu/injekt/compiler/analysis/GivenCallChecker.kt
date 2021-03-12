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

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektErrors
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.SourcePosition
import com.ivianuu.injekt.compiler.resolution.CallableGivenNode
import com.ivianuu.injekt.compiler.resolution.CandidateResolutionResult
import com.ivianuu.injekt.compiler.resolution.GivenGraph
import com.ivianuu.injekt.compiler.resolution.GivenRequest
import com.ivianuu.injekt.compiler.resolution.HierarchicalResolutionScope
import com.ivianuu.injekt.compiler.resolution.isGiven
import com.ivianuu.injekt.compiler.resolution.resolveGiven
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GivenCallChecker(
    private val declarationStore: DeclarationStore,
    private val bindingContextCollector: ((BindingContext) -> Unit)?
) : CallChecker {

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        bindingContextCollector?.invoke(context.trace.bindingContext)
        val resultingDescriptor = resolvedCall.resultingDescriptor
        if (resultingDescriptor !is FunctionDescriptor) return

        val callExpression = resolvedCall.call.callElement

        val requests = resolvedCall
            .valueArguments
            .filterValues { it is DefaultValueArgument }
            .filterKeys { it.isGiven(declarationStore) }
            .map {
                GivenRequest(
                    type = it.key.type.toTypeRef(declarationStore),
                    required = !it.key.hasDefaultValueIgnoringGiven,
                    callableFqName = resultingDescriptor.fqNameSafe,
                    parameterName = it.key.name
                )
            }

        if (requests.isEmpty()) return

        val scope = HierarchicalResolutionScope(declarationStore, context.scope,
            context.trace.bindingContext)

        when (val graph = scope.resolveGiven(requests)) {
            is GivenGraph.Success -> {
                context.trace.record(
                    InjektWritableSlices.GIVEN_CALLS_IN_FILE,
                    callExpression.containingKtFile.virtualFilePath,
                    Unit
                )
                val visited = mutableSetOf<CandidateResolutionResult.Success>()
                fun CandidateResolutionResult.Success.visit() {
                    if (!visited.add(this)) return
                    if (candidate is CallableGivenNode) {
                        context.trace.record(
                            InjektWritableSlices.USED_GIVEN,
                            candidate.callable.callable,
                            Unit
                        )
                        val existingUsedGivensForFile =
                            context.trace.bindingContext[InjektWritableSlices.USED_GIVENS_FOR_FILE,
                                    callExpression.containingKtFile] ?: emptyList()
                        context.trace.record(
                            InjektWritableSlices.USED_GIVENS_FOR_FILE,
                            callExpression.containingKtFile,
                            existingUsedGivensForFile + candidate.callable.callable
                        )
                    }
                    dependencyResults.forEach { it.value.visit() }
                }
                graph.results.forEach { it.value.visit() }

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