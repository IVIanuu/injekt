package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.Inject
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.createImpl
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.scope

class MainActivity : AppCompatActivity() {
    val activityComponent by lazy {
        (application as App).appComponent
            .mainActivityComponentFactory(this)
            .also { it.injectMainActivity(this) }
    }
    @Inject
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }
}

interface MainActivityComponent {
    val injectMainActivity: @MembersInjector (MainActivity) -> Unit
}

@Scope
annotation class ActivityScoped

@ActivityScoped
class MainViewModel(private val repo: Repo)

@ChildFactory
fun createMainActivityComponent(mainActivity: MainActivity): MainActivityComponent {
    scope<ActivityScoped>()
    instance(mainActivity)
    return createImpl()
}
