package com.ivianuu.ast

import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId

interface AstSymbolProvider {

    fun referenceClass(classId: ClassId): AstRegularClassSymbol?

    fun referenceFunctions(callableId: CallableId): List<AstNamedFunctionSymbol>

    fun referenceConstructors(classId: ClassId): List<AstConstructorSymbol>

    fun referenceProperties(callableId: CallableId): List<AstPropertySymbol>

}
