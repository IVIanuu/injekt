package com.ivianuu.injekt.compiler.resolution

enum class GivenKind {
  NONE, PROVIDE, USING
}

val GivenKind.isGiven: Boolean
  get() = this == GivenKind.PROVIDE || this == GivenKind.USING
