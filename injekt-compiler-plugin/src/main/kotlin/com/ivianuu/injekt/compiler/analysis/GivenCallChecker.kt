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
import kotlinx.coroutines.*
import org.jetbrains.kotlin.com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.inline.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.*

class GivenCallChecker(
    private val context: InjektContext,
    private val resolutionRunner: ResolutionRunner
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
            .map { parameter ->
                GivenRequest(
                    type = callable.parameterTypes[parameter.name.asString()]!!,
                    defaultStrategy = if (parameter is ValueParameterDescriptor &&
                        parameter.hasDefaultValueIgnoringGiven) {
                        if (parameter.injektName() in callable.defaultOnAllErrorParameters)
                            GivenRequest.DefaultStrategy.DEFAULT_ON_ALL_ERRORS
                        else GivenRequest.DefaultStrategy.DEFAULT_IF_NOT_GIVEN
                    } else GivenRequest.DefaultStrategy.NONE,
                    callableFqName = resultingDescriptor.fqNameSafe,
                    parameterName = parameter.name,
                    isInline = InlineUtil.isInlineParameter(parameter),
                    isLazy = false,
                    requestDescriptor = context.scope.ownerDescriptor.cast()
                )
            }
            .toList()

        if (requests.isEmpty()) return

        val callExpression = resolvedCall.call.callElement

        val (initTime, scope) = measureTimeMillisWithResult {
            HierarchicalResolutionScope(this@GivenCallChecker.context, context.scope.takeSnapshot(),
                "", context.trace)
        }

        println("initializing scope ${scope.name} took $initTime ms")

        if (!isIde) {
            scope.recordLookup(KotlinLookupLocation(callExpression))
        }

        when (val graph = runBlocking { resolutionRunner.computeResult(scope, requests) }) {
            is GivenGraph.Success -> {
                try {
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

                    graph.visitRecursive { _, result ->
                        if (result.candidate is CallableGivenNode) {
                            context.trace.record(
                                InjektWritableSlices.USED_GIVEN,
                                result.candidate.callable.callable,
                                Unit
                            )
                            val existingUsedGivensForFile =
                                context.trace.bindingContext[InjektWritableSlices.USED_GIVENS_FOR_FILE,
                                        callExpression.containingKtFile.virtualFilePath] ?: emptyList()
                            context.trace.record(
                                InjektWritableSlices.USED_GIVENS_FOR_FILE,
                                callExpression.containingKtFile.virtualFilePath,
                                existingUsedGivensForFile + result.candidate.callable.callable
                            )
                        }
                    }
                } catch (e: Throwable) {
                }
            }
            is GivenGraph.Error -> context.trace.report(
                InjektErrors.UNRESOLVED_GIVEN
                    .on(reportOn, graph)
            )
        }
    }
}
