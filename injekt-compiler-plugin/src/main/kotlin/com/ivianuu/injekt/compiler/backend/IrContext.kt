package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

@Context
interface IrContext

@Reader
inline val pluginContext: IrPluginContext
    get() = given()

@Reader
inline val irModule: IrModuleFragment
    get() = given()

@Reader
inline val injektSymbols: InjektSymbols
    get() = given()
