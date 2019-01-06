package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.multibinding.injectMap
import kotlin.reflect.KClass

const val ACTIVITY_SCOPE = "activity_scope"

class MainActivity : AppCompatActivity(), InjektTrait {

    override val component by lazy {
        activityComponent(this) {
            modules(mainActivityModule)
        }
    }

    private val aDep by inject<ADep>()

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
    single {
        MainActivityDependency(
            get(),
            get()
        )
    } bindIntoMap (DEPS to MainActivityDependency::class)
}