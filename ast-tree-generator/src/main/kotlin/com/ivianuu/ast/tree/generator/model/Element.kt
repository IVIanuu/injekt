package com.ivianuu.ast.tree.generator.model

import com.ivianuu.ast.tree.generator.printer.BASE_PACKAGE
import com.ivianuu.ast.tree.generator.printer.typeWithArguments
import com.ivianuu.ast.tree.generator.util.set

interface KindOwner : Importable {
    var kind: Implementation.Kind?
    val allParents: List<KindOwner>
}

interface FieldContainer {
    val allFields: List<Field>
    operator fun get(fieldName: String): Field?
}

interface AbstractElement : FieldContainer, KindOwner {
    val fields: Set<Field>
    val parents: List<AbstractElement>
    val typeArguments: List<TypeArgument>
    val parentsArguments: Map<AbstractElement, Map<Importable, Importable>>
    val baseTransformerType: AbstractElement?
    val transformerType: AbstractElement
    val doesNotNeedImplementation: Boolean
    val needTransformOtherChildren: Boolean
    val allImplementations: List<Implementation>
    val allAstFields: List<Field>
    val defaultImplementation: Implementation?
    val customImplementations: List<Implementation>
    val overridenFields: Map<Field, Map<Importable, Boolean>>
    val useNullableForReplace: Set<Field>

    override val allParents: List<KindOwner> get() = parents
}

class Element(val name: String, kind: Kind) : AbstractElement {
    override val fields = mutableSetOf<Field>()
    override val type: String = "Ast$name"
    override val packageName: String =
        BASE_PACKAGE + kind.packageName.let { if (it.isBlank()) it else "." + it }
    override val fullQualifiedName: String get() = super.fullQualifiedName!!
    override val parents = mutableListOf<Element>()
    override var defaultImplementation: Implementation? = null
    override val customImplementations = mutableListOf<Implementation>()
    override val typeArguments = mutableListOf<TypeArgument>()
    override val parentsArguments =
        mutableMapOf<AbstractElement, MutableMap<Importable, Importable>>()
    override var kind: Implementation.Kind? = null
        set(value) {
            if (value != Implementation.Kind.Interface && value != Implementation.Kind.AbstractClass) {
                throw IllegalArgumentException(value.toString())
            }
            field = value
        }
    var _needTransformOtherChildren: Boolean = false

    override var baseTransformerType: Element? = null
    override val transformerType: Element get() = baseTransformerType ?: this

    override var doesNotNeedImplementation: Boolean = false

    override val needTransformOtherChildren: Boolean get() = _needTransformOtherChildren || parents.any { it.needTransformOtherChildren }
    override val overridenFields: MutableMap<Field, MutableMap<Importable, Boolean>> =
        mutableMapOf()
    override val useNullableForReplace: MutableSet<Field> = mutableSetOf()
    override val allImplementations: List<Implementation> by lazy {
        if (doesNotNeedImplementation) {
            emptyList()
        } else {
            val implementations = customImplementations.toMutableList()
            defaultImplementation?.let { implementations += it }
            implementations
        }
    }

    override val allFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        result.addAll(fields.toList().asReversed())
        result.forEach { overridenFields[it, it] = false }
        for (parentField in parentFields.asReversed()) {
            val overrides = !result.add(parentField)
            if (overrides) {
                val existingField = result.first { it == parentField }
                existingField.fromParent = true
                existingField.needsSeparateTransform =
                    existingField.needsSeparateTransform || parentField.needsSeparateTransform
                existingField.needTransformInOtherChildren =
                    existingField.needTransformInOtherChildren || parentField.needTransformInOtherChildren
                existingField.withReplace = parentField.withReplace || existingField.withReplace
                if (parentField.type != existingField.type && parentField.withReplace) {
                    existingField.overridenTypes += parentField
                    overridenFields[existingField, parentField] = false
                } else {
                    overridenFields[existingField, parentField] = true
                    if (parentField.nullable != existingField.nullable) {
                        existingField.useNullableForReplace = true
                    }
                }
            } else {
                overridenFields[parentField, parentField] = true
            }
        }
        result.toList().asReversed()
    }

    val parentFields: List<Field> by lazy {
        val result = LinkedHashSet<Field>()
        parents.forEach { parent ->
            val fields = parent.allFields.map { field ->
                val copy = (field as? SimpleField)?.let { simpleField ->
                    parentsArguments[parent]?.get(Type(null, simpleField.type))?.let {
                        simpleField.replaceType(Type(it.packageName, it.type))
                    }
                } ?: field.copy()
                copy.apply {
                    arguments.replaceAll {
                        parentsArguments[parent]?.get(it) ?: it
                    }
                    fromParent = true
                }
            }
            result.addAll(fields)
        }
        result.toList()
    }

    override val allAstFields: List<Field> by lazy {
        allFields.filter { it.isAstType }
    }

    override fun toString(): String {
        return typeWithArguments
    }

    override fun get(fieldName: String): Field? {
        return allFields.firstOrNull { it.name == fieldName }
    }

    enum class Kind(val packageName: String) {
        Expression("expressions"),
        Declaration("declarations"),
        Reference("references"),
        Type("types"),
        Other("")
    }
}

class ElementWithArguments(val element: Element, override val typeArguments: List<TypeArgument>) :
    AbstractElement by element {
    override fun equals(other: Any?): Boolean {
        return element.equals(other)
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }
}

sealed class TypeArgument(val name: String) {
    abstract val upperBounds: List<Importable>
}

class SimpleTypeArgument(name: String, val upperBound: Importable?) : TypeArgument(name) {
    override val upperBounds: List<Importable> = listOfNotNull(upperBound)

    override fun toString(): String {
        var result = name
        if (upperBound != null) {
            result += " : ${upperBound.typeWithArguments}"
        }
        return result
    }
}

class TypeArgumentWithMultipleUpperBounds(
    name: String,
    override val upperBounds: List<Importable>
) : TypeArgument(name) {
    override fun toString(): String {
        return name
    }
}

data class ArbitraryImportable(override val packageName: String, override val type: String) :
    Importable
