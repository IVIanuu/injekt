package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import org.jetbrains.kotlin.name.FqName

interface AstPackageFragment : AstDeclarationContainer {
    var packageFqName: FqName
}

tailrec fun AstElement.getPackageFragment(): AstPackageFragment? {
    if (this is AstPackageFragment) return this
    val vParent = (this as? AstDeclaration)?.parent
    return when (vParent) {
        is AstPackageFragment -> vParent
        else -> vParent?.getPackageFragment()
    }
}
