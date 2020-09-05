package com.ivianuu.ast

import com.ivianuu.ast.symbols.CallableId
import org.jetbrains.kotlin.fir.lightTree.converter.nameAsSafeName
import org.jetbrains.kotlin.name.FqName

object AstIntrinsics {
    val Package = FqName("ast.intrinsics")

    val LessThan = CallableId(Package, "lessThan".nameAsSafeName())
    val GreaterThan = CallableId(Package, "greaterThan".nameAsSafeName())
    val LessThanEqual = CallableId(Package, "lessThanEqual".nameAsSafeName())
    val GreaterThanEqual = CallableId(Package, "greaterThanEqual".nameAsSafeName())

    val StructuralEqual = CallableId(Package, "structuralEqual".nameAsSafeName())
    val StructuralNotEqual = CallableId(Package, "structuralNotEqual".nameAsSafeName())
    val IdentityEqual = CallableId(Package, "identityEqual".nameAsSafeName())
    val IdentityNotEqual = CallableId(Package, "identityNotEqual".nameAsSafeName())

    val LazyAnd = CallableId(Package, "lazyAnd".nameAsSafeName())
    val LazyOr = CallableId(Package, "lazyOr".nameAsSafeName())

}
