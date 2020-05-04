package com.ivianuu.injekt.sample

import android.app.Application
import android.content.Context
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.Transient
import com.ivianuu.injekt.childFactory
import com.ivianuu.injekt.createImplementation

class App : Application() {
    val component by lazy { createAppComponent(this) }
}

interface AppComponent {
    val repo: Repo
    val activityComponentFactory: @ChildFactory (MainActivity) -> ActivityComponent
}

@Factory
fun createAppComponent(app: App): AppComponent = createImplementation {
    childFactory(::activityComponentFactory)
}

@Transient
class MyWorker(
    @Assisted private val context: Context,
    @Assisted private val workerParameters: WorkerParameters,
    private val repo: Repo
) : Worker(context, workerParameters)

