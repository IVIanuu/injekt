package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstImport
import com.ivianuu.ast.declarations.AstResolvedImport
import com.ivianuu.ast.declarations.impl.AstResolvedImportImpl
import org.jetbrains.kotlin.name.FqName
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstResolvedImportBuilder {
    lateinit var delegate: AstImport
    lateinit var packageFqName: FqName
    var relativeClassName: FqName? = null

    fun build(): AstResolvedImport {
        return AstResolvedImportImpl(
            delegate,
            packageFqName,
            relativeClassName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildResolvedImport(init: AstResolvedImportBuilder.() -> Unit): AstResolvedImport {
    return AstResolvedImportBuilder().apply(init).build()
}
