package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.descriptors.CallableDescriptor

fun <T : Any> resolveGivens(
    declarationStore: DeclarationStore,
    request: GivenRequest,
    initial: T,
    getGivens: T.(TypeRef) -> Pair<List<GivenNode>, T?>,
    getGivenCollectionElements: T.(TypeRef) -> Pair<List<CallableDescriptor>, T?>,
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

    val mapType = declarationStore.module.builtIns.map.defaultType.toTypeRef()
    val setType = declarationStore.module.builtIns.set.defaultType.toTypeRef()
    if (request.type.isSubTypeOf(mapType) || request.type.isSubTypeOf(setType)) {
        val elements = mutableListOf<CallableDescriptor>()
        var currentForElements: T? = initial
        while (currentForElements != null) {
            val (currentElements, next) = getGivenCollectionElements(currentForElements,
                request.type)
            elements += currentElements
            currentForElements = next
        }
        if (elements.isNotEmpty()) {
            return listOf(
                CollectionGivenNode(
                    request.type,
                    request.origin,
                    elements,
                    elements
                        .flatMap { element ->
                            element.getGivenRequests(request.type, declarationStore)
                        }
                )
            )
        }
    }

    return emptyList()
}
