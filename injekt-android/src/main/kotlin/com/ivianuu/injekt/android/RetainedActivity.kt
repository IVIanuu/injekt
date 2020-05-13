package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.EntryPoint
import com.ivianuu.injekt.InstallIn
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.entryPointOf

@Scope
annotation class RetainedActivityScoped

interface RetainedActivityComponent

val ComponentActivity.retainedActivityComponent: RetainedActivityComponent
    get() {
        val holder = ViewModelProvider(this, RetainedActivityComponentHolder.Factory)
            .get(RetainedActivityComponentHolder::class.java)

        synchronized(holder) {
            if (holder.component == null) {
                holder.component =
                    entryPointOf<RetainedActivityComponentFactoryOwner>(application.applicationComponent)
                        .retainedActivityComponentFactory()
            }
        }

        return holder.component!!
    }

/*
@CompositionFactory
fun createRetainedActivityComponent(): RetainedActivityComponent {
    parent<ApplicationComponent>()
    scope<RetainedActivityScoped>()
    return createImpl()
}*/

@InstallIn<ApplicationComponent>
@EntryPoint
interface RetainedActivityComponentFactoryOwner {
    val retainedActivityComponentFactory: @ChildFactory () -> RetainedActivityComponent
}

private class RetainedActivityComponentHolder : ViewModel() {
    var component: RetainedActivityComponent? = null
    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RetainedActivityComponentHolder() as T
    }
}
