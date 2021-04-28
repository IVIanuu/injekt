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

package com.ivianuu.injekt.ide

import com.ivianuu.injekt.compiler.resolution.*
import kotlinx.coroutines.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.util.concurrent.*

object IdeResolutionRunner : ResolutionRunner {
    private val resultsByScope = mutableMapOf<String, InflightGraph>()

    private data class InflightGraph(
        val scope: ResolutionScope,
        val completable: CompletableDeferred<GivenGraph>
    )

    private val dispatcher = Executors.newCachedThreadPool()
        .asCoroutineDispatcher()

    override suspend fun computeResult(
        scope: ResolutionScope,
        requests: List<GivenRequest>
    ): GivenGraph {
        val (graph, compute) = synchronized(this) {
            var existing = resultsByScope[scope.key]
            if (existing != null && existing.scope != scope) {
                println("CACHE cancel invalid in flight result for ${scope.key} -> $requests")
                existing.completable.cancel()
                resultsByScope -= scope.key
                existing = null
            }
            existing?.let { it to false }
                ?: InflightGraph(scope, CompletableDeferred())
                    .also { resultsByScope[scope.key] = it } to true
        }
        if (!compute) {
            println("CACHE await in flight result for ${scope.key} -> $requests")
            return graph.completable.await()
        }
        println("CACHE compute fresh result for ${scope.key} -> $requests")
        val (took, result) = measureTimeMillisWithResult {
            withContext(dispatcher) {
                with(scope) { resolveRequests(requests) }
            }
        }
        println("CACHE computed result took $took ms ${scope.key} -> $requests")
        graph.completable.complete(result)
        return result
    }
}
