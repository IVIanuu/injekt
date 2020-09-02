/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.AstTreeBuilder.block
import com.ivianuu.ast.tree.generator.AstTreeBuilder.call
import com.ivianuu.ast.tree.generator.AstTreeBuilder.declaration
import com.ivianuu.ast.tree.generator.AstTreeBuilder.declarationStatus
import com.ivianuu.ast.tree.generator.AstTreeBuilder.expression
import com.ivianuu.ast.tree.generator.AstTreeBuilder.file
import com.ivianuu.ast.tree.generator.AstTreeBuilder.functionCall
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeParameter
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeProjection
import com.ivianuu.ast.tree.generator.AstTreeBuilder.type
import com.ivianuu.ast.tree.generator.AstTreeBuilder.valueParameter
import com.ivianuu.ast.tree.generator.context.type
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.field
import com.ivianuu.ast.tree.generator.model.fieldList
import com.ivianuu.ast.tree.generator.model.fieldSet
import com.ivianuu.ast.tree.generator.model.withTransform

object FieldSets {
    val callee = field("callee", astSymbolType, withReplace = true)

    val receivers = fieldSet(
        field("dispatchReceiver", expression, nullable = true).withTransform(),
        field("extensionReceiver", expression, nullable = true).withTransform()
    )

    val typeArguments =
        fieldList("typeArguments", typeProjection, withReplace = true)

    val valueArguments =
        fieldList("valueArguments", expression)

    val declarations = fieldList(declaration)
    val files = fieldList(file)

    val annotations =
        fieldList("annotations", functionCall).withTransform(needTransformInOtherChildren = true)

    fun symbolWithPackage(
        packageName: String?,
        symbolClassName: String,
        argument: String? = null
    ): Field {
        return field("symbol", type(packageName, symbolClassName), argument)
    }

    fun symbol(symbolClassName: String, argument: String? = null): Field =
        symbolWithPackage("ast.symbols.impl", symbolClassName, argument)

    fun body(nullable: Boolean = false) =
        field("body", block, nullable)

    val returnType =
        field("returnType", type)

    val typeField =
        field(type, withReplace = true)

    fun receiverType(nullable: Boolean = false) = field("receiverType", type, nullable)

    val valueParameters = fieldList(valueParameter)

    val typeParameters = fieldList("typeParameters", typeParameter)

    val name = field(nameType)

    val initializer = field("initializer", expression, nullable = true)

    fun superTypes(withReplace: Boolean = false) =
        fieldList("superTypes", type, withReplace)

    val classKind = field(classKindType)

    val status = field("status", declarationStatus)

    val visibility = field(visibilityType)

    val modality = field(modalityType)
}
