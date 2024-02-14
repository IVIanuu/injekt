/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.samples.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.samples.android.data.CounterDb
import kotlinx.coroutines.launch

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

data class CounterState(
  val state: Int,
  val incCounter: () -> Unit,
  val decCounter: () -> Unit
)

fun interface CounterPresenter {
  @Composable operator fun invoke(): CounterState

  @Provide companion object {
    @Provide fun impl(db: CounterDb) = CounterPresenter {
      val scope = rememberCoroutineScope()
      CounterState(
        state = db.counter.collectAsState(0).value,
        incCounter = {
          scope.launch {
            db.updateCounter { it.inc() }
          }
        },
        decCounter = {
          scope.launch {
            db.updateCounter { it.dec() }
          }
        }
      )
    }
  }
}
