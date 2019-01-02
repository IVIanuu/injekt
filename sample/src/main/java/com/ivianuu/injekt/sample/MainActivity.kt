package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.ComponentHolder
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.inject

class MainActivity : AppCompatActivity(), ComponentHolder {

    override val component by lazy { activityComponent(this) }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDependency
        mainActivityDependency

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(MyFragment(), "my_fragment")
                .commit()
        }
    }

}

@Single
class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)