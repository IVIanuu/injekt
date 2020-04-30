package com.ivianuu.injekt.sample

import android.app.Application
import com.ivianuu.injekt.Factory
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.childFactory
import com.ivianuu.injekt.createImplementation
import com.ivianuu.injekt.internal.InjektAst
import com.ivianuu.injekt.internal.InstanceProvider
import com.ivianuu.injekt.internal.injektIntrinsic

class App : Application() {
    val component by lazy { createAppComponent(this) }
}

interface AppComponent {
    val repo: Repo
    val activityComponentFactory: (MainActivity) -> ActivityComponent
}

@Factory
fun createAppComponent(app: App): AppComponent = createImplementation {
    childFactory(::activityComponentFactory)
}

private class createAppComponentModuleImpl {
    @InjektAst.Module
    interface Descriptor {
        @InjektAst.ChildFactory
        fun childFactory_0(): activityComponentFactoryModule
    }
}

private class createAppComponentImpl : AppComponent {
    private val repoProvider: Provider<Repo> = injektIntrinsic()
    override val repo: Repo get() = repoProvider()

    override val activityComponentFactory: (MainActivity) -> ActivityComponent
        get() = activityComponentFactoryImpl(this)

    private class activityComponentFactoryImpl(
        private val appComponentImpl: createAppComponentImpl
    ) : (MainActivity) -> ActivityComponent {
        override fun invoke(p1: MainActivity): ActivityComponent {
            return activityComponentImpl(appComponentImpl)
        }
    }

    private class activityComponentImpl(
        private val appComponentImpl: createAppComponentImpl
    ) : ActivityComponent {
        override val injector: MembersInjector<MainActivity>
            get() = injectorProvider()
        private val viewModelProvider: Provider<HomeViewModel> = injektIntrinsic()
        private val injectorProvider: Provider<MembersInjector<MainActivity>> = InstanceProvider(
            MainActivity.MainActivityMembersInjector(viewModelProvider)
        )
    }
}
