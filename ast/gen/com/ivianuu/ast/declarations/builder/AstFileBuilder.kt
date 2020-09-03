package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.builder.AstPackageFragmentBuilder
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
class AstFileBuilder : AstPackageFragmentBuilder {
    val annotations: MutableList<AstFunctionCall> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
    lateinit var name: String
    lateinit var packageFqName: FqName

    override fun build(): AstFile {
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

@OptIn(ExperimentalContracts::class)
inline fun AstFile.copy(init: AstFileBuilder.() -> Unit = {}): AstFile {
    val copyBuilder = AstFileBuilder()
    copyBuilder.annotations.addAll(annotations)
    copyBuilder.declarations.addAll(declarations)
    copyBuilder.name = name
    copyBuilder.packageFqName = packageFqName
    return copyBuilder.apply(init).build()
}
