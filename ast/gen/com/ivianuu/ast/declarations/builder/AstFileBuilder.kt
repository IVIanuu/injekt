package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationOrigin
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.impl.AstFileImpl
import com.ivianuu.ast.expressions.AstAnnotationCall
import org.jetbrains.kotlin.name.FqName
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstFileBuilder : AstAnnotationContainerBuilder {
    lateinit var origin: AstDeclarationOrigin
    override val annotations: MutableList<AstAnnotationCall> = mutableListOf()
    val declarations: MutableList<AstDeclaration> = mutableListOf()
    lateinit var name: String
    lateinit var packageFqName: FqName

    override fun build(): AstFile {
        return AstFileImpl(
            origin,
            annotations,
            declarations,
            name,
            packageFqName,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildFile(init: AstFileBuilder.() -> Unit): AstFile {
    return AstFileBuilder().apply(init).build()
}
