/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.AstTreeBuilder.block
import com.ivianuu.ast.tree.generator.AstTreeBuilder.declaration
import com.ivianuu.ast.tree.generator.AstTreeBuilder.expression
import com.ivianuu.ast.tree.generator.AstTreeBuilder.file
import com.ivianuu.ast.tree.generator.AstTreeBuilder.functionCall
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeParameter
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeProjection
import com.ivianuu.ast.tree.generator.AstTreeBuilder.type
import com.ivianuu.ast.tree.generator.AstTreeBuilder.valueParameter
import com.ivianuu.ast.tree.generator.context.type
import com.ivianuu.ast.tree.generator.model.Field
import com.ivianuu.ast.tree.generator.model.booleanField
import com.ivianuu.ast.tree.generator.model.field
import com.ivianuu.ast.tree.generator.model.fieldList
import com.ivianuu.ast.tree.generator.model.fieldSet

object FieldSets {
    val callee = field("callee", astSymbolType)

    val receivers = fieldSet(
        field("dispatchReceiver", expression, nullable = true),
        field("extensionReceiver", expression, nullable = true)
    )

    val typeArguments = fieldList("typeArguments", typeProjection)

    val valueArguments =
        fieldList("valueArguments", expression)

    val declarations = fieldList(declaration)
    val files = fieldList(file)

    val annotations = fieldList("annotations", functionCall)

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

    val typeField = field(type)

    fun receiverType(nullable: Boolean = false) = field("receiverType", type, nullable)

    val valueParameters = fieldList(valueParameter)

    val typeParameters = fieldList("typeParameters", typeParameter)

    val name = field(nameType)

    val initializer = field("initializer", expression, nullable = true)

    fun superTypes() = fieldList("superTypes", type)

    val classKind = field(classKindType)

    val visibility = field(visibilityType)
    val modality = field(modalityType)
    val expectActual = fieldSet(
        booleanField("isExpect"),
        booleanField("isActual")
    )
    val isOperator = booleanField("isOperator")
    val isInfix = booleanField("isInfix")
    val isInline = booleanField("isInline")
    val isTailrec = booleanField("isTailrec")
    val isExternal = booleanField("isExternal")
    val isConst = booleanField("isConst")
    val isLateinit = booleanField("isLateinit")
    val isInner = booleanField("isInner")
    val isCompanion = booleanField("isCompanion")
    val isData = booleanField("isData")
    val isSuspend = booleanField("isSuspend")
    val isFun = booleanField("isFun")

}
