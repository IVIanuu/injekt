package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstPureAbstractElement
import org.jetbrains.kotlin.name.Name
import com.ivianuu.ast.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class AstModuleFragment : AstPureAbstractElement(), AstElement {
    abstract val name: Name
    abstract val files: List<AstFile>

    override fun <R, D> accept(visitor: AstVisitor<R, D>, data: D): R = visitor.visitModuleFragment(this, data)

    abstract fun replaceFiles(newFiles: List<AstFile>)
}
