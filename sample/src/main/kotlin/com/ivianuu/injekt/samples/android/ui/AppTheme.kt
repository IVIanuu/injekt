/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import com.ivianuu.injekt.Provide

fun interface AppTheme {
  @Composable operator fun invoke(content: @Composable () -> Unit)
}

@Provide val appTheme = AppTheme { content ->
  MaterialTheme(colors = darkColors()) {
    Surface(content = content)
  }
}
