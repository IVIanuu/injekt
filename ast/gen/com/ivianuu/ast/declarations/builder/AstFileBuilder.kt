package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.impl.AstFileImpl
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.visitors.*
import kotlin.contracts.*
import org.jetbrains.kotlin.name.FqName

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstFileBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    val declarations: MutableList<AstDeclaration> = mutableListOf()
    lateinit var name: String
    lateinit var packageFqName: FqName

    fun build(): AstFile {
        return AstFileImpl(
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
