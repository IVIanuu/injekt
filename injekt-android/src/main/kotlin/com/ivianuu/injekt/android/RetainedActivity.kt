package com.ivianuu.injekt.android

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.plus

val <T : ComponentActivity> T.retainedActivityComponent: Component
    get() {
        val holder = ViewModelProvider(this, RetainedActivityComponentHolder.Factory)
            .get(RetainedActivityComponentHolder::class.java)

        if (holder.component == null) {
            holder.component = application.applicationComponent
                .plus<RetainedActivityScoped>()
        }

        return holder.component!!
    }

private class RetainedActivityComponentHolder : ViewModel() {
    var component: Component? = null

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
            RetainedActivityComponentHolder() as T
    }
}

@Scope
annotation class RetainedActivityScoped
