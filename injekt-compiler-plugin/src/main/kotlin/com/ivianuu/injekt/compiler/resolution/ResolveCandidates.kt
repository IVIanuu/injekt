package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import org.jetbrains.kotlin.descriptors.CallableDescriptor

fun ResolutionScope.resolveGivenCandidates(
    declarationStore: DeclarationStore,
    request: GivenRequest,
): List<GivenNode> {
    var currentScope: ResolutionScope? = this
    while (currentScope != null) {
        val givens = currentScope.givensForTypeInThisScope(request.type)
        if (givens.isNotEmpty()) return givens
        currentScope = currentScope.parent
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
        var currentForElements: ResolutionScope? = this
        while (currentForElements != null) {
            val currentElements = currentForElements
                .givenCollectionElementsForTypeInThisScope(request.type)
            elements += currentElements
            currentForElements = currentForElements.parent
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
