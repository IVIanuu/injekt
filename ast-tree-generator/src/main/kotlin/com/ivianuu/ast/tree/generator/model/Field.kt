/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator.model

import com.ivianuu.ast.tree.generator.printer.typeWithArguments

sealed class Field : Importable {
    abstract val name: String
    open val arguments = mutableListOf<Importable>()
    abstract val nullable: Boolean
    abstract val isAstType: Boolean

    var fromParent: Boolean = false

    open val defaultValueInImplementation: String? get() = null
    abstract var mutable: Boolean
    open var isMutableInInterface: Boolean = false
    open val withGetter: Boolean get() = false
    open val customSetter: String? get() = null
    open val fromDelegate: Boolean get() = false

    open val overriddenTypes: MutableSet<Importable> = mutableSetOf()
    open var useNullableForReplace: Boolean = false

    fun copy(): Field = internalCopy().also {
        updateFieldsInCopy(it)
    }

    protected fun updateFieldsInCopy(copy: Field) {
        if (copy !is FieldWithDefault) {
            copy.arguments.clear()
            copy.arguments.addAll(arguments)
            copy.mutable = mutable
            copy.overriddenTypes += overriddenTypes
            copy.useNullableForReplace = useNullableForReplace
        }
        copy.fromParent = fromParent
    }

    protected abstract fun internalCopy(): Field

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        other as Field
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

// ----------- Field with default -----------

class FieldWithDefault(val origin: Field) : Field() {
    override val name: String get() = origin.name
    override val type: String get() = origin.type
    override val nullable: Boolean get() = origin.nullable
    override val packageName: String? get() = origin.packageName
    override val isAstType: Boolean get() = origin.isAstType

    override val arguments: MutableList<Importable>
        get() = origin.arguments

    override val fullQualifiedName: String?
        get() = origin.fullQualifiedName

    override var defaultValueInImplementation: String? = origin.defaultValueInImplementation
    var defaultValueInBuilder: String? = null
    override var mutable: Boolean = origin.mutable
    override var isMutableInInterface: Boolean = origin.isMutableInInterface
    override var withGetter: Boolean = false
    override var customSetter: String? = null
    override var fromDelegate: Boolean = false
    var needAcceptAndTransform: Boolean = true
    override val overriddenTypes: MutableSet<Importable>
        get() = origin.overriddenTypes

    override var useNullableForReplace: Boolean
        get() = origin.useNullableForReplace
        set(_) {}

    override fun internalCopy(): Field {
        return FieldWithDefault(origin).also {
            it.defaultValueInImplementation = defaultValueInImplementation
            it.mutable = mutable
            it.withGetter = withGetter
            it.fromDelegate = fromDelegate
            it.needAcceptAndTransform = needAcceptAndTransform
        }
    }
}

class SimpleField(
    override val name: String,
    override val type: String,
    override val packageName: String?,
    val customType: Importable? = null,
    override val nullable: Boolean,
    override var mutable: Boolean = false
) : Field() {
    override val isAstType: Boolean = false
    override val fullQualifiedName: String?
        get() = customType?.fullQualifiedName ?: super.fullQualifiedName

    override fun internalCopy(): Field {
        return SimpleField(
            name,
            type,
            packageName,
            customType,
            nullable
        )
    }

    fun replaceType(newType: Type) = SimpleField(
        name,
        newType.type,
        newType.packageName,
        customType,
        nullable
    ).also {
        updateFieldsInCopy(it)
    }
}

class AstField(
    override val name: String,
    val element: AbstractElement,
    override val nullable: Boolean
) : Field() {
    init {
        if (element is ElementWithArguments) {
            arguments += element.typeArguments.map { Type(null, it.name) }
        }
    }

    override val type: String get() = element.type
    override val packageName: String? get() = element.packageName
    override val isAstType: Boolean = true

    override var mutable: Boolean = true

    override fun internalCopy(): Field {
        return AstField(
            name,
            element,
            nullable
        )
    }
}

// ----------- Field list -----------

class FieldList(
    override val name: String,
    val baseType: Importable
) : Field() {
    override var defaultValueInImplementation: String? = null
    override val packageName: String? get() = baseType.packageName
    override val fullQualifiedName: String? get() = baseType.fullQualifiedName
    override val type: String = "List<${baseType.typeWithArguments}>"

    override val nullable: Boolean
        get() = false

    override var mutable: Boolean = true

    override fun internalCopy(): Field {
        return FieldList(
            name,
            baseType
        )
    }

    override val isAstType: Boolean = baseType is Element
}
