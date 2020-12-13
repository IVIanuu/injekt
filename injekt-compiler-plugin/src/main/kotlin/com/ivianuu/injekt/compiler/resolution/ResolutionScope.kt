package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    initialGivensInScope: () -> List<CallableDescriptor>,
    initialGivenCollectionElementsInScope: () -> List<CallableDescriptor>,
) {
    private val givens: MutableList<Pair<CallableDescriptor, ResolutionScope>> by unsafeLazy {
        (initialGivensInScope()
            .map { it to this } + (parent?.givens ?: emptyList()))
            .toMutableList()
    }
    private val givenCollectionElements: MutableList<Pair<CallableDescriptor, ResolutionScope>> by unsafeLazy {
        (initialGivenCollectionElementsInScope()
            .map { it to this } + (parent?.givenCollectionElements ?: emptyList()))
            .sortedBy { it.second.depth() }
            .toMutableList()
    }

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    fun givensForType(type: TypeRef): List<GivenNode> = givensByType.getOrPut(type) {
        givens
            .filter { it.first.returnType!!.toTypeRef().isAssignableTo(type) }
            .map { it.first.toGivenNode(type, declarationStore, it.second.depth()) }
    }

    private val givenCollectionElementsByType = mutableMapOf<TypeRef, List<CallableDescriptor>>()
    fun givenCollectionElementsForType(type: TypeRef): List<CallableDescriptor> =
        givenCollectionElementsByType.getOrPut(type) {
            givenCollectionElements
                .filter { it.first.returnType!!.toTypeRef().isAssignableTo(type) }
                .map { it.first }
        }

    fun addIfNeeded(callable: CallableDescriptor) {
        if (callable.hasAnnotation(InjektFqNames.Given)) givens += callable to this
        else if (callable.hasAnnotation(InjektFqNames.GivenMap) ||
            callable.hasAnnotation(InjektFqNames.GivenSet)
        ) givenCollectionElements += callable to this
    }

    private fun ResolutionScope.depth(): Int {
        var scope: ResolutionScope = this@ResolutionScope
        var depth = 0
        while (scope != this) {
            depth++
            scope = scope.parent!!
        }
        return depth
    }

    override fun toString(): String = "ResolutionScope($name)"
}

fun ExternalResolutionScope(declarationStore: DeclarationStore): ResolutionScope {
    return ResolutionScope(
        name = "EXTERNAL",
        declarationStore = declarationStore,
        parent = null,
        initialGivensInScope = {
            declarationStore.allGivens
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
        },
        initialGivenCollectionElementsInScope = {
            declarationStore.allGivenCollectionElements
                .filter { it.isExternalDeclaration() }
        }
    )
}

fun InternalResolutionScope(
    parent: ResolutionScope?,
    declarationStore: DeclarationStore,
): ResolutionScope {
    return ResolutionScope(
        name = "INTERNAL",
        declarationStore = declarationStore,
        parent = parent,
        initialGivensInScope = {
            declarationStore.allGivens
                .filterNot { it.isExternalDeclaration() }
        },
        initialGivenCollectionElementsInScope = {
            declarationStore.allGivenCollectionElements
                .filterNot { it.isExternalDeclaration() }
        }
    )
}

fun ClassResolutionScope(
    bindingContext: BindingContext,
    declarationStore: DeclarationStore,
    descriptor: ClassDescriptor,
    parent: ResolutionScope?,
): ResolutionScope {
    return ResolutionScope(
        name = "CLASS(${descriptor.fqNameSafe})",
        declarationStore = declarationStore,
        parent = parent,
        initialGivensInScope = {
            descriptor
                .extractGivensOfDeclaration(declarationStore)
        },
        initialGivenCollectionElementsInScope = {
            descriptor.extractGivenCollectionElementsOfDeclaration()
        }
    )
}

fun FunctionResolutionScope(
    bindingContext: BindingContext,
    declarationStore: DeclarationStore,
    parent: ResolutionScope?,
    descriptor: FunctionDescriptor,
): ResolutionScope {
    return ResolutionScope(
        name = "FUN(${descriptor.fqNameSafe})",
        declarationStore = declarationStore,
        parent = parent,
        initialGivensInScope = {
            descriptor.extractGivensOfCallable(bindingContext,
                declarationStore)
        },
        initialGivenCollectionElementsInScope = {
            descriptor.extractGivenCollectionElementsOfCallable()
        }
    )
}

fun BlockResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope?,
): ResolutionScope {
    return ResolutionScope(
        name = "BLOCK",
        declarationStore = declarationStore,
        parent = parent,
        initialGivensInScope = { emptyList() },
        initialGivenCollectionElementsInScope = { emptyList() }
    )
}
