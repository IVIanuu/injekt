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
    nullable: Boolean = false
): Field {
    return SimpleField(name, type, packageName, customType, nullable)
}

fun field(
    name: String,
    type: Type,
    nullable: Boolean = false
): Field {
    return SimpleField(name, type.typeWithArguments, type.packageName, null, nullable)
}

fun field(
    name: String,
    typeWithArgs: Pair<Type, List<Importable>>,
    nullable: Boolean = false
): Field {
    val (type, args) = typeWithArgs
    return SimpleField(
        name,
        type.typeWithArguments,
        type.packageName,
        null,
        nullable
    ).apply {
        arguments += args
    }
}

fun field(type: Type, nullable: Boolean = false): Field {
    return SimpleField(
        type.type.decapitalize(),
        type.typeWithArguments,
        type.packageName,
        null,
        nullable
    )
}

fun booleanField(name: String): Field {
    return field(name, AbstractAstTreeBuilder.boolean, null)
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
    nullable: Boolean = false
): Field {
    return if (argument == null) {
        field(name, type, nullable)
    } else {
        field(name, type to listOf(type(argument)), nullable)
    }
}

fun field(
    name: String,
    element: AbstractElement,
    nullable: Boolean = false
): Field {
    return AstField(name, element, nullable)
}

fun field(element: Element, nullable: Boolean = false): Field {
    return AstField(element.name.decapitalize(), element, nullable)
}

// ----------- Field list -----------

fun fieldList(name: String, type: Importable): Field {
    return FieldList(name, type)
}

fun fieldList(element: Element): Field {
    return FieldList(element.name.decapitalize() + "s", element)
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
