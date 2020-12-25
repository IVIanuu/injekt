package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.isExternalDeclaration
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ResolutionScope(
    val name: String,
    val parent: ResolutionScope?,
    val declarationStore: DeclarationStore,
    val callContext: CallContext,
    declarations: List<CallableRef>,
) {
    private val givens = mutableListOf<Pair<CallableRef, ResolutionScope>>()
    private val givenSetElements = mutableListOf<CallableRef>()
    private val interceptors = mutableListOf<CallableRef>()

    val chain: MutableSet<GivenNode> = parent?.chain ?: mutableSetOf()
    val resultsByRequest = mutableMapOf<GivenRequest, ResolutionResult>()
    val resultsByCandidate = mutableMapOf<GivenNode, CandidateResolutionResult>()

    init {
        parent?.givens?.forEach { givens += it }
        parent?.givenSetElements?.forEach { givenSetElements += it }
        declarations.forEach { declaration ->
            declaration.collectGivens(
                path = listOf(declaration.callable.fqNameSafe),
                addGiven = { givens += it to this },
                addGivenSetElement = { givenSetElements += it },
                addInterceptor = { interceptors += it }
            )
        }
    }

    fun givensForType(type: TypeRef): List<GivenNode> = givens
        .filter { it.first.type.isAssignableTo(type) }
        .map { it.first.toGivenNode(type, it.second) }

    fun givenSetElementsForType(type: TypeRef): List<CallableRef> = givenSetElements
        .filter { it.type.isAssignableTo(type) }
        .map { it.substitute(getSubstitutionMap(listOf(type to it.type))) }

    fun interceptorsForType(type: TypeRef): List<InterceptorNode> = interceptors
        .filter { it.type.isAssignableTo(type) }
        .map { it.substitute(getSubstitutionMap(listOf(type to it.type))) }
        .map {
            InterceptorNode(
                it,
                it.getGivenRequests(false)
            )
        }

    override fun toString(): String = "ResolutionScope($name)"
}

fun ExternalResolutionScope(declarationStore: DeclarationStore): ResolutionScope = ResolutionScope(
    name = "EXTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = null,
    declarations = declarationStore.globalGivenDeclarations
        .filter { it.callable.isExternalDeclaration() }
        .filter { it.callable.visibility == DescriptorVisibilities.PUBLIC }
)

fun InternalResolutionScope(
    parent: ResolutionScope,
    declarationStore: DeclarationStore,
): ResolutionScope = ResolutionScope(
    name = "INTERNAL",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = parent,
    declarations = declarationStore.globalGivenDeclarations
        .filterNot { it.callable.isExternalDeclaration() }
)

fun ClassResolutionScope(
    declarationStore: DeclarationStore,
    descriptor: ClassDescriptor,
    parent: ResolutionScope,
): ResolutionScope = ResolutionScope(
    name = "CLASS(${descriptor.fqNameSafe})",
    declarationStore = declarationStore,
    callContext = CallContext.DEFAULT,
    parent = parent,
    declarations = descriptor.unsubstitutedMemberScope
        .collectContributions(descriptor.defaultType.toTypeRef()) +
            CallableRef(descriptor.thisAsReceiverParameter, contributionKind = ContributionKind.VALUE)
)

fun FunctionResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    descriptor: FunctionDescriptor,
    lambdaType: TypeRef?,
) = ResolutionScope(
    name = "FUN(${descriptor.fqNameSafe})",
    declarationStore = declarationStore,
    callContext = lambdaType?.callContext ?: descriptor.callContext,
    parent = parent,
    declarations = descriptor.collectContributions()
)

fun LocalDeclarationResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
    declaration: DeclarationDescriptor
): ResolutionScope {
    val declarations: List<CallableRef> = when (declaration) {
        is ClassDescriptor -> declaration.getGivenDeclarationConstructors()
        is CallableDescriptor -> declaration
            .contributionKind()
            ?.let { listOf(CallableRef(declaration, contributionKind = it)) }
        else -> null
    } ?: return parent
    return ResolutionScope(
        name = "LOCAL",
        declarationStore = declarationStore,
        callContext = parent.callContext,
        parent = parent,
        declarations = declarations
    )
}
