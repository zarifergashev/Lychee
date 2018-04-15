package net.aquadc.properties.internal


internal fun Array<*>.with(lastElement: Any?): Array<Any?> {
    val size = size
    val new = arrayOfNulls<Any>(size + 1)
    System.arraycopy(this, 0, new, 0, size)
    new[size] = lastElement
    return new
}

@JvmName("copyOfWithoutNn")
internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>): Array<Any?> =
        copyOfWithout(idx, canonicalEmptyArray as Array<Any?>?) as Array<Any?>

internal fun Array<*>.copyOfWithout(idx: Int, canonicalEmptyArray: Array<Any?>?): Array<Any?>? {
    val oldSize = size
    if (idx == 0 && oldSize == 1) return canonicalEmptyArray

    val new = arrayOfNulls<Any>(oldSize-1)
    System.arraycopy(this, 0, new, 0, idx)
    System.arraycopy(this, idx+1, new, idx, oldSize-idx-1)
    return new
}

@JvmName("withoutNullsNn")
internal fun <T : Any> Array<T?>.withoutNulls(canonicalEmptyArray: Array<T>): Array<T> =
        withoutNulls<T>(canonicalEmptyArray as Array<T>?) as Array<T>

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Array<out T?>.withoutNulls(canonicalEmptyArray: Array<T>?): Array<T>? {
    val nulls = count { it == null }
    if (nulls == 0) return this as Array<T> // it safe since there are no actual nulls

    val newSize = size - nulls
    if (newSize == 0) return canonicalEmptyArray

    val newArray = arrayOfNulls<Any>(size - nulls)
    var destPos = 0
    forEach { if (it != null) newArray[destPos++] = it }

    return newArray as Array<T> // not actually T[], but it's OK
}
