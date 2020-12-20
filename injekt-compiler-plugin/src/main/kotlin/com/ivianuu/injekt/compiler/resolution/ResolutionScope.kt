package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.isExternalDeclaration
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
    declarations: List<CallableRef>,
) {
    private val givens = mutableListOf<Pair<CallableRef, ResolutionScope>>()
    private val givenSetElements = mutableListOf<Pair<CallableRef, ResolutionScope>>()

    init {
        parent?.givens?.forEach { givens += it }
        parent?.givenSetElements?.forEach { givenSetElements += it }
        declarations.forEach { addDeclaration(listOf(it.callable.fqNameSafe), it) }
    }

    fun givensForType(type: TypeRef): List<GivenNode> = givens
        .filter { it.first.type.isAssignableTo(type) }
        .map { it.first.toGivenNode(type, it.second.depth()) }

    fun givenSetElementsForType(type: TypeRef): List<CallableRef> = givenSetElements
        .filter { it.first.type.isAssignableTo(type) }
        .sortedBy { it.second.depth() }
        .map { it.first }

    fun addIfNeeded(callable: CallableDescriptor) {
        callable.givenKind()
            ?.let { CallableRef(callable, givenKind = it) }
            ?.let { addDeclaration(emptyList(), it) }
    }

    fun addIfNeeded(clazz: ClassDescriptor) {
        clazz.getGivenDeclarationConstructors()
            .forEach { addDeclaration(emptyList(), it) }
    }

    private fun addDeclaration(path: List<Any>, declaration: CallableRef) {
        when (declaration.givenKind) {
            GivenKind.VALUE -> givens += declaration to this
            GivenKind.SET_ELEMENT -> givenSetElements += declaration to this
            GivenKind.GROUP -> {
                val isFunction =
                    declaration.type.allTypes.any {
                        it.classifier.fqName.asString().startsWith("kotlin.Function")
                                || it.classifier.fqName.asString()
                            .startsWith("kotlin.coroutines.SuspendFunction")
                    }
                if (isFunction) {
                    val nextPath = path + declaration.callable.fqNameSafe
                    if (isFunction) {
                        val nextCallable =
                            declaration.copy(type = declaration.type.copy(path = nextPath))
                        givens += nextCallable to this
                        declaration.callable.returnType!!.memberScope
                            .collectGivenDeclarations(nextCallable.type)
                            .forEach { addDeclaration(path + it.callable.fqNameSafe, it) }
                    } else {
                        givens += declaration to this
                        declaration.callable.returnType!!.memberScope
                            .collectGivenDeclarations(declaration.type)
                            .forEach { addDeclaration(path + it.callable.fqNameSafe, it) }
                    }
                } else {
                    givens += declaration to this
                    declaration.callable.returnType!!.memberScope
                        .collectGivenDeclarations(declaration.type)
                        .forEach { addDeclaration(path + it.callable.fqNameSafe, it) }
                }
            }
        }
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
        .collectGivenDeclarations(descriptor.defaultType.toTypeRef()) +
            CallableRef(descriptor.thisAsReceiverParameter, givenKind = GivenKind.VALUE)
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
    declarations = descriptor.collectGivenDeclarations()
)

fun BlockResolutionScope(
    declarationStore: DeclarationStore,
    parent: ResolutionScope,
): ResolutionScope = ResolutionScope(
    name = "BLOCK",
    declarationStore = declarationStore,
    callContext = parent.callContext,
    parent = parent,
    declarations = emptyList()
)
