package com.ivianuu.ast.declarations.builder

import com.ivianuu.ast.builder.AstBuilderDsl
import com.ivianuu.ast.declarations.AstImport
import com.ivianuu.ast.declarations.impl.AstImportImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.contracts.ExperimentalContracts

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@AstBuilderDsl
class AstImportBuilder {
    var importedFqName: FqName? = null
    var isAllUnder: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    var aliasName: Name? = null

    fun build(): AstImport {
        return AstImportImpl(
            importedFqName,
            isAllUnder,
            aliasName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildImport(init: AstImportBuilder.() -> Unit): AstImport {
    return AstImportBuilder().apply(init).build()
}
