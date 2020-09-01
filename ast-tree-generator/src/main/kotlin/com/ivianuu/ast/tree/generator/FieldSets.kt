/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.FirTreeBuilder.annotationCall
import com.ivianuu.ast.tree.generator.FirTreeBuilder.block
import com.ivianuu.ast.tree.generator.FirTreeBuilder.controlFlowGraphReference
import com.ivianuu.ast.tree.generator.FirTreeBuilder.declaration
import com.ivianuu.ast.tree.generator.FirTreeBuilder.declarationStatus
import com.ivianuu.ast.tree.generator.FirTreeBuilder.expression
import com.ivianuu.ast.tree.generator.FirTreeBuilder.reference
import com.ivianuu.ast.tree.generator.FirTreeBuilder.typeParameter
import com.ivianuu.ast.tree.generator.FirTreeBuilder.typeParameterRef
import com.ivianuu.ast.tree.generator.FirTreeBuilder.typeProjection
import com.ivianuu.ast.tree.generator.FirTreeBuilder.typeRef
import com.ivianuu.ast.tree.generator.FirTreeBuilder.valueParameter
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
        symbolWithPackage("fir.symbols.impl", symbolClassName, argument)

    fun body(nullable: Boolean = false) =
        field("body", block, nullable)

    val returnTypeRef =
        field("returnTypeRef", typeRef)

    val typeRefField =
        field(typeRef, withReplace = true)

    fun receiverTypeRef(nullable: Boolean = false) = field("receiverTypeRef", typeRef, nullable)

    val valueParameters = fieldList(valueParameter)

    val typeParameters = fieldList("typeParameters", typeParameter)

    val typeParameterRefs = fieldList("typeParameters", typeParameterRef)

    val name = field(nameType)

    val initializer = field("initializer", expression, nullable = true)

    fun superTypeRefs(withReplace: Boolean = false) =
        fieldList("superTypeRefs", typeRef, withReplace)

    val classKind = field(classKindType)

    val status = field("status", declarationStatus)

    val controlFlowGraphReferenceField = field(
        "controlFlowGraphReference",
        controlFlowGraphReference,
        withReplace = true,
        nullable = true
    )

    val visibility = field(visibilityType)

    val effectiveVisibility = field("effectiveVisibility", effectiveVisibilityType)

    val modality = field(modalityType, nullable = true)

    val scopeProvider = field("scopeProvider", firScopeProviderType)
}
