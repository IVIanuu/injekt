package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getGivenDeclarationParameters
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.calls.DslMarkerUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class CallableRef(
    val callable: CallableDescriptor,
    val type: TypeRef = callable.returnType!!.toTypeRef(),
    val originalType: TypeRef = callable.returnType!!.toTypeRef(),
    val parameterTypes: Map<ParameterDescriptor, TypeRef> = callable.allParameters
        .map { it to it.type.toTypeRef() }
        .toMap(),
    val givenKind: GivenKind? = callable.givenKind(),
    val callContext: CallContext = callable.callContext,
)

fun CallableRef.substitute(substitutionMap: Map<ClassifierRef, TypeRef>): CallableRef {
    return copy(
        type = type.substitute(substitutionMap),
        parameterTypes = parameterTypes.mapValues { it.value.substitute(substitutionMap) }
    )
}

enum class GivenKind {
    VALUE, SET_ELEMENT, GROUP
}

fun CallableDescriptor.toCallableRef() = CallableRef(this)

fun MemberScope.collectGivenDeclarations(type: TypeRef): List<CallableRef> {
    // special case to support @Given () -> Foo etc
    if ((type.classifier.fqName.asString().startsWith("kotlin.Function")
                || type.classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction"))
    ) {
        val givenKind = type.givenKind
        if (givenKind != null) {
            return listOf(
                getContributedFunctions("invoke".asNameId(), NoLookupLocation.FROM_BACKEND)
                    .first()
                    .toCallableRef()
                    .let { callable ->
                        callable.copy(
                            givenKind = givenKind,
                            parameterTypes = callable.parameterTypes.toMutableMap()
                                .also { it[callable.callable.dispatchReceiverParameter!!] = type }
                        )
                    }
            )
        }
    }

    val primaryConstructorKinds = (type.classifier.descriptor
        ?.safeAs<ClassDescriptor>()
        ?.unsubstitutedPrimaryConstructor
        ?.valueParameters
        ?.mapNotNull {
            val kind = it.givenKind()
            if (kind != null) it.name to kind else null
        }
        ?.toMap()
        ?: emptyMap())
    return getContributedDescriptors()
        .flatMap { callable ->
            when (callable) {
                is ClassDescriptor -> callable.getGivenDeclarationConstructors()
                is PropertyDescriptor -> (callable.givenKind()
                    ?: primaryConstructorKinds[callable.name])
                    ?.let { kind -> listOf(CallableRef(callable, givenKind = kind)) } ?: emptyList()
                is FunctionDescriptor -> callable.givenKind()?.let { kind ->
                    listOf(CallableRef(callable, givenKind = kind))
                } ?: emptyList()
                else -> emptyList()
            }
        }
}

fun Annotated.givenKind(): GivenKind? = when {
    hasAnnotation(InjektFqNames.Given) -> GivenKind.VALUE
    hasAnnotation(InjektFqNames.GivenSetElement) -> GivenKind.SET_ELEMENT
    hasAnnotation(InjektFqNames.GivenGroup) -> GivenKind.GROUP
    this is ClassConstructorDescriptor -> constructedClass.givenKind()
    else -> null
}

fun CallableDescriptor.collectGivenDeclarations(): List<CallableRef> {
    val declarations = mutableListOf<CallableRef>()

    declarations += allParameters
        .mapNotNull {
            val kind = it.givenKind()
            if (kind != null) CallableRef(it, givenKind = kind) else null
        }

    extensionReceiverParameter?.let { receiver ->
        declarations += CallableRef(receiver, givenKind = GivenKind.VALUE)
        declarations += receiver.type.memberScope.collectGivenDeclarations(
            extensionReceiverParameter!!.type.toTypeRef()
        )
    }

    return declarations
}

fun ParameterDescriptor.givenKind(): GivenKind? {
    val userData = getUserData(DslMarkerUtils.FunctionTypeAnnotationsKey)
    val givenDeclarationParameters = getGivenDeclarationParameters()

    return (this as Annotated).givenKind() ?: type.givenKind() ?: userData?.let {
        when {
            userData.hasAnnotation(InjektFqNames.Given) -> GivenKind.VALUE
            userData.hasAnnotation(InjektFqNames.GivenSetElement) -> GivenKind.SET_ELEMENT
            userData.hasAnnotation(InjektFqNames.GivenGroup) -> GivenKind.GROUP
            else -> null
        }
    } ?: givenDeclarationParameters
        .firstOrNull { it.callable == this }
        ?.givenKind
}

fun ClassDescriptor.getGivenDeclarationConstructors(): List<CallableRef> = constructors
    .mapNotNull { constructor ->
        if (constructor.isPrimary) {
            (constructor.givenKind() ?: givenKind())?.let { kind ->
                CallableRef(constructor, givenKind = kind)
            }
        } else {
            constructor.givenKind()?.let { kind ->
                CallableRef(constructor, givenKind = kind)
            }
        }
    }
    .flatMap { declaration ->
        if (declaration.givenKind == GivenKind.VALUE) {
            allGivenTypes().map { type ->
                declaration.copy(type = type)
            }
        } else {
            listOf(declaration)
        }
    }


fun ClassDescriptor.allGivenTypes(): List<TypeRef> = buildList<TypeRef> {
    this += defaultType.toTypeRef()
    this += defaultType.constructor.supertypes
        .filter { it.hasAnnotation(InjektFqNames.Given) }
        .map { it.toTypeRef() }
}

fun CallableRef.collectGivens(
    path: List<Any>,
    addGiven: (CallableRef) -> Unit,
    addGivenSetElement: (CallableRef) -> Unit
) {
    when (givenKind) {
        GivenKind.VALUE -> addGiven(this)
        GivenKind.SET_ELEMENT -> addGivenSetElement(this)
        GivenKind.GROUP -> {
            val isFunction = type.allTypes.any {
                it.classifier.fqName.asString().startsWith("kotlin.Function")
                        || it.classifier.fqName.asString()
                    .startsWith("kotlin.coroutines.SuspendFunction")
            }
            if (isFunction) {
                val nextPath = path + callable.fqNameSafe
                if (isFunction) {
                    val nextCallable = copy(type = type.copy(path = nextPath))
                    addGiven(nextCallable)
                    callable.returnType!!.memberScope
                        .collectGivenDeclarations(nextCallable.type)
                        .forEach {
                            it.collectGivens(path + it.callable.fqNameSafe, addGiven, addGivenSetElement)
                        }
                } else {
                    addGiven(this)
                    callable.returnType!!.memberScope
                        .collectGivenDeclarations(type)
                        .forEach {
                            it.collectGivens(
                                path + it.callable.fqNameSafe,
                                addGiven,
                                addGivenSetElement
                            )
                        }
                }
            } else {
                addGiven(this)
                callable.returnType!!.memberScope
                    .collectGivenDeclarations(type)
                    .forEach {
                        it.collectGivens(
                            path + it.callable.fqNameSafe,
                            addGiven,
                            addGivenSetElement
                        )
                    }
            }
        }
    }
}
