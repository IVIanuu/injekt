package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.ChildFactory
import com.ivianuu.injekt.MembersInjector
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Transient
import com.ivianuu.injekt.createImplementation
import com.ivianuu.injekt.inject
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.internal.InjektAst

class MainActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by inject()
    private lateinit var _viewModel: HomeViewModel

    private var injected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as App).component
            .activityComponentFactory(this)
            .injector
            .inject(this)
    }

    private fun inject(viewModel: HomeViewModel) {
        injected = true
        _viewModel = viewModel
    }

    class MainActivityMembersInjector(
        private val viewModelProvider: Provider<HomeViewModel>
    ) : MembersInjector<MainActivity> {
        override fun inject(instance: MainActivity) {
            inject(instance, viewModelProvider())
        }

        companion object {
            fun inject(instance: MainActivity, viewModel: HomeViewModel) {
                instance.inject(viewModel)
            }
        }
    }
}

interface ActivityComponent {
    val injector: MembersInjector<MainActivity>
}

@ChildFactory
fun activityComponentFactory(mainActivity: MainActivity): ActivityComponent = createImplementation {
    instance(mainActivity)
}

@InjektAst.ChildFactoryDescriptor
interface activityComponentFactoryDescriptor {
    @InjektAst.ChildFactoryType
    fun type(): (MainActivity) -> ActivityComponent
    @InjektAst.ChildFactoryModule
    fun module(): activityComponentFactoryModule
}

class activityComponentFactoryModule(mainActivity: MainActivity) {
    val instance_0: MainActivity

    init {
        instance_0 = mainActivity
    }

    @InjektAst.Module
    interface Descriptor {
        @InjektAst.Binding
        fun provide_0(): MainActivity
    }
}

@Transient
class HomeViewModel(private val repo: Repo)
