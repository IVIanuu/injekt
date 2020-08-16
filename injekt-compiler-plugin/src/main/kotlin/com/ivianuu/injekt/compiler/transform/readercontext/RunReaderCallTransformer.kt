package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.Indexer
import com.ivianuu.injekt.compiler.transform.InjektContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName

class RunReaderCallTransformer(
    injektContext: InjektContext,
    private val indexer: Indexer
) : AbstractInjektTransformer(injektContext) {

    private val newIndexBuilders = mutableListOf<NewIndexBuilder>()

    private data class NewIndexBuilder(
        val originatingDeclaration: IrDeclarationWithName,
        val classBuilder: IrClass.() -> Unit
    )

    override fun lower() {
    }

}