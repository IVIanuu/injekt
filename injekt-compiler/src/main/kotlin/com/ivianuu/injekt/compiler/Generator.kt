package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.symbol.KSFile

interface Generator {
    fun generate(files: List<KSFile>)
}
