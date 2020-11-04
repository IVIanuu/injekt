package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.source
import junit.framework.Assert.assertSame
import org.junit.Test

class EssentialsTest {

    @Test
    fun testStore() = codegen(
        """
            interface Scope
            
            interface Store<S, A> {
                val state: S
                val dispatch: A
            }
            
            interface StoreState
            interface StoreAction
            
            @Binding
            val <S : StoreState> Store<S, *>.storeState: S get() = state
            @Binding
            val <A : StoreAction> Store<*, A>.storeDispatch: A get() = dispatch
            
            @Binding(MyComponent::class)
            fun <S, A> storeFromProvider(provider: (Scope) -> Store<S, A>): Store<S, A> = provider(object : Scope {})
            
            class MyState(val store: Store<MyState, MyAction>) : StoreState
            class MyAction(val store: Store<MyState, MyAction>) : StoreAction
            
            @Binding
            fun myStore(): (Scope) -> Store<MyState, MyAction> = {
                object : Store<MyState, MyAction> {
                    override val state: MyState = MyState(this)
                    override val dispatch: MyAction = MyAction(this)
                }
            }
            
            @FunBinding
            fun MyPage(state: MyState, dispatch: MyAction): Pair<Any, Any> {
                return state.store to dispatch.store
            }
            
            @Component
            abstract class MyComponent {
                abstract val myPage: MyPage
            }
            
            fun invoke(): Pair<Any, Any> {
                return component<MyComponent>().myPage()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testStore2() = codegen(
        """
            interface Scope
            
            interface Store<S, A> {
                val state: S
                val dispatch: A
            }
            
            interface StoreState
            interface StoreAction
            
            @Binding
            val <S : StoreState> ComposableStore<S, *>.storeState: S get() = state
            @Binding
            val <A : StoreAction> ComposableStore<*, A>.storeDispatch: A get() = dispatch
            
            typealias ComposableStore<S, A> = Store<S, A>
            
            @Binding(MyComponent::class)
            fun <S, A> storeFromProvider(provider: (Scope) -> Store<S, A>): ComposableStore<S, A> =
                provider(object : Scope {})
            
            class MyState(val store: Store<MyState, MyAction>) : StoreState
            class MyAction(val store: Store<MyState, MyAction>) : StoreAction
            
            @Binding
            fun Scope.myStore(): Store<MyState, MyAction> {
                return object : Store<MyState, MyAction> {
                    override val state: MyState = MyState(this)
                    override val dispatch: MyAction = MyAction(this)
                }
            }
            
            @FunBinding
            fun MyPage(state: MyState, dispatch: MyAction): Pair<Any, Any> {
                return state.store to dispatch.store
            }
            
            @Component
            abstract class MyComponent {
                abstract val myPage: MyPage
            }
            
            fun invoke(): Pair<Any, Any> {
                return component<MyComponent>().myPage()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testStore3() = codegen(
        """
            interface Scope
            
            interface Store<S, A> {
                val state: S
                val dispatch: A
            }
            
            internal typealias BoundStore<S, A> = Store<S, A>
            
            @BindingAdapter
            annotation class StoreBinding {
                companion object { 
                    @Binding(MyComponent::class)
                    fun <T : Store<S, A>, S, A> storeFromProvider(provider: (Scope) -> T): BoundStore<S, A> =
                        provider(object : Scope {})
                
                    @Binding
                    fun <T : Store<S, A>, S, A> storeState(instance: BoundStore<S, A>): S = instance.state
                    
                    @Binding
                    fun <T : Store<S, A>, S, A> storeDispatch(instance: BoundStore<S, A>): A = instance.dispatch
                }
            }
            
            class MyState(val store: Store<MyState, MyAction>)
            class MyAction(val store: Store<MyState, MyAction>)
            
            @StoreBinding
            fun Scope.myStore(): Store<MyState, MyAction> {
                return object : Store<MyState, MyAction> {
                    override val state: MyState = MyState(this)
                    override val dispatch: MyAction = MyAction(this)
                }
            }
            
            @FunBinding
            fun MyPage(state: MyState, dispatch: MyAction): Pair<Any, Any> {
                return state.store to dispatch.store
            }
            
            @Component
            abstract class MyComponent {
                abstract val myPage: MyPage
            }
            
            fun invoke(): Pair<Any, Any> {
                return component<MyComponent>().myPage()
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Any, Any>>()
        assertSame(a, b)
    }

    @Test
    fun testBoundedGenericWithAlias() = multiCodegen(
        listOf(
            source(
                """
                    interface ViewModel
            
                    interface ViewModelStore
            
                    @Binding
                    class DefaultViewModelStore : ViewModelStore
                """
            )
        ),
        listOf(
            source(
                """
                    @BindingAdapter
                    annotation class ViewModelBinding {
                        companion object {
                            @Binding
                            fun <T : ViewModel> bind(
                                getViewModel: getViewModel<T, DefaultViewModelStore>
                            ): T = getViewModel()
                        }
                    }
                    
                    @FunBinding
                    inline fun <reified VM : ViewModel, VMSO : ViewModelStore> getViewModel(
                        store: VMSO,
                        noinline provider: () -> VM
                    ) = provider()
                    """
            )
        ),
        listOf(
            source(
                """
                    @ViewModelBinding
                    class MyViewModel : ViewModel
                    
                    @FunBinding
                    fun WithMyViewModel(
                        viewModelFactory: () -> MyViewModel,
                        @FunApi children: (MyViewModel) -> Unit 
                    ) {
                    }
                    
                    @Component
                    abstract class MyComponent {
                        abstract val withMyViewModel: WithMyViewModel
                    }
                """
            )
        )
    )

}