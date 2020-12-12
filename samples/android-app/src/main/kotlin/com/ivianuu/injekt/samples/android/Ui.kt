package com.ivianuu.injekt.samples.android

import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.ivianuu.injekt.given

@Composable fun MyAppUi(repo: Repo = given, activity: ComponentActivity = given) {
    Text("Hello world $repo $activity")
}
