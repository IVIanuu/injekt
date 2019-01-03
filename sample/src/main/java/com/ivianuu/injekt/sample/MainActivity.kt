package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.codegen.Module
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.inject

class MainActivity : AppCompatActivity(), InjektTrait {

    override val component by lazy {
        activityComponent(this) {
            modules(mainActivityModule)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDependency
        mainActivityDependency

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(ParentFragment(), "my_fragment")
                .commit()
        }
    }

}

@Module
private annotation class MainActivityModule

@Single
@MainActivityModule
class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)