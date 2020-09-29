package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
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
    val superTypes: List<Type> = emptyList(),
    val isTypeParameter: Boolean = false,
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

val ClassifierRef.defaultType: Type
    get() = SimpleType(
        this,
        typeArguments = typeParameters.map { it.defaultType }
    )

sealed class Type {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val typeArguments: List<Type>
    abstract val variance: Variance
    abstract val isGiven: Boolean
    abstract val isModule: Boolean
    abstract val isChildFactory: Boolean
    abstract val isComposable: Boolean
    abstract val superTypes: List<Type>
    abstract val expandedType: Type?
    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }
    override fun equals(other: Any?) = other is Type && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
}

class KotlinType(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT,
) : Type() {
    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef()
    }
    override val isGiven: Boolean by unsafeLazy {
        (kotlinType.constructor.declarationDescriptor!! as? ClassDescriptor)
            ?.getGivenConstructor() != null
    }
    override val isModule: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.Module)
    }
    override val isChildFactory: Boolean by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.hasAnnotation(InjektFqNames.ChildFactory)
    }
    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val superTypes: List<Type> by unsafeLazy {
        kotlinType.constructor.supertypes.map { it.toTypeRef() }
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<Type> by unsafeLazy {
        finalType.arguments.map { it.type.toTypeRef(it.projectionKind) }
    }
    override val expandedType: Type? by unsafeLazy {
        (kotlinType.constructor?.declarationDescriptor as? TypeAliasDescriptor)
            ?.expandedType?.toTypeRef()
    }
}

fun KotlinType.toTypeRef(variance: Variance = Variance.INVARIANT) =
    KotlinType(this, variance)

class SimpleType(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val typeArguments: List<Type> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isGiven: Boolean = false,
    override val isModule: Boolean = false,
    override val isChildFactory: Boolean = false,
    override val isComposable: Boolean = false,
    override val superTypes: List<Type> = emptyList(),
    override val expandedType: Type? = null,
) : Type() {
    init {
        check(typeArguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${typeArguments.map { it.render() }}"
        }
    }
}

fun Type.typeWith(typeArguments: List<Type>): Type = copy(typeArguments = typeArguments)

fun Type.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    typeArguments: List<Type> = this.typeArguments,
    variance: Variance = this.variance,
    isComposable: Boolean = this.isComposable,
) = SimpleType(
    classifier = classifier,
    isMarkedNullable = isMarkedNullable,
    typeArguments = typeArguments,
    variance = variance,
    isComposable = isComposable
)

fun Type.substitute(map: Map<ClassifierRef, Type>): Type {
    map[classifier]?.let { return it }
    return copy(typeArguments = typeArguments.map { it.substitute(map) })
}

fun Type.render(): String {
    return buildString {
        val annotations = listOfNotNull(
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

fun Type.uniqueTypeName(includeNullability: Boolean = true): Name {
    fun Type.renderName(includeArguments: Boolean = true): String {
        return buildString {
            if (isComposable) append("composable_")
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

fun Type.getSubstitutionMap(baseType: Type): Map<ClassifierRef, Type> {
    val substitutionMap = mutableMapOf<ClassifierRef, Type>()

    fun visitType(
        thisType: Type,
        baseType: Type,
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

fun Type.isAssignable(superType: Type): Boolean {
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

fun Type.isSubTypeOf(superType: Type): Boolean {
    if (classifier.fqName == superType.classifier.fqName) return true
    if (superType.classifier.fqName.asString() == KotlinBuiltIns.FQ_NAMES.any.asString() && superType.isMarkedNullable)
        return true
    return superTypes.any { it.isSubTypeOf(superType) }
}
