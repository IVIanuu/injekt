package com.ivianuu.ast

import com.ivianuu.ast.builder.AstBuilder
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName

interface AstContext : AstBuilder {
    override val context: AstContext
        get() = this
    val builtIns: AstBuiltIns

    fun referenceClass(fqName: FqName): AstRegularClassSymbol?

    fun referenceFunctions(fqName: FqName): List<AstNamedFunctionSymbol>

    fun referenceConstructors(classFqName: FqName): List<AstConstructorSymbol>

    fun referenceProperties(fqName: FqName): List<AstPropertySymbol>?

}
