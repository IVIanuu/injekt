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
            
            @Effect
            annotation class StoreBinding {
                companion object { 
                    @Binding(MyComponent::class)
                    fun <T : Store<S, A>, S, A> storeFromProvider(provider: (Scope) -> @ForEffect T): BoundStore<S, A> =
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
    fun testStore4() = codegen(
        """
            interface Scope
            
            interface Store<S, A> {
                val state: S
                val dispatch: A
            }
            
            @Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class State
            
            @Binding
            operator fun <S : Any?> Store<S, *>.component1(): @State S = state
            
            /*@Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class Dispatch
            
            @Binding
            operator fun <A : @Dispatch Any> Store<*, A>.component2(): A = dispatch*/

            class MyState(val store: Store<MyState, MyAction>)
            class MyAction(val store: Store<MyState, MyAction>)
            
            @Binding(MyComponent::class)
            fun myStore(): Store<MyState, MyAction> {
                return object : Store<MyState, MyAction> {
                    override val state: MyState = MyState(this)
                    override val dispatch: MyAction = MyAction(this)
                }
            }
            
            @FunBinding
            fun MyPage(state: @State MyState): Pair<Any, Any> {
                return state.store to state.store
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
    fun testStore5() = multiCodegen(
        listOf(
            source(
                """
                    interface Scope

                    interface Store<S, A> {
                        val state: S
                        val dispatch: A
                    }
                    
                    @Qualifier
                    @Target(AnnotationTarget.TYPE)
                    annotation class State
                    
                    @Binding
                    operator fun <S> Store<S, *>.component1(): @State S = state
        
                    @Effect
                    annotation class StateEffect {
                        companion object {
                            @SetElements
                            fun <T : suspend (S) -> Unit, S> intoSet(block: T) = setOf<suspend (S) -> Unit>(block)
                        }
                    }
 
                    @Decorator
                    fun <T : Store<S, A>, S, A> stateEffectStoreDecorator(
                        stateEffects: Set<(S) -> Unit>?,
                        factory: suspend () -> T
                    ): suspend () -> T = factory
                """
            )
        ),
        listOf(
            source(
                """
                    class MyState(val store: Store<MyState, MyAction>)
                    class MyAction(val store: Store<MyState, MyAction>)
                    
                    @Binding
                    fun myStore(): Store<MyState, MyAction> {
                        return object : Store<MyState, MyAction> {
                            override val state: MyState = MyState(this)
                            override val dispatch: MyAction = MyAction(this)
                        }
                    }
                    
                    @FunBinding
                    fun MyPage(state: @State MyState): Pair<Any, Any> {
                        return state.store to state.store
                    }
                    
                    class MyState2(val store: Store<MyState2, MyAction2>)
                    class MyAction2(val store: Store<MyState2, MyAction2>)
                    
                    @StateEffect
                    @FunBinding
                    suspend fun MyStateEffect2(@FunApi myState2: MyState2) {
                    
                    }
                    
                    @Binding
                    suspend fun myStore2(): Store<MyState2, MyAction2> {
                        return object : Store<MyState2, MyAction2> {
                            override val state: MyState2 = MyState2(this)
                            override val dispatch: MyAction2 = MyAction2(this)
                        }
                    }
                    
                    @FunBinding
                    suspend fun MyPage2(state: @State MyState2): Pair<Any, Any> {
                        return state.store to state.store
                    }
                    """
                    )
        ),
        listOf(
            source(
                """
                    @Component
                    abstract class MyComponent {
                        abstract val myPage: MyPage
                        abstract val myPage2: MyPage2
                    }
                    
                    fun invoke(): Pair<Any, Any> {
                        return component<MyComponent>().myPage()
                    } 
                """
            )
        )
    ) {
    }

    @Test
    fun test() = codegen(
        """
            interface Flow<T>
            
            interface MutableFlow<T>
            
            interface Pref<T> {
                val flow: Flow<T>
            }
            
            @Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class UiState
            
            @Qualifier
            @Target(AnnotationTarget.TYPE)
            annotation class Initial

            @Binding
            fun <T> MutableFlow<T>.latest(): @UiState T = error("")
            
            @Binding
            fun <T> Flow<T>.latest(initial: @Initial T): @UiState T = error("")
            
            @Binding
            fun <T> Pref<T>.flow(): Flow<T> = error("")
            
            @Effect
            annotation class PrefBinding {
                companion object {
                    @Binding
                    fun <T> pref(initial: () -> @Initial T): Pref<T> = error("")
                
                    @Binding
                    inline fun <reified T> initial(): @Initial T = T::class.java.newInstance()
                }
            }

            @PrefBinding
            class MyState

            @Component
            abstract class MyComponent {
                abstract val state: @UiState MyState
            }
        """
    )
}
