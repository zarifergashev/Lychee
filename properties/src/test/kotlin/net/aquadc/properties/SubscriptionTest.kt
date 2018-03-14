package net.aquadc.properties

import net.aquadc.properties.internal.copyOfWithout
import net.aquadc.properties.internal.with
import net.aquadc.properties.internal.withoutNulls
import org.junit.Assert.*
import org.junit.Test


class SubscriptionTest {

    @Test fun concSubscribe() = subscribe(true)
    @Test fun unsSubscribe() = subscribe(false)

    private fun subscribe(conc: Boolean) {
        val prop = mutablePropertyOf(false, conc)

        var l2Called = false

        val l2 = { old: Boolean, new: Boolean ->
            l2Called = true
        }

        val l1 = { old: Boolean, new: Boolean ->
            prop.addChangeListener(l2)
            Unit
        }

        prop.addChangeListener(l1)

        prop.value = false

        assertTrue(l2Called)
    }

    @Test fun concUnsubscribe() = unsubscribe(true)
    @Test fun unsUnsubscribe() = unsubscribe(false)

    private fun unsubscribe(conc: Boolean) {
        val prop = mutablePropertyOf(false, conc)

        var l1Called = 0
        var l2Called = 0
        var l3Called = 0

        val l1 = { old: Boolean, new: Boolean ->
            l1Called++
            Unit
        }

        val l3 = { old: Boolean, new: Boolean ->
            l3Called++
            Unit
        }

        val l2 = object : ChangeListener<Boolean> {
            override fun invoke(old: Boolean, new: Boolean) {
                l2Called++
                prop.removeChangeListener(this)
                prop.removeChangeListener(l3)
            }
        }

        prop.addChangeListener(l1)
        prop.addChangeListener(l2)
        prop.addChangeListener(l3)

        prop.value = false // l1Called++, l2Called++, remove l2 & l3
        prop.value = false // l1Called++

        assertEquals(2, l1Called)
        assertEquals(1, l2Called)
        assertEquals(0, l3Called)
    }

    @Test fun concUpdateInside() = updateInside(true)
    @Test fun unsUpdateInside() = updateInside(false)

    private fun updateInside(conc: Boolean) {
        val prop = mutablePropertyOf(0, conc)

        var v1 = 0
        var v2 = 0

        val l2 = { old: Int, new: Int ->
            v2 = new
        }

        val l1 = { old: Int, new: Int ->
            v1 = new
            if (new != 10) prop.value = 10
        }

        prop.addChangeListener(l1)
        prop.addChangeListener(l2)

        prop.value = 1

        assertEquals(10, v1)
        assertEquals(10, v2)
    }

    // todo: check conc updater under contention

    @Test fun with() {
        assertArrayEquals(arrayOf(1), emptyArray<Int>().with(1))
        assertArrayEquals(arrayOf(1, 2), arrayOf(1).with(2))
    }

    @Test fun copyWithout() {
        assertArrayEquals(arrayOf(1, 2), arrayOf(0, 1, 2).copyOfWithout(0))
        assertArrayEquals(arrayOf(0, 2), arrayOf(0, 1, 2).copyOfWithout(1))
        assertArrayEquals(arrayOf(0, 1), arrayOf(0, 1, 2).copyOfWithout(2))
    }

    @Test fun withoutNulls() {
        assertArrayEquals(emptyArray(), arrayOf<Any?>(null).withoutNulls())
        assertArrayEquals(emptyArray(), arrayOf<Any?>(null, null).withoutNulls())
        assertArrayEquals(arrayOf(0, 1, 2), arrayOf<Any?>(0, null, 1, null, 2).withoutNulls())
        assertArrayEquals(arrayOf(0, 2), arrayOf<Any?>(0, null, null, null, 2).withoutNulls())
        assertArrayEquals(arrayOf(0, 1), arrayOf<Any?>(0, 1, null).withoutNulls())
        assertArrayEquals(arrayOf(0, 1), arrayOf<Any?>(0, null, null, 1, null).withoutNulls())
        assertArrayEquals(arrayOf(3, 4), arrayOf<Any?>(null, 3, 4).withoutNulls())
        assertArrayEquals(arrayOf(3, 4), arrayOf<Any?>(null, null, 3, null, 4).withoutNulls())
    }

}
