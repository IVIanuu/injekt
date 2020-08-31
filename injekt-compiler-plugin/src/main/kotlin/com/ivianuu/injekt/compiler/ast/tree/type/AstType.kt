/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.injekt.compiler.ast.tree.type

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.fqName
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformResult
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstTransformer
import com.ivianuu.injekt.compiler.ast.tree.visitor.AstVisitor
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformInplace
import com.ivianuu.injekt.compiler.ast.tree.visitor.transformSingle
import org.jetbrains.kotlin.name.FqName

class AstType : AstAnnotationContainer, AstTypeArgument, AstTypeProjection {

    override var variance: AstVariance?
        get() = null
        set(value) {
            TODO()
        }

    override var type: AstType
        get() = this
        set(value) {
            TODO()
        }

    lateinit var classifier: AstClassifier
    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()
    var hasQuestionMark: Boolean = false
    val arguments: MutableList<AstTypeArgument> = mutableListOf()
    var abbreviation: AstTypeAbbreviation? = null

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitType(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
        abbreviation?.accept(visitor, data)
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstType> = accept(transformer, data) as AstTransformResult<AstType>

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        arguments.transformInplace(transformer, data)
        abbreviation = abbreviation?.transformSingle(transformer, data)
    }

}

interface AstTypeArgument : AstElement

object AstStarProjection : AstTypeArgument {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeArgument(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstType> = accept(transformer, data) as AstTransformResult<AstType>

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
    }

}

interface AstTypeProjection : AstTypeArgument {
    var variance: AstVariance?
    var type: AstType
}

class AstTypeProjectionImpl(
    override var variance: AstVariance?,
    override var type: AstType
) : AstTypeProjection {

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitTypeProjection(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        type.accept(visitor, data)
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstTypeProjection> =
        accept(transformer, data) as AstTransformResult<AstTypeProjection>

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        type = type.transformSingle(transformer, data)
    }

}

class AstTypeAbbreviation(
    var typeAlias: AstTypeAlias,
) : AstAnnotationContainer {
    override val annotations: MutableList<AstQualifiedAccess> = mutableListOf()
    var hasQuestionMark: Boolean = false
    val arguments: MutableList<AstTypeArgument> = mutableListOf()

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R =
        visitor.visitElement(this, data)

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transform(
        transformer: AstTransformer<D>,
        data: D
    ): AstTransformResult<AstTypeProjection> =
        accept(transformer, data) as AstTransformResult<AstTypeProjection>

    override fun <D> transformChildren(transformer: AstTransformer<D>, data: D) {
        annotations.transformInplace(transformer, data)
        arguments.transformInplace(transformer, data)
    }

}

fun AstType.copy(
    classifier: AstClassifier = this.classifier,
    annotations: MutableList<AstQualifiedAccess> = this.annotations,
    hasQuestionMark: Boolean = this.hasQuestionMark,
    arguments: MutableList<AstTypeArgument> = this.arguments,
    abbreviation: AstTypeAbbreviation? = this.abbreviation
) = AstType().apply {
    this.classifier = classifier
    this.annotations += annotations
    this.hasQuestionMark = hasQuestionMark
    this.arguments += arguments
    this.abbreviation = abbreviation
}

fun AstType.isClassType(fqName: FqName) =
    (classifier as? AstClass)?.fqName == fqName

val AstType.classOrNull: AstClass? get() = classifier as? AstClass
val AstType.classOrFail: AstClass get() = classOrNull ?: error("Expected class classifier $this")
