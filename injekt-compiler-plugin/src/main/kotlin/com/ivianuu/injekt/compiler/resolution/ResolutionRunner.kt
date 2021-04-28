package com.ivianuu.injekt.compiler.resolution

interface ResolutionRunner {
    suspend fun computeResult(
        scope: ResolutionScope,
        requests: List<GivenRequest>
    ): GivenGraph
}

object CliResolutionRunner : ResolutionRunner {
    override suspend fun computeResult(
        scope: ResolutionScope,
        requests: List<GivenRequest>
    ): GivenGraph = with(scope) { resolveRequests(requests) }
}
