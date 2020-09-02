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

    val Equal = CallableId(Package, "equal".nameAsSafeName())
    val NotEqual = CallableId(Package, "notEqual".nameAsSafeName())
    val Identity = CallableId(Package, "identity".nameAsSafeName())
    val NotIdentity = CallableId(Package, "notIdentity".nameAsSafeName())

    val And = CallableId(Package, "andAnd".nameAsSafeName())
    val Or = CallableId(Package, "orOr".nameAsSafeName())

    val Is = CallableId(Package, "is".nameAsSafeName())
    val NotIs = CallableId(Package, "notIs".nameAsSafeName())
    val As = CallableId(Package, "as".nameAsSafeName())
    val SafeAs = CallableId(Package, "safeAs".nameAsSafeName())
}

class AstBuiltIns {



}
