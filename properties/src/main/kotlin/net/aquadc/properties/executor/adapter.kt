package net.aquadc.properties.executor

import net.aquadc.properties.ChangeListener
import net.aquadc.properties.diff.DiffChangeListener
import java.util.concurrent.Executor

internal class MapWhenChanged<in T, U>(
        private val mapOn: Worker,
        private val map: (T) -> U,
        private val consumer: (U) -> Unit
) : ChangeListener<T> {

    override fun invoke(old: T, new: T) {
        mapOn.map(new, map, consumer)
    }

}

internal class ConsumeOn<in T>(
        private val consumeOn: Executor,
        private val consumer: (T) -> Unit
) : (T) -> Unit, Runnable {

    @Volatile @Suppress("UNCHECKED_CAST")
    private var value: T = this as T
    // 'this' means 'unset', should not pass instance of this class to its `invoke` 😅

    override fun invoke(value: T) {
        check(value !== this)
        this.value = value
        consumeOn.execute(this)
    }

    override fun run() {
        val value = value
        check(value !== this)
        consumer(value)
    }

}

/**
 * When invoked, calls [actual] on [executor].
 */
internal class ConfinedChangeListener<in T>(
        private val executor: Executor,
        @JvmField internal val actual: ChangeListener<T>
) : ChangeListener<T> {

    @Volatile @JvmField
    internal var canceled = false

    override fun invoke(old: T, new: T) {
        executor.execute {
            if (!canceled) {
                actual(old, new)
            }
        }
    }

}

/**
 * When invoked, calls [actual] on [executor].
 */
internal class ConfinedDiffChangeListener<in T, in D>(
        private val executor: Executor,
        @JvmField internal val actual: DiffChangeListener<T, D>
) : DiffChangeListener<T, D> {

    @Volatile @JvmField
    internal var canceled = false

    override fun invoke(old: T, new: T, diff: D) {
        executor.execute {
            if (!canceled) {
                actual(old, new, diff)
            }
        }
    }

}

/**
 * Executes given command in-place.
 */
object UnconfinedExecutor : Executor {

    override fun execute(command: Runnable) =
            command.run()

}
