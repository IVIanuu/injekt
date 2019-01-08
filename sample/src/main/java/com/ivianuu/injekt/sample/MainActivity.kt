package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.APPLICATION_SCOPE
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.module
import com.ivianuu.injekt.modules
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.multibinding.injectMap
import com.ivianuu.injekt.single
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), InjektTrait {

    override val component by lazy {
        activityComponent(this) {
            modules(mainActivityModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    private val dependencies by injectMap<KClass<out Dependency>, Dependency>(DEPS)

    override fun onCreate(savedInstanceState: Bundle?) {
        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "All dependencies $dependencies" }

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ParentFragment())
                .commit()
        }

        get<String>("some_string")
    }

}

class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
) : Dependency

val mainActivityModule = module {
    single {
        MainActivityDependency(
            get(),
            get()
        )
    } bindIntoMap (DEPS to MainActivityDependency::class)

    factory("some_string", APPLICATION_SCOPE) { "some_string" }
}