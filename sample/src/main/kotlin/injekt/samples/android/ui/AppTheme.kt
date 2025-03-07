/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import injekt.*

@Provide val AppColors = darkColors()

fun interface AppTheme {
  @Composable fun Content(content: @Composable () -> Unit)

  @Provide companion object {
    @Provide fun impl(appColors: Colors) = AppTheme { content ->
      MaterialTheme(colors = appColors) {
        Surface(content = content)
      }
    }
  }
}
