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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    initialGivensInScope: () -> List<CallableDescriptor>,
    initialGivenSetElementsInScope: () -> List<CallableDescriptor>,
) {
    private val givens: MutableList<Pair<CallableDescriptor, ResolutionScope>> by unsafeLazy {
        (initialGivensInScope()
            .map { it to this } + (parent?.givens ?: emptyList()))
            .toMutableList()
    }
    private val givenSetElements: MutableList<Pair<CallableDescriptor, ResolutionScope>> by unsafeLazy {
        (initialGivenSetElementsInScope()
            .map { it to this } + (parent?.givenSetElements ?: emptyList()))
            .sortedBy { it.second.depth() }
            .toMutableList()
    }

    private val givensByType = mutableMapOf<TypeRef, List<GivenNode>>()
    fun givensForType(type: TypeRef): List<GivenNode> = givensByType.getOrPut(type) {
        givens
            .filter { it.first.returnType!!.toTypeRef().isAssignableTo(type) }
            .map { it.first.toGivenNode(type, declarationStore, it.second.depth()) }
    }

    private val givenSetElementsByType = mutableMapOf<TypeRef, List<CallableDescriptor>>()
    fun givenSetElementsForType(type: TypeRef): List<CallableDescriptor> =
        givenSetElementsByType.getOrPut(type) {
            givenSetElements
                .filter { it.first.returnType!!.toTypeRef().isAssignableTo(type) }
                .map { it.first }
        }

    fun addIfNeeded(callable: CallableDescriptor) {
        if (callable.hasAnnotation(InjektFqNames.Given)) givens += callable to this
        else if (callable.hasAnnotation(InjektFqNames.GivenSetElement)) givenSetElements += callable to this
    }

    fun addIfNeeded(clazz: ClassDescriptor) {
        givens += clazz.getGivenConstructors().map { it to this }
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
        callContext = CallContext.DEFAULT,
        parent = null,
        initialGivensInScope = {
            declarationStore.globalGivens
                .filter { it.isExternalDeclaration() }
                .filter { it.visibility == DescriptorVisibilities.PUBLIC }
        },
        initialGivenSetElementsInScope = {
            declarationStore.globalGivenSetElements
                .filter { it.isExternalDeclaration() }
        }
    )
}

fun InternalResolutionScope(
    parent: ResolutionScope,
    declarationStore: DeclarationStore,
): ResolutionScope {
    return ResolutionScope(
        name = "INTERNAL",
        declarationStore = declarationStore,
        callContext = CallContext.DEFAULT,
        parent = parent,
        initialGivensInScope = {
            declarationStore.globalGivens
                .filterNot { it.isExternalDeclaration() }
        },
        initialGivenSetElementsInScope = {
            declarationStore.globalGivenSetElements
                .filterNot { it.isExternalDeclaration() }
        }
    )
}

fun ClassResolutionScope(
    declarationStore: DeclarationStore,
    descriptor: ClassDescriptor,
    parent: ResolutionScope,
): ResolutionScope {
    return ResolutionScope(
        name = "CLASS(${descriptor.fqNameSafe})",
        declarationStore = declarationStore,
        callContext = CallContext.DEFAULT,
        parent = parent,
        initialGivensInScope = {
            descriptor
                .extractGivensOfDeclaration(declarationStore)
        },
        initialGivenSetElementsInScope = {
            descriptor.extractGivenSetElementsOfDeclaration()
        }
    )
}

fun FunctionResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    descriptor: FunctionDescriptor,
): ResolutionScope {
    return ResolutionScope(
        name = "FUN(${descriptor.fqNameSafe})",
        declarationStore = declarationStore,
        callContext = descriptor.callContext,
        parent = parent,
        initialGivensInScope = {
            descriptor.extractGivensOfCallable(declarationStore)
        },
        initialGivenSetElementsInScope = {
            descriptor.extractGivenSetElementsOfCallable()
        }
    )
}

fun BlockResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
): ResolutionScope {
    return ResolutionScope(
        name = "BLOCK",
        declarationStore = declarationStore,
        callContext = parent.callContext,
        parent = parent,
        initialGivensInScope = { emptyList() },
        initialGivenSetElementsInScope = { emptyList() }
    )
}
