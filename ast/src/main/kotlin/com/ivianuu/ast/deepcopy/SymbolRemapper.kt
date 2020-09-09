package com.ivianuu.ast.deepcopy

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousObjectSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstEnumEntrySymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.symbols.impl.AstTypeAliasSymbol
import com.ivianuu.ast.symbols.impl.AstTypeParameterSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.visitors.AstVisitorVoid

class SymbolRemapper : AstVisitorVoid() {

    private val symbols = mutableMapOf<AstSymbol<*>, AstSymbol<*>>()

    fun <S : AstSymbol<*>> getSymbol(symbol: S): S = symbols[symbol] as? S ?: symbol

    override fun visitElement(element: AstElement) {
    }

    override fun <E> visitSymbolOwner(symbolOwner: AstSymbolOwner<E>) where E : AstSymbolOwner<E>, E : AstDeclaration {
        super.visitSymbolOwner(symbolOwner)
        symbols[symbolOwner.symbol] = when (val symbol = symbolOwner.symbol) {
            is AstAnonymousInitializerSymbol -> AstAnonymousInitializerSymbol()
            is AstRegularClassSymbol -> AstRegularClassSymbol(symbol.fqName)
            is AstAnonymousObjectSymbol -> AstAnonymousObjectSymbol()
            is AstTypeAliasSymbol -> AstTypeAliasSymbol(symbol.fqName)
            is AstEnumEntrySymbol -> AstEnumEntrySymbol(symbol.fqName)
            is AstNamedFunctionSymbol -> AstNamedFunctionSymbol(symbol.fqName)
            is AstConstructorSymbol -> AstConstructorSymbol(symbol.fqName)
            is AstAnonymousFunctionSymbol -> AstAnonymousFunctionSymbol()
            is AstPropertyAccessorSymbol -> AstPropertyAccessorSymbol()
            is AstTypeParameterSymbol -> AstTypeParameterSymbol(symbol.fqName)
            is AstPropertySymbol -> AstPropertySymbol(symbol.fqName)
            is AstValueParameterSymbol -> AstValueParameterSymbol(symbol.fqName)
            is AstVariableSymbol<*> -> AstVariableSymbol(symbol.fqName)
            else -> error("Unexpected symbol $symbol")
        }
    }

}
