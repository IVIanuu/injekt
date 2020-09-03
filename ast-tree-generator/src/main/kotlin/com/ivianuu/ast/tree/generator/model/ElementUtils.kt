/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator.model

import com.ivianuu.ast.tree.generator.context.AbstractAstTreeBuilder
import com.ivianuu.ast.tree.generator.context.type
import com.ivianuu.ast.tree.generator.printer.typeWithArguments

fun field(
    name: String,
    type: String,
    packageName: String?,
    customType: Importable? = null,
    nullable: Boolean = false,
    withReplace: Boolean = true
): Field {
    return SimpleField(name, type, packageName, customType, nullable, withReplace)
}

fun field(
    name: String,
    type: Type,
    nullable: Boolean = false,
    withReplace: Boolean = true
): Field {
    return SimpleField(name, type.typeWithArguments, type.packageName, null, nullable, withReplace)
}

fun field(
    name: String,
    typeWithArgs: Pair<Type, List<Importable>>,
    nullable: Boolean = false,
    withReplace: Boolean = true
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

fun field(type: Type, nullable: Boolean = false, withReplace: Boolean = true): Field {
    return SimpleField(
        type.type.decapitalize(),
        type.typeWithArguments,
        type.packageName,
        null,
        nullable,
        withReplace
    )
}

fun booleanField(name: String, withReplace: Boolean = true): Field {
    return field(name, AbstractAstTreeBuilder.boolean, null, withReplace = withReplace)
}

fun stringField(name: String, nullable: Boolean = false, withReplace: Boolean = true): Field {
    return field(name, AbstractAstTreeBuilder.string, null, null, nullable, withReplace)
}

fun field(
    name: String,
    type: Type,
    argument: String? = null,
    nullable: Boolean = false,
    withReplace: Boolean = true
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
    withReplace: Boolean = true
): Field {
    return AstField(name, element, nullable, withReplace)
}

fun field(element: Element, nullable: Boolean = false, withReplace: Boolean = true): Field {
    return AstField(element.name.decapitalize(), element, nullable, withReplace)
}

fun fieldList(name: String, type: Importable, withReplace: Boolean = true, nullableBaseType: Boolean = false): Field {
    return FieldList(name, type, withReplace, nullableBaseType)
}

fun fieldList(element: Element, withReplace: Boolean = true, nullableBaseType: Boolean = false): Field {
    return FieldList(element.name.decapitalize() + "s", element, withReplace, nullableBaseType)
}

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
