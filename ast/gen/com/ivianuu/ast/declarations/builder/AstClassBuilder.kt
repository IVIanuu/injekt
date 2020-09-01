package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationAttributes
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstTypeParameterRef
import com.ivianuu.ast.expressions.AstAnnotationCall
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.visitors.*
import org.jetbrains.kotlin.descriptors.ClassKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
interface AstClassBuilder : AstAnnotationContainerBuilder {
    abstract override val annotations: MutableList<AstAnnotationCall>
    abstract var origin: AstDeclarationOrigin
    abstract var attributes: AstDeclarationAttributes
    abstract val typeParameters: MutableList<AstTypeParameterRef>
    abstract var classKind: ClassKind
    abstract val superTypes: MutableList<AstType>
    abstract val declarations: MutableList<AstDeclaration>

    override fun build(): AstClass<*>
}
