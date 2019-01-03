package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.annotations.Module
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.common.instanceModule
import com.ivianuu.injekt.get
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.module
import com.ivianuu.injekt.single

class MainActivity : AppCompatActivity(), AppComponentTrait {

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        component.scopedModules(this, "MAIN_ACTIVITY",
            mainActivityModule, instanceModule(this))

        if (savedInstanceState == null) {
            component.scopedModules("MAIN_ACTIVITY_RETAINED", retainedModule)
        }

        super.onCreate(savedInstanceState)

        appDependency
        mainActivityDependency

        get<String>("retained")

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(ParentFragment(), "my_fragment")
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            component.removeScope("MAIN_ACTIVITY_RETAINED")
        }
    }

}

val retainedModule = module {
    single("retained") { "Retained object" }
}

@Module private annotation class MainActivityModule

@Single @MainActivityModule
class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)