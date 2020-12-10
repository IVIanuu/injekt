package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.prepare
import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.getAbbreviatedType
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

data class ClassifierRef(
    val fqName: FqName,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val expandedType: TypeRef? = null,
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false,
    val isTypeAlias: Boolean = false,
    val descriptor: ClassifierDescriptor? = null
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

val TypeRef.expandedType: TypeRef?
    get() {
        return classifier.expandedType?.let {
            val substitutionMap = classifier.typeParameters
                .zip(typeArguments)
                .toMap()
            it.substitute(substitutionMap)
                .copy(isMarkedNullable = isMarkedNullable)
        }
    }

val ClassifierRef.defaultType: TypeRef
    get() = SimpleTypeRef(
        this,
        typeArguments = typeParameters.map { it.defaultType }
    )

fun TypeRef.superTypes(substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap()): List<TypeRef> {
    val merged = classifier.typeParameters
        .zip(typeArguments)
        .toMap() + substitutionMap
    return classifier.superTypes
        .map { it.substitute(merged) }
}

fun IrType.toTypeRef(variance: Variance = Variance.INVARIANT, isStarProjection: Boolean = false) =
    toKotlinType().toTypeRef(variance, isStarProjection)

fun KotlinType.toTypeRef(
    variance: Variance = Variance.INVARIANT,
    isStarProjection: Boolean = false,
) = KotlinTypeRef(this, variance, isStarProjection)

fun ClassifierDescriptor.toClassifierRef(): ClassifierRef = ClassifierRef(
    fqName = original.fqNameSafe,
    typeParameters = (original as? ClassifierDescriptorWithTypeParameters)?.declaredTypeParameters
        ?.map { it.toClassifierRef() } ?: emptyList(),
    superTypes = typeConstructor.supertypes.map { it.toTypeRef() },
    expandedType = (original as? TypeAliasDescriptor)?.expandedType?.toTypeRef()?.fullyExpandedType,
    isTypeParameter = this is TypeParameterDescriptor,
    isObject = this is ClassDescriptor && kind == ClassKind.OBJECT,
    isTypeAlias = this is TypeAliasDescriptor,
    descriptor = this
)

sealed class TypeRef {
    abstract val classifier: ClassifierRef
    abstract val isMarkedNullable: Boolean
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isComposable: Boolean
    abstract val isStarProjection: Boolean
    abstract val qualifiers: List<AnnotationDescriptor>

    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }

    override fun toString(): String = typeName.asString()

    override fun equals(other: Any?) =
        other is TypeRef && other._hashCode == _hashCode

    private val _hashCode by unsafeLazy {
        var result = classifier.hashCode()
        // todo result = 31 * result + isMarkedNullable.hashCode()
        result = 31 * result + typeArguments.hashCode()
        // todo result result = 31 * result + variance.hashCode()
        result = 31 * result + isComposable.hashCode()
        result = 31 * result + isStarProjection.hashCode()
        result = 31 * result + qualifiers.hashCode()
        result
    }

    override fun hashCode(): Int = _hashCode

    val allTypes: Set<TypeRef> by unsafeLazy {
        buildSet<TypeRef> {
            this += this@TypeRef
            expandedType?.let { this += it.allTypes }
            this += superTypes().flatMap { it.allTypes }
        }
    }
}

class KotlinTypeRef(
    val kotlinType: KotlinType,
    override val variance: Variance = Variance.INVARIANT,
    override val isStarProjection: Boolean = false,
) : TypeRef() {
    init {
        check(!kotlinType.isError) {
            "Error type $kotlinType"
        }
    }

    private val finalType by unsafeLazy { kotlinType.getAbbreviation() ?: kotlinType.prepare() }
    override val classifier: ClassifierRef by unsafeLazy {
        finalType.constructor.declarationDescriptor!!.toClassifierRef()
    }
    override val isComposable: Boolean by unsafeLazy {
        kotlinType.hasAnnotation(InjektFqNames.Composable) &&
                kotlinType.getAbbreviatedType()?.expandedType?.hasAnnotation(InjektFqNames.Composable) != true
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        kotlinType.isMarkedNullable
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        finalType.arguments.map {
            it.type.toTypeRef(it.projectionKind, it.isStarProjection)
        }
    }
    override val qualifiers: List<AnnotationDescriptor> by unsafeLazy {
        kotlinType.getAnnotatedAnnotations(InjektFqNames.Qualifier)
    }
}

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isComposable: Boolean = false,
    override val isStarProjection: Boolean = false,
    override val qualifiers: List<AnnotationDescriptor> = emptyList(),
) : TypeRef() {
    init {
        check(typeArguments.size == classifier.typeParameters.size) {
            "Argument size mismatch ${classifier.fqName} " +
                    "params: ${classifier.typeParameters.map { it.fqName }} " +
                    "args: ${typeArguments.map { it.render() }}"
        }
    }
}

fun TypeRef.typeWith(typeArguments: List<TypeRef>): TypeRef = copy(typeArguments = typeArguments)

fun TypeRef.copy(
    classifier: ClassifierRef = this.classifier,
    isMarkedNullable: Boolean = this.isMarkedNullable,
    typeArguments: List<TypeRef> = this.typeArguments,
    variance: Variance = this.variance,
    isComposable: Boolean = this.isComposable,
    isStarProjection: Boolean = this.isStarProjection,
    qualifiers: List<AnnotationDescriptor> = this.qualifiers
) = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    typeArguments,
    variance,
    isComposable,
    isStarProjection,
    qualifiers
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    map[classifier]?.let {
        return it.copy(
            // we copy nullability to support T : Any? -> String
            isMarkedNullable = if (!isStarProjection) isMarkedNullable else it.isMarkedNullable,
            // we copy qualifiers to support @MyQualifier T -> @MyQualifier String
            qualifiers = qualifiers + it.qualifiers
        )
    }

    if (typeArguments.isEmpty() && qualifiers.isEmpty() &&
        !classifier.isTypeParameter
    ) return this

    val substituted = copy(
        typeArguments = typeArguments.map { it.substitute(map) }
    )

    if (classifier.isTypeParameter && substituted == this) {
        classifier
            .superTypes
            .firstNotNullResult {
                val substitutedSuperType = it.substitute(map)
                if (substitutedSuperType != it) substitutedSuperType
                else null
            }
            ?.let { return it }
    }

    return substituted
}

val STAR_PROJECTION_TYPE = SimpleTypeRef(
    classifier = ClassifierRef(StandardNames.FqNames.any.toSafe()),
    isStarProjection = true
)

fun TypeRef.replaceTypeParametersWithStars(): TypeRef {
    if (classifier.isTypeParameter) return STAR_PROJECTION_TYPE
    if (typeArguments.isEmpty() && expandedType == null) return this
    return copy(typeArguments = typeArguments.map { it.replaceTypeParametersWithStars() })
}

fun TypeRef.substituteStars(baseType: TypeRef): TypeRef {
    if (this == baseType) return this
    if (isStarProjection && !baseType.classifier.isTypeParameter) return baseType
    if (classifier != baseType.classifier) return this
    if (typeArguments.isEmpty()) return this
    return copy(
        typeArguments = typeArguments
            .zip(baseType.typeArguments)
            .map { (thisTypeArgument, baseTypeArgument) ->
                thisTypeArgument.substituteStars(baseTypeArgument)
            }
    )
}

fun TypeRef.render(expanded: Boolean = false): String {
    return buildString {
        fun TypeRef.inner() {
            val annotations = (if (!expanded) qualifiers.map {
                "@${it.type}(${it.allValueArguments.toList().joinToString { "${it.first}=${it.second}" }})"
            } else emptyList()) + listOfNotNull(
                if (isComposable) "@${InjektFqNames.Composable}" else null,
            )
            if (annotations.isNotEmpty()) {
                annotations.forEach { annotation ->
                    append(annotation)
                    append(" ")
                }
            }
            when {
                classifier.isTypeParameter -> append(classifier.fqName.shortName())
                isStarProjection -> append("*")
                else -> append(classifier.fqName)
            }
            if (typeArguments.isNotEmpty()) {
                append("<")
                typeArguments.forEachIndexed { index, typeArgument ->
                    if (typeArgument.variance != Variance.INVARIANT &&
                        !typeArgument.isStarProjection
                    )
                        append("${typeArgument.variance.label} ")
                    append(typeArgument.render(expanded = expanded))
                    if (index != typeArguments.lastIndex) append(", ")
                }
                append(">")
            }
            if (isMarkedNullable && !isStarProjection) append("?")
        }
        if (expanded) fullyExpandedType.inner() else inner()
    }
}
fun TypeRef.uniqueTypeName(includeNullability: Boolean = true): Name {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            qualifiers.forEach {
                append(it.type.constructor.declarationDescriptor!!.fqNameSafe)
                append(it.allValueArguments.hashCode())
                append("_")
            }
            if (isComposable) append("composable_")
            // if (includeNullability && isMarkedNullable) append("nullable_")
            if (isStarProjection) append("star")
            else append(classifier.fqName.pathSegments().joinToString("_") { it.asString() })
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
    return (
            if (fullTypeName.length <= 128) fullTypeName
            else ("${renderName(includeArguments = false)}_${fullTypeName.hashCode()}")
            )
        .removeIllegalChars()
        .asNameId()
}
fun getSubstitutionMap(
    pairs: List<Pair<TypeRef, TypeRef>>,
    typeParameters: List<ClassifierRef> = emptyList(),
): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()
    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
            baseType.superTypes()
                .map { it.fullyExpandedType }
                .map { thisType.subtypeView(it.classifier, substitutionMap) to it }
                .forEach { (thisBaseTypeView, baseSuperType) ->
                    thisBaseTypeView?.typeArguments?.zip(baseSuperType.typeArguments)
                        ?.forEach { visitType(it.first, it.second) }
                }
        } else {
            thisType.typeArguments.zip(baseType.typeArguments)
                .forEach { visitType(it.first, it.second) }
        }
    }
    var lastSubstitutionMap: Map<ClassifierRef, TypeRef>? = null
    while (lastSubstitutionMap != substitutionMap) {
        pairs.forEach { visitType(it.first, it.second) }
        substitutionMap.forEach { visitType(it.value, it.key.defaultType) }
        lastSubstitutionMap = substitutionMap.toMap()
    }
    typeParameters
        .filter { it !in substitutionMap }
        .forEach { typeParameter ->
            substitutionMap[typeParameter] = typeParameter.defaultType.substitute(substitutionMap)
        }
    return substitutionMap
}
fun TypeRef.getStarSubstitutionMap(baseType: TypeRef): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()
    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.isStarProjection && !thisType.isStarProjection && !thisType.classifier.isTypeParameter) {
            substitutionMap[baseType.classifier] = thisType
        } else {
            thisType.typeArguments.zip(baseType.typeArguments).forEach {
                visitType(it.first, it.second)
            }
        }
    }
    visitType(this, baseType)
    return substitutionMap
}
fun TypeRef.isAssignableTo(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): Boolean {
    if (isStarProjection || superType.isStarProjection) return true
    if (classifier.fqName == superType.classifier.fqName) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (!qualifiers.isAssignableTo(superType.qualifiers)) return false
        if (allTypes.any { it.isComposable } != superType.allTypes.any { it.isComposable }) return false
        if (typeArguments.zip(superType.typeArguments)
                .any { (a, b) -> !a.isAssignableTo(b, substitutionMap) }
        )
            return false
        return true
    }
    if (superType.classifier.isTypeParameter) {
        val superTypesAssignable = superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
        if (!superTypesAssignable) return false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(superType.qualifiers)
        ) return false
        return true
    } else if (classifier.isTypeParameter) {
        val superTypesAssignable = superTypes(substitutionMap).all { upperBound ->
            superType.isSubTypeOf(upperBound, substitutionMap)
        }
        if (!superTypesAssignable) return false
        if (qualifiers.isNotEmpty() &&
            !superType.qualifiers.isAssignableTo(qualifiers)
        ) return false
        return true
    }
    return false
}
fun TypeRef.isSubTypeOf(
    superType: TypeRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): Boolean {
    if (classifier.fqName == superType.classifier.fqName) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (!qualifiers.isAssignableTo(superType.qualifiers)) return false
        if (allTypes.any { it.isComposable } != superType.allTypes.any { it.isComposable }) return false
        if (typeArguments.zip(superType.typeArguments)
                .any { (a, b) -> !a.isAssignableTo(b, substitutionMap) }
        )
            return false
        return true
    }
    if (superType.classifier.fqName == InjektFqNames.Any) {
        if (isMarkedNullable && !superType.isMarkedNullable) return false
        if (superType.qualifiers.isNotEmpty() &&
            !qualifiers.isAssignableTo(superType.qualifiers)
        ) return false
        return true
    }
    val subTypeView = subtypeView(superType.classifier, substitutionMap)
    if (subTypeView != null) {
        if (subTypeView == superType && (!subTypeView.isMarkedNullable || superType.isMarkedNullable) &&
            (superType.qualifiers.isEmpty() || subTypeView.qualifiers.isAssignableTo(superType.qualifiers))
        )
            return true
        return subTypeView.typeArguments.zip(superType.typeArguments)
            .all { (subTypeArg, superTypeArg) ->
                superTypeArg.superTypes(substitutionMap).all {
                    subTypeArg.isSubTypeOf(it, substitutionMap)
                }
            }
    } else if (superType.classifier.isTypeParameter ||
        superType.classifier.isTypeAlias
    ) {
        return superType.superTypes(substitutionMap).all { upperBound ->
            isSubTypeOf(upperBound, substitutionMap)
        }
    }
    return false
}
fun List<AnnotationDescriptor>.isAssignableTo(superQualifiers: List<AnnotationDescriptor>): Boolean {
    if (size != superQualifiers.size) return false
    return zip(superQualifiers).all { (thisQualifier, superQualifier) ->
        thisQualifier.isAssignableTo(superQualifier)
    }
}
fun AnnotationDescriptor.isAssignableTo(superQualifier: AnnotationDescriptor): Boolean {
    if (!type.toTypeRef().isAssignableTo(superQualifier.type.toTypeRef())) return false
    return allValueArguments == superQualifier.allValueArguments
}
val TypeRef.fullyExpandedType: TypeRef
    get() = expandedType?.fullyExpandedType ?: this
fun TypeRef.subtypeView(
    classifier: ClassifierRef,
    substitutionMap: Map<ClassifierRef, TypeRef> = emptyMap(),
): TypeRef? {
    if (this.classifier == classifier) return this
    expandedType?.subtypeView(classifier, substitutionMap)?.let { return it }
    for (superType in superTypes(substitutionMap)) {
        superType.subtypeView(classifier, substitutionMap)?.let { return it }
    }
    return null
}
