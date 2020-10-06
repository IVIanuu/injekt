package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Nullability
import com.google.devtools.ksp.symbol.Variance
import org.jetbrains.kotlin.builtins.KotlinBuiltIns

data class ClassifierRef(
    val fqName: String,
    val typeParameters: List<ClassifierRef> = emptyList(),
    val superTypes: List<TypeRef> = emptyList(),
    val isTypeParameter: Boolean = false,
    val isObject: Boolean = false
) {
    override fun equals(other: Any?): Boolean = (other is ClassifierRef) && fqName == other.fqName
    override fun hashCode(): Int = fqName.hashCode()
}

fun KSDeclaration.toClassifierRef(injektTypes: InjektTypes): ClassifierRef {
    return ClassifierRef(
        qualifiedName!!.asString(),
        (this as? KSClassDeclaration)?.typeParameters
            ?.map { it.toClassifierRef(injektTypes) } ?: emptyList(),
        (this as? KSTypeParameter)?.bounds?.map { it.resolve().toTypeRef(injektTypes) } ?: emptyList(),
        this is KSTypeParameter,
        this is KSClassDeclaration && classKind == ClassKind.OBJECT
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
    abstract val typeArguments: List<TypeRef>
    abstract val variance: Variance
    abstract val isAssisted: Boolean
    abstract val isFunction: Boolean
    abstract val isSuspendFunction: Boolean
    abstract val isModule: Boolean
    abstract val isBinding: Boolean
    abstract val isMergeComponent: Boolean
    abstract val isMergeChildComponent: Boolean
    abstract val isChildComponent: Boolean
    abstract val isComposable: Boolean
    abstract val isFunctionAlias: Boolean
    abstract val superTypes: List<TypeRef>
    abstract val expandedType: TypeRef?
    private val typeName by unsafeLazy { uniqueTypeName(includeNullability = false) }
    override fun equals(other: Any?) = other is TypeRef && typeName == other.typeName
    override fun hashCode() = typeName.hashCode()
    override fun toString(): String = render()
}

class KsTypeRef(
    val ksType: KSType,
    val injektTypes: InjektTypes,
    override val variance: Variance = Variance.INVARIANT,
) : TypeRef() {
    override val classifier: ClassifierRef by unsafeLazy {
        ksType.declaration.toClassifierRef(injektTypes)
    }
    override val isAssisted: Boolean by unsafeLazy {
        ksType.hasAnnotation(injektTypes.assisted)
    }
    override val isFunction: Boolean by unsafeLazy {
        ksType.declaration.qualifiedName!!.asString().startsWith("kotlin.Function")
    }
    override val isSuspendFunction: Boolean by unsafeLazy {
        ksType.declaration.qualifiedName!!.asString().startsWith("kotlin.SuspendFunction")
    }
    override val isModule: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.module)
    }
    override val isBinding: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.binding)
    }
    override val isMergeComponent: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.mergeComponent)
    }
    override val isMergeChildComponent: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.mergeChildComponent)
    }
    override val isChildComponent: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.childComponent)
    }
    override val isFunctionAlias: Boolean by unsafeLazy {
        ksType.declaration.hasAnnotation(injektTypes.functionAlias)
    }
    override val isComposable: Boolean by unsafeLazy {
        ksType.annotations.any { it.annotationType.resolve() == injektTypes.composable }
    }
    override val superTypes: List<TypeRef> by unsafeLazy {
        (ksType.declaration as? KSClassDeclaration)?.superTypes
            ?.map { it.resolve().toTypeRef(injektTypes) } ?: emptyList()
    }
    override val isMarkedNullable: Boolean by unsafeLazy {
        ksType.nullability == Nullability.NULLABLE
    }
    override val typeArguments: List<TypeRef> by unsafeLazy {
        ksType.arguments.map { it.type!!.resolve().toTypeRef(injektTypes, it.variance) }
    }
    override val expandedType: TypeRef? by unsafeLazy {
        (ksType.declaration as? KSTypeAlias)?.type?.resolve()?.toTypeRef(injektTypes)
    }
}

fun KSType.toTypeRef(injektTypes: InjektTypes, variance: Variance = Variance.INVARIANT) =
    KsTypeRef(this, injektTypes, variance)

class SimpleTypeRef(
    override val classifier: ClassifierRef,
    override val isMarkedNullable: Boolean = false,
    override val typeArguments: List<TypeRef> = emptyList(),
    override val variance: Variance = Variance.INVARIANT,
    override val isAssisted: Boolean = false,
    override val isFunction: Boolean = false,
    override val isSuspendFunction: Boolean = false,
    override val isModule: Boolean = false,
    override val isBinding: Boolean = false,
    override val isMergeComponent: Boolean = false,
    override val isMergeChildComponent: Boolean = false,
    override val isChildComponent: Boolean = false,
    override val isFunctionAlias: Boolean = false,
    override val isComposable: Boolean = false,
    override val superTypes: List<TypeRef> = emptyList(),
    override val expandedType: TypeRef? = null,
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
    isAssisted: Boolean = this.isAssisted,
    isFunction: Boolean = this.isFunction,
    isSuspendFunction: Boolean = this.isSuspendFunction,
    isModule: Boolean = this.isModule,
    isBinding: Boolean = this.isBinding,
    isMergeComponent: Boolean = this.isMergeComponent,
    isMergeChildComponent: Boolean = this.isMergeChildComponent,
    isChildComponent: Boolean = this.isChildComponent,
    isComposable: Boolean = this.isComposable,
    isFunctionAlias: Boolean = this.isFunctionAlias,
    superTypes: List<TypeRef> = this.superTypes,
    expandedType: TypeRef? = this.expandedType
) = SimpleTypeRef(
    classifier,
    isMarkedNullable,
    typeArguments,
    variance,
    isAssisted,
    isFunction,
    isSuspendFunction,
    isModule,
    isBinding,
    isMergeComponent,
    isMergeChildComponent,
    isChildComponent,
    isFunctionAlias,
    isComposable,
    superTypes,
    expandedType
)

fun TypeRef.substitute(map: Map<ClassifierRef, TypeRef>): TypeRef {
    map[classifier]?.let { return it }
    return copy(
        typeArguments = typeArguments.map { it.substitute(map) },
        expandedType = expandedType?.substitute(map)
    )
}

fun TypeRef.render(): String {
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
        if (classifier.isTypeParameter) append(classifier.fqName.substringAfterLast("."))
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

fun TypeRef.renderExpanded() = expandedType?.render() ?: render()

fun TypeRef.uniqueTypeName(includeNullability: Boolean = true): String {
    fun TypeRef.renderName(includeArguments: Boolean = true): String {
        return buildString {
            if (isComposable) append("composable_")
            // if (includeNullability && isMarkedNullable) append("nullable_")
            append(classifier.fqName.split(".").joinToString("_"))
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
}

fun TypeRef.getSubstitutionMap(baseType: TypeRef): Map<ClassifierRef, TypeRef> {
    val substitutionMap = mutableMapOf<ClassifierRef, TypeRef>()

    fun visitType(
        thisType: TypeRef,
        baseType: TypeRef,
    ) {
        if (baseType.classifier.isTypeParameter) {
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
    if (superType.classifier.fqName == KotlinBuiltIns.FQ_NAMES.any.asString() && superType.isMarkedNullable)
        return true
    return superTypes.any { it.isSubTypeOf(superType) }
}
