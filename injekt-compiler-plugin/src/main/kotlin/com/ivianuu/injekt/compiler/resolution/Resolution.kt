package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.descriptors.CallableDescriptor

fun <T : Any> resolveGivens(
    declarationStore: DeclarationStore,
    request: GivenRequest,
    initial: T,
    getGivens: T.(TypeRef) -> Pair<List<GivenNode>, T?>,
    getGivenSets: T.(TypeRef) -> Pair<List<CallableDescriptor>, T?>,
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

    val setType = declarationStore.module.builtIns.set.defaultType.toTypeRef()
    if (request.type.isSubTypeOf(setType)) {
        val setElements = mutableListOf<CallableDescriptor>()
        var currentForSetElements: T? = initial
        while (currentForSetElements != null) {
            val (currentSetElements, next) = getGivenSets(currentForSetElements, request.type)
            setElements += currentSetElements
            currentForSetElements = next
        }
        if (setElements.isNotEmpty()) {
            return listOf(
                SetGivenNode(
                    request.type,
                    request.origin,
                    setElements,
                    setElements
                        .flatMap { element ->
                            element.getGivenRequests(request.type, declarationStore)
                        }
                )
            )
        }
    }

    return emptyList()
}
