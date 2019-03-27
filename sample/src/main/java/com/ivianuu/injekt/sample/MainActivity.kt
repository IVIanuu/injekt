package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.PerActivity
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.annotations.Single
import com.ivianuu.injekt.inject

class MainActivity : AppCompatActivity(), InjektTrait {

    override val component by lazy { activityComponent() }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        d { "Injected app dependency $appDependency" }
        d { "Injected main activity dependency $mainActivityDependency" }

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, ParentFragment())
                .commit()
        }
    }

}

@Single(PerActivity::class)
class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)