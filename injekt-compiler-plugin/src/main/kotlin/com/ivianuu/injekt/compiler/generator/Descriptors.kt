package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.getFunctionType
import com.ivianuu.injekt.compiler.irtransform.asNameId
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeserializedDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

data class ContextFactoryDescriptor(
    val factoryType: TypeRef,
    val contextType: TypeRef,
    val inputTypes: List<TypeRef>
)

data class ContextFactoryImplDescriptor(
    val factoryImplFqName: FqName,
    val factory: ContextFactoryDescriptor
)

data class CallableRef(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val uniqueKey: String,
    val type: TypeRef,
    val receiver: TypeRef?,
    val parameters: List<ParameterRef>,
    val targetContext: TypeRef?,
    val givenKind: GivenKind,
    val isExternal: Boolean,
    val isPropertyAccessor: Boolean
) {
    enum class GivenKind {
        GIVEN, GIVEN_MAP_ENTRIES, GIVEN_SET_ELEMENTS, GIVEN_SET
    }
}

fun FunctionDescriptor.toCallableRef() = CallableRef(
    name = when (this) {
        is ConstructorDescriptor -> constructedClass.name
        is PropertyAccessorDescriptor -> correspondingProperty.name
        else -> name
    },
    packageFqName = findPackage().fqName,
    fqName = when (this) {
        is ConstructorDescriptor -> constructedClass.fqNameSafe
        is PropertyAccessorDescriptor -> correspondingProperty.fqNameSafe
        else -> fqNameSafe
    },
    type = (if (extensionReceiverParameter != null || valueParameters.isNotEmpty()) getFunctionType() else returnType!!)
        .toTypeRef(),
    receiver = dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
        ?.takeIf { it is ClassDescriptor && it.kind == ClassKind.OBJECT }?.defaultType?.toTypeRef(),
    isExternal = when (this) {
        is ConstructorDescriptor -> constructedClass is DeserializedDescriptor
        is PropertyAccessorDescriptor -> correspondingProperty is DeserializedDescriptor
        else -> this is DeserializedDescriptor
    },
    targetContext = annotations.findAnnotation(InjektFqNames.Given)
        ?.allValueArguments
        ?.get("scopeContext".asNameId())
        ?.let { it as KClassValue }
        ?.getArgumentType(module)
        ?.toTypeRef(),
    givenKind = when {
        hasAnnotationWithPropertyAndClass(InjektFqNames.Given) -> CallableRef.GivenKind.GIVEN
        hasAnnotatedAnnotationsWithPropertyAndClass(InjektFqNames.Effect) -> CallableRef.GivenKind.GIVEN
        hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) -> CallableRef.GivenKind.GIVEN_MAP_ENTRIES
        hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) -> CallableRef.GivenKind.GIVEN_SET_ELEMENTS
        hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSet) -> CallableRef.GivenKind.GIVEN_SET
        else -> error("Unexpected callable $this")
    },
    parameters = listOfNotNull(
        extensionReceiverParameter?.type?.let {
            ParameterRef(
                KotlinTypeRef(it),
                true
            )
        }
    ) + valueParameters.map { ParameterRef(it.type.toTypeRef()) },
    isPropertyAccessor = this is PropertyAccessorDescriptor,
    uniqueKey = when (this) {
        is ConstructorDescriptor -> constructedClass.uniqueKey()
        is PropertyAccessorDescriptor -> correspondingProperty.uniqueKey()
        else -> uniqueKey()
    }
)

data class ParameterRef(
    val typeRef: TypeRef,
    val isExtensionReceiver: Boolean = false
)

data class GivenSetDescriptor(
    val type: TypeRef,
    val callables: List<CallableRef>
)
