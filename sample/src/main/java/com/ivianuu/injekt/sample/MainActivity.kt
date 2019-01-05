package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.multibinding.injectMap
import com.ivianuu.injekt.multibinding.intoMap
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
        super.onCreate(savedInstanceState)

        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }
        d { "All dependencies $dependencies" }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(ParentFragment(), "my_fragment")
                .commit()
        }
    }

}

class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
) : Dependency

val mainActivityModule = module {
    single { MainActivityDependency(get(), get()) } intoMap (DEPS to MainActivityDependency::class)
}