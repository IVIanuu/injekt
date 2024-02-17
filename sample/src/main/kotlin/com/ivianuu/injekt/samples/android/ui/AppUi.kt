/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.ivianuu.injekt.*

fun interface AppUi {
  @Composable operator fun invoke()

  @Provide companion object {
    @Provide fun impl(presenter: CounterPresenter) = AppUi {
      Scaffold(
        topBar = {
          TopAppBar(
            title = { Text("Injekt sample") },
            backgroundColor = MaterialTheme.colors.primary
          )
        }
      ) {
        val state = presenter()
        Column(
          modifier = Modifier
            .padding(it)
            .fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("Count ${state.state}", style = MaterialTheme.typography.subtitle1)
          Spacer(Modifier.height(8.dp))
          Button(onClick = state.incCounter) {
            Text("Inc")
          }
          Spacer(Modifier.height(8.dp))
          Button(onClick = state.decCounter) {
            Text("Dec")
          }
        }
      }
    }
  }
}

