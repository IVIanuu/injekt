package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstElement
import org.jetbrains.kotlin.name.FqName

interface AstPackageFragment : AstDeclarationContainer {
    var packageFqName: FqName
}

tailrec fun AstElement.getPackageFragment(): AstPackageFragment? {
    if (this is AstPackageFragment) return this
    return when (val vParent = (this as? AstDeclaration)?.parent) {
        is AstPackageFragment -> vParent
        else -> vParent?.getPackageFragment()
    }
}
