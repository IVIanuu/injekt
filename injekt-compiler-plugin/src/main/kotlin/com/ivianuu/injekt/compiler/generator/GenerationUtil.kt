package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.checkers.isMarkedAsReader
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.utils.refineType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.upperIfFlexible
import kotlin.math.absoluteValue

@Reader
val isInjektCompiler: Boolean
    get() = moduleDescriptor.name.asString() == "<injekt-compiler-plugin>"

@Reader
fun <D : DeclarationDescriptor> KtDeclaration.descriptor() =
    given<BindingContext>()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as D

@Reader
val moduleDescriptor: ModuleDescriptor
    get() = given()

fun DeclarationDescriptor.uniqueKey() = when (this) {
    is ClassDescriptor -> "${fqNameSafe}${
        if (visibility == Visibilities.LOCAL &&
            name.isSpecial
        ) findPsi()?.startOffset else ""
    }__class"
    is FunctionDescriptor -> uniqueFunctionKeyOf(
        fqNameSafe,
        visibility,
        findPsi()?.startOffset,
        allParameters.map {
            it.type.prepare()
                .constructor.declarationDescriptor?.fqNameSafe
                ?: error("Wtf broken ${it.type} ${it.type.javaClass} ${it.type.prepare()} ${it.type.prepare().javaClass}")
        })
    is PropertyDescriptor -> "${fqNameSafe}${
        if (visibility == Visibilities.LOCAL &&
            name.isSpecial
        ) findPsi()?.startOffset else ""
    }__property${
        listOfNotNull(
            dispatchReceiverParameter?.type?.prepare(),
            extensionReceiverParameter?.type?.prepare()
        ).map { it.constructor.declarationDescriptor!!.fqNameSafe }.hashCode().absoluteValue
    }"
    else -> error("Unsupported declaration $this")
}

fun KotlinType.prepare(): KotlinType {
    var tmp = refineType()
    if (constructor is IntersectionTypeConstructor) {
        tmp = CommonSupertypes.commonSupertype(constructor.supertypes)
    }
    tmp = tmp.upperIfFlexible()
    return tmp
}

fun uniqueFunctionKeyOf(
    fqName: FqName,
    visibility: Visibility,
    startOffset: Int? = null,
    parameterTypes: List<FqName>
) = "$fqName${
    if (visibility == Visibilities.LOCAL && fqName.shortName().isSpecial) startOffset ?: "" else ""
}__function${parameterTypes.hashCode().absoluteValue}"

fun KotlinType.render() = KotlinTypeRef(this).render()

fun TypeRef.render(): String {
    return buildString {
        val annotations = listOfNotNull(
            if (isReader) "@Reader" else null,
            if (isComposable) "@Composable" else null,
            qualifier?.let { "@Qualifier($it)" }
        )
        if (annotations.isNotEmpty()) {
            append("[")
            annotations.forEachIndexed { index, annotation ->
                append(annotation)
                if (index != annotations.lastIndex) append(", ")
            }
            append("] ")
        }
        append(fqName)
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.forEachIndexed { index, typeArgument ->
                if (typeArgument.variance != Variance.INVARIANT)
                    append("${typeArgument.variance.label} ")
                append(typeArgument.render())
                if (index != typeArguments.lastIndex) append(", ")
            }
            append(">")
        }
        if (isMarkedNullable) append("?")
    }
}

fun TypeRef.uniqueTypeName(): Name {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            if (isComposable) append("composable_")
            if (isReader) append("reader_")
            if (qualifier != null) append("${qualifier}_")
            if (isMarkedNullable) append("nullable_")
            append(fqName.pathSegments().joinToString("_") { it.asString() })
            if (includeArguments) {
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (index == 0) append("_")
                    else append(typeArgument.renderName())
                    if (index != typeArguments.lastIndex) append("_")
                }
            }
        }
    }

    val fullTypeName = renderName()

    // Conservatively shorten the name if the length exceeds 128
    return (if (fullTypeName.length <= 128) fullTypeName
    else ("${renderName(includeArguments = false)}_${fullTypeName.hashCode()}"))
        .removeIllegalChars()
        .asNameId()
}

fun DeclarationDescriptor.hasAnnotationWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotation(fqName) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotation(fqName)) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotation(fqName))

fun DeclarationDescriptor.hasAnnotatedAnnotationsWithPropertyAndClass(
    fqName: FqName
): Boolean = hasAnnotatedAnnotations(fqName, module) ||
        (this is PropertyAccessorDescriptor && correspondingProperty.hasAnnotatedAnnotations(
            fqName,
            module
        )) ||
        (this is ConstructorDescriptor && constructedClass.hasAnnotatedAnnotations(fqName, module))

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
    type = KotlinTypeRef(returnType!!),
    receiver = dispatchReceiverParameter?.type?.constructor?.declarationDescriptor
        ?.takeIf { it is ClassDescriptor && it.kind == ClassKind.OBJECT }
        ?.let { KotlinTypeRef(it.defaultType) },
    isExternal = findPsi() != null, // todo might be wrong
    targetContext = annotations.findAnnotation(InjektFqNames.Given)
        ?.allValueArguments
        ?.get("scopeContext".asNameId())
        ?.getType(module)
        ?.let { KotlinTypeRef(it) },
    givenKind = when {
        hasAnnotationWithPropertyAndClass(InjektFqNames.Given) -> CallableRef.GivenKind.GIVEN
        hasAnnotationWithPropertyAndClass(InjektFqNames.GivenMapEntries) -> CallableRef.GivenKind.MAP_ENTRIES
        hasAnnotationWithPropertyAndClass(InjektFqNames.GivenSetElements) -> CallableRef.GivenKind.SET_ELEMENTS
        else -> error("Unexpected callable $this")
    },
    parameters = listOfNotNull(
        extensionReceiverParameter?.type?.let {
            ParameterRef(
                KotlinTypeRef(it),
                true
            )
        }
    ) + valueParameters.map {
        ParameterRef(KotlinTypeRef(it.type))
    },
    isPropertyAccessor = this is PropertyAccessorDescriptor,
    uniqueKey = when (this) {
        is ConstructorDescriptor -> constructedClass.uniqueKey()
        is PropertyAccessorDescriptor -> correspondingProperty.uniqueKey()
        else -> uniqueKey()
    }
)

fun ClassDescriptor.getReaderConstructor(): ConstructorDescriptor? {
    constructors
        .firstOrNull {
            it.isMarkedAsReader()
        }?.let { return it }
    if (!isMarkedAsReader()) return null
    return unsubstitutedPrimaryConstructor
}
