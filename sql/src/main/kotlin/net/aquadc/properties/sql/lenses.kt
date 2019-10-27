@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable


/**
 * String concatenation factory to build names for nested lenses.
 */
abstract class NamingConvention {

    abstract fun concatNames(outer: String, nested: String): String

    @JvmName("0") @Deprecated("not perfectly correct and does not look useful")
    inline operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U> NamedLens<TS, TR, out T?, *>.div(
            nested: NamedLens<US, T, U, *>
    ): NamedLens<TS, TR, U?, *> =
            Telescope(concatNames(this.name, nested.name), this, nested)

    @JvmName("1") @Deprecated("not perfectly correct and does not look useful")
    inline operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : Struct<US>, U> NamedLens<TS, TR, T, *>.div(
            nested: NamedLens<US, T, U, *>
    ): NamedLens<TS, TR, U, *> =
            Telescope(concatNames(this.name, nested.name), this, nested)

}


@JvmName("structLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<Struct<US>, US>,
        V, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, V, VD> =
        Telescope(null, nested.exactType, this, nested, false)

@JvmName("partialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.exactType), this, nested, true)

@JvmName("partialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, V?, VD> =
        Telescope(null, nested.exactType, this, nested, true)

@JvmName("nullablePartialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.Partial<UR, US>>,
        V : Any, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.exactType), this, nested, true)

@JvmName("nullablePartialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.Partial<UR, US>>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, V?, VD> =
        Telescope(null, nested.exactType, this, nested, true)


@Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
internal fun <SCH : Schema<SCH>, STR : PartialStruct<SCH>> NamingConvention?.concatErased(
        dis: StoredLens<SCH, *, *>, that: StoredLens<*, *, *>
): Lens<SCH, STR, *, *> =
        Telescope<SCH, STR, Schema<*>, PartialStruct<Schema<*>>, Any?, DataType<*>>(
                (if (this !== null && dis is Named && that is Named) concatNames(dis.name, that.name) else null),
                dis as Lens<SCH, STR, out PartialStruct<Schema<*>>?, *>, that as Lens<Schema<*>, PartialStruct<Schema<*>>, out Any?, *>
        )

/**
 * Generates lenses whose names are concatenated with snake_case
 */
object SnakeCase : NamingConvention() {

    override fun concatNames(outer: String, nested: String): String = buildString(outer.length + 1 + nested.length) {
        append(outer).append('_').append(nested)
    }

}

/**
 * Generates lenses whose names are concatenated with camelCase
 */
object CamelCase : NamingConvention() {

    override fun concatNames(outer: String, nested: String): String = buildString(outer.length + nested.length) {
        append(outer)
        if (nested.isNotEmpty()) {
            // Note: this capitalizer is intended for use with identifiers which are mostly ASCII.
            // It will also work for most of non-ASCII scripts. However, it won't capitalize letters of
            // some rare scripts, see https://stackoverflow.com/a/57908029/3050249 for fully-featured capitalizer
            append(nested[0].toUpperCase())
            append(nested, 1, nested.length)
        }
    }

}

@PublishedApi internal class
Telescope<TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U, UD : DataType<U>>(
        name: String?,
        exactType: UD,
        private val outer: StoredLens<TS, out T?, *>,
        private val nested: StoredLens<US, out U, *>,
        private val outerMayReturnNull: Boolean? // todo break 'dis shit with a test
) : BaseLens<TS, TR, U, UD>(name, exactType) {

    @PublishedApi internal constructor(
            name: String?,
            exactType: UD,
            outer: Lens<TS, TR, out T?, *>,
            nested: Lens<US, T, out U, *>,
            outerMayReturnNull: Boolean?
    ) : this(name, exactType, outer as StoredLens<TS, out T?, *>, nested as StoredLens<US, out U, *>, outerMayReturnNull)

    @PublishedApi
    internal constructor(name: String?, outer: Lens<TS, TR, out T?, *>, nested: Lens<US, T, out U, *>) : this(
            name,
            Unit.run {
                if (outer.type is Schema<*> || nested.type is DataType.Nullable<*, *>) nested.type
                //  ^^^^^^leave as is^^^^^^ or ^^^^^^^ it is already nullable ^^^^^^^
                else nullable(nested.type as DataType<Any>)
            } as UD,
            outer,
            nested,
            outerMayReturnNull = outer.type !is Schema<*>
    )

    override fun invoke(p1: TR): U? {
        val a = (outer as Lens<TS, TR, out T?, *>)(p1)
        return if (a == null && outerMayReturnNull!!) null else (nested as Lens<US, T, out U, *>)(a as T)
    }

    override val default: U
        get() = (nested as Lens<US, T, out U, *>).default

    override val size: Int
        get() = outer.size + nested.size

    override fun get(index: Int): NamedLens<*, *, *, *> {
        val outerSize = outer.size
        return if (index < outerSize) outer[index] else nested[index - outerSize]
    }

    // copy-paste of orderedHashCode + orderedEquals from AbstractList
    // note: this intentionally ignores [name] value

    override fun hashCode(): Int {
        var hashCode = 1
        for (i in 0 until size) {
            hashCode = 31 * hashCode + (this[i].hashCode())
        }
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Lens<*, *, *, *> || other.size != size) return false

        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun toString(): String = buildString {
        append(outer)
        if (outerMayReturnNull == true) append('?')
        append('.').append(nested)
    }

}

// endregion telescope impl

// region special lenses

internal class PkLens<S : Schema<S>, ID : IdBound> constructor(
        private val table: Table<S, ID, out Record<S, ID>>
) : BaseLens<S, Record<S, ID>, ID, DataType.Simple<ID>>(
        table.idColName, table.idColType
) {

    override fun get(index: Int): NamedLens<*, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            64 // FieldDef.hashCode() is in [0; 63], be different!

    override fun equals(other: Any?): Boolean = // for tests
            other is PkLens<*, *> && other.table === table

    override fun toString(): String =
            "${table.name}.$name (PRIMARY KEY)"


    override fun invoke(p1: Record<S, ID>): ID =
            p1.primaryKey

}

internal class FieldSetLens<S : Schema<S>>(
        name: String
) : BaseLens<S, PartialStruct<S>, Long, DataType.Simple<Long>>(name, long) {

    override fun invoke(p1: PartialStruct<S>): Long? =
            p1.fields.bitmask

    override fun get(index: Int): NamedLens<*, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            if (type is DataType.Nullable<*, *>) 65 else 66 // FieldDef.hashCode() is in [0; 63], PK is 64, be different!

    // tests only

    override fun equals(other: Any?): Boolean =
            other is FieldSetLens<*> && name == other.name && type == other.type

    override fun toString(): String = buildString {
        append(name).append(" (")
        val nullable = exactType is DataType.Nullable<*, *>
        val actual: DataType<*> = if (exactType is DataType.Nullable<*, *>) exactType.actualType else exactType
        val partial = actual !is Schema<*>
        if (nullable) append("nullability info")
        if (nullable && partial) append(", ")
        if (partial) append("fields set")
        append(')')
    }

}

// ugly class for minimizing method count / vtable size
internal abstract class BaseLens<SCH : Schema<SCH>, STR : PartialStruct<SCH>, T, DT : DataType<T>>(
        private val _name: String?,
        final override val exactType: DT
) : NamedLens<SCH, STR, T, DT> {

    final override val name: String
        get() = _name!! // give this instance as Lens, not NamedLens, when _name is null

    final override val type: DataType<T>
        get() = exactType

    override val default: T // PK and synthetic lenses don't have & don't need it
        get() = throw UnsupportedOperationException()

    override val size: Int
        get() = 1 // true for FieldSetLens and PkLens, but overridden in Telescope

}

// endregion special lenses
