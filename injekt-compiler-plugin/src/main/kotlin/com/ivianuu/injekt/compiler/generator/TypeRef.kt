package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviation

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val isTypeParameter: Boolean = false
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

fun ClassifierDescriptor.toClassifierRef(): ClassifierRef {
    return ClassifierRef(
        fqNameSafe,
        (this as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef() } ?: emptyList(),
        this is TypeParameterDescriptor
    )
}

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val isContext: Boolean
    abstract val isChildContextFactory: Boolean
    abstract val isGivenSet: Boolean
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isReader: Boolean
    abstract val isComposable: Boolean
    abstract val qualifier: String?
    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT
) : TypeRef() {
    override val classifier: ClassifierRef
    init {
        val declarationDescriptor = kotlinType.prepare()
            .getAbbreviation()?.constructor?.declarationDescriptor
            ?: kotlinType.prepare().constructor.declarationDescriptor!!
        classifier = declarationDescriptor.toClassifierRef()
    }
    override val isComposable: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.Composable)
    override val isReader: Boolean
        get() = kotlinType.hasAnnotation(InjektFqNames.Reader)
    override val qualifier: String?
        get() = kotlinType.annotations.findAnnotation(InjektFqNames.Qualifier)
            ?.allValueArguments?.values?.singleOrNull()?.value as? String
    override val isContext: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ContextMarker)
    override val isChildContextFactory: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ChildContextFactory)
    override val isGivenSet: Boolean
        get() = kotlinType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.GivenSet)
    override val isMarkedNullable: Boolean
        get() = kotlinType.isMarkedNullable
    override val typeArguments: List<TypeRef>
        get() {
            val finalType = kotlinType.prepare()
                .getAbbreviation()
                ?: kotlinType
            return finalType.arguments.map { it.type.toTypeRef(it.projectionKind) }
        }
}

fun KotlinType.toTypeRef(variance: Variance = Variance.INVARIANT) = KotlinTypeRef(this, variance)

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val isContext: Boolean = false,
    override val isChildContextFactory: Boolean = false,
    override val isGivenSet: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isComposable: Boolean = false,
    override val isReader: Boolean = false,
    override val qualifier: String? = null
) : TypeRef()

fun TypeRef.makeNullable() = copy(isMarkedNullable = true)
fun TypeRef.makeNotNull() = copy(isMarkedNullable = false)

fun TypeRef.typeWith(typeArguments: List<TypeRef>): TypeRef {
    check(typeArguments.size == classifier.typeParameters.size) {
        "Argument size mismatch ${classifier.fqName} " +
                "params: ${classifier.typeParameters.map { it.fqName }} " +
                "args: ${typeArguments.map { it.render() }}"
    }
    return copy(typeArguments = typeArguments)
}

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    isContext: Boolean = this.isContext,
    isChildContextFactory: Boolean = this.isChildContextFactory,
    isGivenSet: Boolean = this.isGivenSet,
    typeArguments: List<TypeRef> = this.typeArguments,
    variance: Variance = this.variance,
    isComposable: Boolean = this.isComposable,
    isReader: Boolean = this.isReader,
    qualifier: String? = this.qualifier
) = SimpleTypeRef(
    classifier = classifier,
    isMarkedNullable = isMarkedNullable,
    isContext = isContext,
    isChildContextFactory = isChildContextFactory,
    isGivenSet = isGivenSet,
    typeArguments = typeArguments,
    variance = variance,
    isComposable = isComposable,
    isReader = isReader,
    qualifier = qualifier
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    map[classifier]?.let { return it }
    return copy(typeArguments = typeArguments.map { it.substitute(map) })
}

fun TypeRef.render(): String {
    return buildString {
        val annotations = listOfNotNull(
            if (isReader) "@com.ivianuu.injekt.Reader" else null,
            if (isComposable) "@androidx.compose.runtime.Composable" else null,
            qualifier?.let { "@com.ivianuu.injekt.internal.Qualifier(\"$it\")" }
        )
        if (annotations.isNotEmpty()) {
            annotations.forEach { annotation ->
                append(annotation)
                append(" ")
            }
        }
        if (classifier.isTypeParameter) append(classifier.fqName.shortName())
        else append(classifier.fqName)
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

fun TypeRef.uniqueTypeName(includeNullability: Boolean = true): Name {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            if (isComposable) append("composable_")
            if (isReader) append("reader_")
            if (qualifier != null) append("${qualifier}_")
            if (includeNullability && isMarkedNullable) append("nullable_")
            append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
            if (includeArguments) {
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (index == 0) append("_")
                    append(typeArgument.renderName())
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
