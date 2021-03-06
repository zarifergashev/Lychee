package net.aquadc.persistence.tokens

import androidx.annotation.RestrictTo
import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.contains
import net.aquadc.collections.plus
import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.fromBase64
import net.aquadc.persistence.hasFraction
import net.aquadc.persistence.toBase64
import java.io.Closeable

/**
 * Token type in abstract serialization protocol.
 */
enum class Token(internal val delta: Int) {
    Null(0) {
        override fun coerce(value: Any?): Any? =
                if (value == null) null else throw IllegalArgumentException("expected $Null but was $value")
    },
    Bool(0) {
        override fun coerce(value: Any?): Any? =
                if (value is Boolean) value else throw IllegalArgumentException("expected $Bool but was $value")
    },
    I32(0), I64(0),
    F32(0), F64(0),
    Str(0) {
        override fun coerce(value: Any?): Any? = when (value) {
            is Boolean, is Number -> value.toString()
            is CharSequence -> value
            is ByteArray -> toBase64(value)
            else -> throw IllegalArgumentException("value $value cannot be coerced to $Str")
        }
    },
    Blob(0) {
        override fun coerce(value: Any?): Any? = when (value) {
            is CharSequence -> fromBase64(value.toString())
            is ByteArray -> value
            else -> throw IllegalArgumentException("value $value cannot be coerced to $this")
        }
    },
    BeginSequence(+1), EndSequence(-1),
    BeginDictionary(+1), EndDictionary(-1),
    ;

    open fun coerce(value: Any?): Any? = when (this) {
        in Numbers -> {
            when (value) {
                is Byte -> coerceToNumber(value.toLong())
                is Short -> coerceToNumber(value.toLong())
                is Int -> coerceToNumber(value.toLong())
                is Long -> coerceToNumber(value)
                is Float -> when (this) {
                    F32 -> value
                    F64 -> value.toDouble()
                    else -> throw IllegalArgumentException("value $value cannot be coerced to $this")
                }
                is Double -> when (this) {
                    F32 -> value.toFloat()
                    F64 -> value
                    else -> throw IllegalArgumentException("value $value cannot be coerced to $this")
                }
                is CharSequence -> value.toString().let { value -> // to<Number> are extensions on String
                    if (hasFraction(value)) when (this) {
                        F32 -> value.toFloat()
                        F64 -> value.toDouble()
                        else -> throw IllegalArgumentException("value $value cannot be coerced to $this") // never coerce fractionals to ints
                    } else coerceToNumber(value.toLong())
                }
                else -> throw IllegalArgumentException("value $value cannot be coerced to $this")
            }
        }
        in ControlTokens -> {
            if (value == this) value
            else throw IllegalArgumentException("value $value cannot be coerced to $this")
        }
        else -> throw AssertionError()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun coerceToNumber(value: Long): Any = when {
        this == I32 && value in Int.MIN_VALUE..Int.MAX_VALUE -> value.toInt()
        this == I64 -> value
        this == F32 -> value.toFloat()
        this == F64 -> value.toDouble()
        else -> throw IllegalArgumentException("value $value cannot be coerced to $this")
    }

    @Suppress("INAPPLICABLE_JVM_FIELD")
    companion object {
        // wannabe const but https://youtrack.jetbrains.com/issue/KT-25915
        @JvmField val Integers: InlineEnumSet<Token> = I32 + I64
        @JvmField val Floats: InlineEnumSet<Token> = F32 + F64
        @JvmField val Numbers: InlineEnumSet<Token> = Integers + Floats
        @JvmField val ControlTokens: InlineEnumSet<Token> = BeginSequence + EndSequence + BeginDictionary + EndDictionary

        fun ofValue(value: Any?): Token? = when (value) {
            null -> Null
            is Boolean -> Bool
            is Int -> I32
            is Long -> I64
            is Float -> F32
            is Double -> F64
            is CharSequence -> Str
            is ByteArray -> Blob
            is Token -> if (value in ControlTokens) value else null
            else -> null
        }
    }
}

fun Token?.coerce(value: Any?): Any? =
        if (this == null) value else coerce(value)

/**
 * A consumable Iterator/Stream/Sequence of tokens.
 * Must consist of a single value which could be a primitive, a sequence, or a dictionary.
 * Dictionaries are effectively collections of pairs, e. g. have even number of values.
 * Only primitive names (dictionary values at odd positions) are supported by most of operations.
 */
interface TokenStream : CloseableIterator<Any?> {

    /**
     * Nesting information.
     */
    val path: List<Any? /* = name: Primitive | index: Index */>
    // where Primitive is any number, string, or blob, and Index is a special int box

    /**
     * Retrieve type of next token without consuming it.
     */
    fun peek(): Token

    /**
     * Consume and return the next token.
     * @param coerceTo expected token type, or null to retrieve any
     * @return null|Boolean|Byte|Short|Int|Long|Float|Double|CharSequence|ByteArray|Token.(Begin|End)(Sequence|Dictionary)
     *         **must** conform the type specified by [coerceTo]
     */
    fun poll(coerceTo: Token? = null): Any?

    /**
     * Consume and return the next token.
     * @see poll
     */
    override fun next(): Any? =
            poll(null)

    /**
     * Skip and consume the next value.
     * A value is either a 'primitive' or a whole bracket sequence
     * from the [Token.BeginSequence] or [Token.BeginDictionary] and until
     * [Token.EndSequence] or [Token.BeginSequence].
     */
    fun skipValue() {
        var depth = 0
        do {
            val token = poll()
            if (token is Token) depth += token.delta
        } while (depth > 0) // `while (depth != 0)` is a cause of Gson JsonReader bug: https://github.com/google/gson/issues/605
    }

}

class Index(value: Int) {
    var value: Int = value; @JvmSynthetic internal set
    override fun hashCode(): Int = value
    override fun equals(other: Any?): Boolean = other is Index && value == other.value
    override fun toString(): String = "#$value"

    companion object {
        @JvmField val First = Index(0)
        @JvmField val Second = Index(1)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class TokenPath : ArrayList<Any?>() {
    fun beginArray() { add(Index(0)) }
    fun endArray() { removeAt(lastIndex) as Index; afterValue() }
    fun beginObject() { add(null) } // inside object; name was not read yet
    fun endObject() { check(removeAt(lastIndex) !is Index); afterValue() }
    fun onName(name: Any?) { this[lastIndex] = name }
    fun afterValue() { (lastOrNull() as? Index)?.let { it.value++ } } // increments index if there was an array

    override fun toString(): String = buildString {
        append('$')
        this@TokenPath.forEach {
            append('[')
            when (it) {
                is Index -> append('#').append(it.value)

                is CharSequence -> append('\'') // of course I know the fast way to append while replacing occurrences
                        .append(it.toString().replace("\\", "\\\\").replace("'", "\\'")) // of several characters.
                        .append('\'') // But there's no reason to do so in non-critical toString

                else -> append(it) // make difference between strings (e. g. `$['null']`) and other types (`$[null]`)
            }
            append(']')
        }
    }
}
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NameTracingTokenPath : TokenPath() {
    val expectingName = ArrayList<Boolean?>()

    fun skip() {
        afterValue(null)
    }
    fun afterToken(value: Any?) {
        when (value) {
            Token.BeginSequence -> {
                check(expectingName.lastOrNull() != true) { "names of type '${Token.BeginSequence}' are not supported" }
                beginArray()
                expectingName.add(null)
            }
            Token.EndSequence -> {
                endArray()
                check(expectingName.removeAt(expectingName.lastIndex) == null)
                flipExpectName()
            }
            Token.BeginDictionary -> {
                check(expectingName.lastOrNull() != true) { "names of type '${Token.BeginDictionary}' are not supported" }
                beginObject()
                expectingName.add(true)
            }
            Token.EndDictionary -> {
                endObject()
                check(expectingName.removeAt(expectingName.lastIndex) == true) {
                    "dangling name. Expected a value but was '${Token.EndDictionary}'"
                }
                flipExpectName()
            }
            else -> {
                afterValue(value)
            }
        }
    }

    private fun afterValue(value: Any?) {
        if (expectingName.isNotEmpty()) {
            val en = expectingName.last()
            if (en == null) {
                afterValue()
            } else {
                if (en) onName(value) else afterValue()
                expectingName[expectingName.lastIndex] = !en
            }
        } // else we're at the root element, nothing to do here
    }

    private fun flipExpectName() {
        if (expectingName.isNotEmpty()) {
            val li = expectingName.lastIndex
            expectingName[li]?.let { expectingName[li] = !it }
        }
    }

    override fun clear() {
        super.clear()
        expectingName.clear()
    }

}
