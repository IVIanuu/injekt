/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.tree.generator

import com.ivianuu.ast.tree.generator.printer.printElements
import com.ivianuu.ast.tree.generator.util.configureInterfacesAndAbstractClasses
import com.ivianuu.ast.tree.generator.util.removePreviousGeneratedFiles
import java.io.File

fun main(args: Array<String>) {
    val generationPath = args.firstOrNull()?.let { File(it) }
        ?: File("ast/gen").absoluteFile

    NodeConfigurator.configureFields()
    ImplementationConfigurator.configureImplementations()
    configureInterfacesAndAbstractClasses(AstTreeBuilder)
    BuilderConfigurator.configureBuilders()
    removePreviousGeneratedFiles(generationPath)
    printElements(AstTreeBuilder, generationPath)
}
