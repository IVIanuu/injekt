/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator.model

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.context.type
import com.ivianuu.ast.tree.generator.printer.typeWithArguments

// ----------- Simple field -----------

fun field(
    name: String,
    type: String,
    packageName: String?,
    customType: Importable? = null,
    nullable: Boolean = false,
    withReplace: Boolean = false
): Field {
    return SimpleField(name, type, packageName, customType, nullable, withReplace)
}

fun field(
    name: String,
    type: Type,
    nullable: Boolean = false,
    withReplace: Boolean = false
): Field {
    return SimpleField(name, type.typeWithArguments, type.packageName, null, nullable, withReplace)
}

fun field(
    name: String,
    typeWithArgs: Pair<Type, List<Importable>>,
    nullable: Boolean = false,
    withReplace: Boolean = false
): Field {
    val (type, args) = typeWithArgs
    return SimpleField(
        name,
        type.typeWithArguments,
        type.packageName,
        null,
        nullable,
        withReplace
    ).apply {
        arguments += args
    }
}

fun field(type: Type, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(
        type.type.decapitalize(),
        type.typeWithArguments,
        type.packageName,
        null,
        nullable,
        withReplace
    )
}

fun booleanField(name: String, withReplace: Boolean = false): Field {
    return field(name, AbstractAstTreeBuilder.boolean, null, withReplace = withReplace)
}

fun stringField(name: String, nullable: Boolean = false): Field {
    return field(name, AbstractAstTreeBuilder.string, null, null, nullable)
}

fun intField(name: String): Field {
    return field(name, AbstractAstTreeBuilder.int, null)
}

// ----------- Ast field -----------

fun field(
    name: String,
    type: Type,
    argument: String? = null,
    nullable: Boolean = false,
    withReplace: Boolean = false
): Field {
    return if (argument == null) {
        field(name, type, nullable, withReplace)
    } else {
        field(name, type to listOf(type(argument)), nullable, withReplace)
    }
}

fun field(
    name: String,
    element: AbstractElement,
    nullable: Boolean = false,
    withReplace: Boolean = false
): Field {
    return AstField(name, element, nullable, withReplace)
}

fun field(element: Element, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return AstField(element.name.decapitalize(), element, nullable, withReplace)
}

// ----------- Field list -----------

fun fieldList(name: String, type: Importable, withReplace: Boolean = false): Field {
    return FieldList(name, type, withReplace)
}

fun fieldList(element: Element, withReplace: Boolean = false): Field {
    return FieldList(element.name.decapitalize() + "s", element, withReplace)
}

// ----------- Field set -----------

typealias FieldSet = List<Field>

fun fieldSet(vararg fields: Field): FieldSet {
    return fields.toList()
}

@JvmName("foo")
infix fun FieldSet.with(sets: List<FieldSet>): FieldSet {
    return sets.flatten()
}

infix fun FieldSet.with(set: FieldSet): FieldSet {
    return this + set
}

fun Field.withTransform(needTransformInOtherChildren: Boolean = false): Field = copy().apply {
    needsSeparateTransform = true
    this.needTransformInOtherChildren = needTransformInOtherChildren
}

fun FieldSet.withTransform(): FieldSet = this.map { it.withTransform() }