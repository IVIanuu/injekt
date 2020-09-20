package com.ivianuu.injekt.compiler.backend.readercontextimpl

import com.ivianuu.injekt.Context
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName

@Context
interface ReaderContextImplContext

typealias InitTrigger = IrDeclarationWithName
