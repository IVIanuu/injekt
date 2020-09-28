package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

fun ClassifierDescriptor.toClassifierRef(): ClassifierRef {
    return ClassifierRef(
        original.fqNameSafe,
        (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
            ?.map { it.toClassifierRef() } ?: emptyList(),
        (original as? TypeParameterDescriptor)?.upperBounds?.map { it.toTypeRef() } ?: emptyList(),
        this is TypeParameterDescriptor
    )
}

val ClassifierRef.defaultType: TypeRef
    get() = SimpleTypeRef(
        this,
        typeArguments = typeParameters.map { it.defaultType }
    )

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
    abstract val superTypes: List<TypeRef>
    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT
) : TypeRef() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef()
    }

    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val superTypes: List<TypeRef> by unsafeLazy {
        kotlinType.constructor.supertypes.map { it.toTypeRef() }
    }
    override val isReader: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Reader) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Reader) != true
    }
    override val isContext: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ContextMarker)
    }
    override val isChildContextFactory: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ChildContextFactory)
    }
    override val isGivenSet: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.GivenSet)
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        finalType.arguments.map { it.type.toTypeRef(it.projectionKind) }
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
    override val superTypes: List<TypeRef> = emptyList(),
    override val isReader: Boolean = false
) : TypeRef() {
    init {
        check(typeArguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${typeArguments.map { it.render() }}"
        }
    }
}

fun TypeRef.makeNullable() = copy(isMarkedNullable = true)
fun TypeRef.makeNotNull() = copy(isMarkedNullable = false)

fun TypeRef.typeWith(typeArguments: List<TypeRef>): TypeRef = copy(typeArguments = typeArguments)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    isContext: Boolean = this.isContext,
    isChildContextFactory: Boolean = this.isChildContextFactory,
    isGivenSet: Boolean = this.isGivenSet,
    typeArguments: List<TypeRef> = this.typeArguments,
    variance: Variance = this.variance,
    isComposable: Boolean = this.isComposable,
    isReader: Boolean = this.isReader
) = SimpleTypeRef(
    classifier = classifier,
    isMarkedNullable = isMarkedNullable,
    isContext = isContext,
    isChildContextFactory = isChildContextFactory,
    isGivenSet = isGivenSet,
    typeArguments = typeArguments,
    variance = variance,
    isComposable = isComposable,
    isReader = isReader
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
            //if (includeNullability && isMarkedNullable) append("nullable_")
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

fun TypeRef.getSubstitutionMap(baseType: TypeRef): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef
    ) {
        if (baseType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
        } else {
            thisType.typeArguments.zip(baseType.classifier.typeParameters).forEach {
                visitType(it.first, it.second.defaultType)
            }
        }
    }

    visitType(this, baseType)

    return substitutionMap
}

fun TypeRef.isAssignable(superType: TypeRef): Boolean {
    if (this == superType) return true

    if (superType.classifier.isTypeParameter) {
        return superType.classifier.superTypes.all { upperBound ->
            isSubTypeOf(upperBound)
        }
    }

    if (classifier.fqName != superType.classifier.fqName) return false

    return typeArguments.zip(superType.typeArguments).all { (a, b) ->
        a.isAssignable(b)
    }
}

fun TypeRef.isSubTypeOf(superType: TypeRef): Boolean {
    if (classifier.fqName == superType.classifier.fqName) return true
    if (superType.classifier.fqName.asString() == KotlinBuiltIns.FQ_NAMES.any.asString() && superType.isMarkedNullable)
        return true
    return superTypes.any { it.isSubTypeOf(superType) }
}
