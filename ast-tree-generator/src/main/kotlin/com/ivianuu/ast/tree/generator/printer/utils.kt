package com.ivianuu.ast.tree.generator.printer

import com.ivianuu.ast.tree.generator.astImplementationDetailType
import com.ivianuu.ast.tree.generator.compositeTransformResultType
import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.model.AbstractElement
import com.ivianuu.ast.tree.generator.model.AstField
import com.ivianuu.ast.tree.generator.model.Builder
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.FieldList
import com.ivianuu.ast.tree.generator.model.FieldWithDefault
import com.ivianuu.ast.tree.generator.model.Implementation
import com.ivianuu.ast.tree.generator.model.ImplementationWithArg
import com.ivianuu.ast.tree.generator.model.Importable
import com.ivianuu.ast.tree.generator.model.IntermediateBuilder
import com.ivianuu.ast.tree.generator.model.KindOwner
import com.ivianuu.ast.tree.generator.model.LeafBuilder
import com.ivianuu.ast.tree.generator.model.Type
import com.ivianuu.ast.tree.generator.model.TypeArgumentWithMultipleUpperBounds
import com.ivianuu.ast.tree.generator.pureAbstractElementType

enum class ImportKind(val postfix: String) {
    Element(""), Implementation(".impl"), Builder(".builder")
}

fun Builder.collectImports(): List<String> {
    val parents = parents.mapNotNull { it.fullQualifiedName }
    val builderDsl = "com.ivianuu.ast.builder.AstBuilderDsl"
    return when (this) {
        is LeafBuilder -> implementation.collectImports(
            parents,
            ImportKind.Builder,
        ) + implementation.fullQualifiedName!! + usedTypes.mapNotNull { it.fullQualifiedName } + builderDsl + "kotlin.contracts.*"
        is IntermediateBuilder -> {
            val fqns = parents + allFields.mapNotNull { it.fullQualifiedName } + allFields.flatMap {
                it.arguments.mapNotNull { it.fullQualifiedName }
            } + (materializedElement?.fullQualifiedName
                ?: throw IllegalStateException(type)) + builderDsl
            fqns.filterRedundantImports(packageName, ImportKind.Builder)
        }
    }.sorted()
}

fun Implementation.collectImports(
    base: List<String> = emptyList(),
    kind: ImportKind = ImportKind.Implementation
): List<String> {
    return element.collectImportsInternal(
        base + listOf(element.fullQualifiedName)
                + usedTypes.mapNotNull { it.fullQualifiedName }
                + arbitraryImportables.mapNotNull { it.fullQualifiedName }
                + parents.mapNotNull { it.fullQualifiedName }
                + listOfNotNull(
            pureAbstractElementType.fullQualifiedName?.takeIf { needPureAbstractElement },
            astImplementationDetailType.fullQualifiedName?.takeIf { isPublic || requiresOptIn },
        ),
        kind,
    )
}

fun Element.collectImports(): List<String> {
    val baseTypes = parents.mapTo(mutableListOf()) { it.fullQualifiedName }
    baseTypes += parentsArguments.values.flatMap { it.values }.mapNotNull { it.fullQualifiedName }
    val isBaseAstElement = this == AbstractAstTreeBuilder.baseAstElement
    if (isBaseAstElement) {
        baseTypes += compositeTransformResultType.fullQualifiedName!!
    }
    if (needPureAbstractElement) {
        baseTypes += pureAbstractElementType.fullQualifiedName!!
    }
    return collectImportsInternal(
        baseTypes,
        ImportKind.Element,
    )
}

private fun Element.collectImportsInternal(base: List<String>, kind: ImportKind): List<String> {
    val fqns = base + allFields.mapNotNull { it.fullQualifiedName } +
            allFields.flatMap { it.overriddenTypes.mapNotNull { it.fullQualifiedName } } +
            allFields.flatMap { it.arguments.mapNotNull { it.fullQualifiedName } } +
            typeArguments.flatMap { it.upperBounds.mapNotNull { it.fullQualifiedName } }
    return fqns.filterRedundantImports(packageName, kind)
}

private fun List<String>.filterRedundantImports(
    packageName: String,
    kind: ImportKind,
): List<String> {
    val realPackageName = "$packageName.${kind.postfix}"
    return filter { fqn ->
        fqn.dropLastWhile { it != '.' } != realPackageName
    }.distinct().sorted() + "$VISITOR_PACKAGE.*"
}


val KindOwner.needPureAbstractElement: Boolean
    get() = (kind != Implementation.Kind.Interface) && !allParents.any { it.kind == Implementation.Kind.AbstractClass }


val Field.isVal: Boolean get() = this is FieldList || (this is FieldWithDefault && origin is FieldList) || !isMutable

fun Field.transformFunctionDeclaration(returnType: String): String {
    return transformFunctionDeclaration(name.capitalize(), returnType)
}

fun transformFunctionDeclaration(transformName: String, returnType: String): String {
    return "fun <D> transform$transformName(transformer: FirTransformer<D>, data: D): $returnType"
}

fun Field.replaceFunctionDeclaration(
    overriddenType: Importable? = null,
    forceNullable: Boolean = false
): String {
    val capName = name.capitalize()
    val type = overriddenType?.typeWithArguments ?: typeWithArguments

    val typeWithNullable = if (forceNullable && !type.endsWith("?")) "$type?" else type

    return "fun replace$capName(new$capName: $typeWithNullable)"
}

val Field.mutableType: String
    get() = when (this) {
        is FieldList -> if (isMutable) "Mutable$typeWithArguments" else typeWithArguments
        is FieldWithDefault -> if (isMutable) origin.mutableType else typeWithArguments
        else -> typeWithArguments
    }

fun Field.call(): String = if (nullable) "?." else "."

fun Element.multipleUpperBoundsList(): String {
    return typeArguments.filterIsInstance<TypeArgumentWithMultipleUpperBounds>()
        .takeIf { it.isNotEmpty() }?.let { arguments ->
        val upperBoundsList = arguments.joinToString(", ", postfix = " ") { argument ->
            argument.upperBounds.joinToString(", ") { upperBound -> "${argument.name} : ${upperBound.typeWithArguments}" }
        }
        " where $upperBoundsList"
    } ?: " "
}

fun Implementation.Kind?.braces(): String = when (this) {
    Implementation.Kind.Interface -> ""
    Implementation.Kind.OpenClass, Implementation.Kind.AbstractClass -> "()"
    else -> throw IllegalStateException(this.toString())
}

val Element.safeDecapitalizedName: String get() =
    when (name) {
        "Class" -> "klass"
        "Break" -> "breakExpression"
        "Continue" -> "continueExpression"
        "Return" -> "returnExpression"
        "Try" -> "tryExpression"
        "Throw" -> "throwExpression"
        "When" -> "whenExpression"
        else -> name.decapitalize()
    }

val Importable.typeWithArguments: String
    get() = when (this) {
        is AbstractElement -> type + generics
        is Implementation -> type + element.generics
        is AstField -> element.typeWithArguments + if (nullable) "?" else ""
        is Field -> type + generics + if (nullable) "?" else ""
        is Type -> type + generics
        is ImplementationWithArg -> type + generics
        is LeafBuilder -> type + implementation.element.generics
        is IntermediateBuilder -> type
        else -> throw IllegalArgumentException()
    }

val ImplementationWithArg.generics: String
    get() = argument?.let { "<${it.type}>" } ?: ""

val AbstractElement.generics: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.name } }
        ?: ""

val Field.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }
        ?.let { it.joinToString(", ", "<", ">") { it.typeWithArguments } }
        ?: ""

val Element.typeParameters: String
    get() = typeArguments.takeIf { it.isNotEmpty() }
        ?.joinToString(", ", "<", "> ")
        ?: ""

val Type.generics: String
    get() = arguments.takeIf { it.isNotEmpty() }?.joinToString(",", "<", ">") ?: ""
