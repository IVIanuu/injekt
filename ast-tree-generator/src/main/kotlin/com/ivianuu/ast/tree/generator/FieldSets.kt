/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.AstTreeBuilder.annotationCall
import com.ivianuu.ast.tree.generator.AstTreeBuilder.block
import com.ivianuu.ast.tree.generator.AstTreeBuilder.declaration
import com.ivianuu.ast.tree.generator.AstTreeBuilder.declarationStatus
import com.ivianuu.ast.tree.generator.AstTreeBuilder.expression
import com.ivianuu.ast.tree.generator.AstTreeBuilder.reference
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeParameter
import com.ivianuu.ast.tree.generator.AstTreeBuilder.typeParameterRef
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
    val calleeReference = field("calleeReference", reference, withReplace = true)

    val receivers = fieldSet(
        field("explicitReceiver", expression, nullable = true, withReplace = true).withTransform(),
        field("dispatchReceiver", expression).withTransform(),
        field("extensionReceiver", expression).withTransform()
    )

    val typeArguments =
        fieldList("typeArguments", typeProjection, withReplace = true)

    val arguments =
        fieldList("arguments", expression)

    val declarations = fieldList(declaration)

    val annotations =
        fieldList("annotations", annotationCall).withTransform(needTransformInOtherChildren = true)

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

    val typeParameterRefs = fieldList("typeParameters", typeParameterRef)

    val name = field(nameType)

    val initializer = field("initializer", expression, nullable = true)

    fun superTypes(withReplace: Boolean = false) =
        fieldList("superTypes", type, withReplace)

    val classKind = field(classKindType)

    val status = field("status", declarationStatus)

    val visibility = field(visibilityType)

    val modality = field(modalityType)
}
