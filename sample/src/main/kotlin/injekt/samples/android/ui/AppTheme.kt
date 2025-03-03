/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package injekt.samples.android.ui

import androidx.compose.material.*
import androidx.compose.runtime.*
import injekt.Provide

fun interface AppTheme {
  @Composable fun Content(content: @Composable () -> Unit)

  @Provide companion object {
    @Provide val impl = AppTheme { content ->
      MaterialTheme(colors = darkColors()) {
        Surface(content = content)
      }
    }
  }
}
