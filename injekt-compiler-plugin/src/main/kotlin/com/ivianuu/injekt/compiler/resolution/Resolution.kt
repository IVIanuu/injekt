package com.ivianuu.injekt.compiler.resolution

fun <T : Any> resolveGivens(
    request: GivenRequest,
    initial: T,
    getGivens: T.(TypeRef) -> Pair<List<GivenNode>, T?>,
): List<GivenNode> {
    var current: T? = initial
    while (current != null) {
        val (givens, next) = getGivens(current, request.type)
        if (givens.isNotEmpty()) return givens
        current = next
    }

    if (request.type.classifier.fqName.asString() == "kotlin.Function0") {
        return listOf(
            ProviderGivenNode(
                request.type,
                request.origin,
                request.required
            )
        )
    }

    return emptyList()
}
