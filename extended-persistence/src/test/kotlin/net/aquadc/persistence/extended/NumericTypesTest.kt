package net.aquadc.persistence.extended

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class NumericTypesTest {

    @Test fun `int array`() {
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(intArrayOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(arrayOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.load(listOf(1, 2, 3)))
        assertArrayEquals(intArrayOf(1, 2, 3), intCollection.store(intArrayOf(1, 2, 3)) as IntArray)
    }

    @Test fun `long array`() {
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(longArrayOf(1, 2, 3)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(arrayOf(1L, 2L, 3L)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.load(listOf(1L, 2L, 3L)))
        assertArrayEquals(longArrayOf(1, 2, 3), longCollection.store(longArrayOf(1, 2, 3)) as LongArray)
    }

    @Test fun `float array`() {
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(floatArrayOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(arrayOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.load(listOf(1f, 2f, 3f)), 0f)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), floatCollection.store(floatArrayOf(1f, 2f, 3f)) as FloatArray, 0f)
    }

    @Test fun `double array`() {
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(doubleArrayOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(arrayOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.load(listOf(1.0, 2.0, 3.0)), 0.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0), doubleCollection.store(doubleArrayOf(1.0, 2.0, 3.0)) as DoubleArray, 0.0)
    }

}
