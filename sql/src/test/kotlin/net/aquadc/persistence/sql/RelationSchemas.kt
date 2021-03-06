package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.tuple.Tuple
import net.aquadc.persistence.extended.partial
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Test

typealias ShallowSchema = Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>
val shallowSchema = Tuple("a", string, "b", i64)

val dupeEmbed = Tuple("a_b", string, "a", shallowSchema)

val embedSchema = Tuple("a", string, "b", shallowSchema)

val embedPartial = Tuple("a", string, "b", partial(shallowSchema))

val embedNullable = Tuple("a", string, "b", nullable(shallowSchema))

val embedNullablePartial = Tuple("a", string, "b", nullable(partial(shallowSchema)))

class RelationSchemas {

    @Test fun `no rels`() {
        val table = tableOf(shallowSchema, "zzz", "_id", i64)
        assertEquals(
                arrayOf(PkLens(table, i64), shallowSchema.First, shallowSchema.Second),
                table.columns
        )
    }
    @Test(expected = IllegalStateException::class) fun `same name as pk`() {
        tableOf(shallowSchema, "dupeName", "a", i64).columns
    }

    @Test(expected = IllegalStateException::class) fun `same name as nested`() {
        tableOf(dupeEmbed, "", "a", i64) {
            arrayOf(
                embed(SnakeCase, dupeEmbed.Second)
            )
        }.columns
    }

    object EmptyName : Schema<EmptyName>() {
        val a = "" let string
    }
    @Test(expected = IllegalStateException::class) fun `empty name`() {
        tableOf(EmptyName, "", "id", i64).columns
    }

    // primary key

    @Test fun `with id`() {
        assertEquals(
                arrayOf(SchWithId.Id, SchWithId.Value, SchWithId.MutValue),
                TableWithId.columns
        )
    }

    // embedded

    @Test(expected = NoSuchElementException::class)
    fun `rels required`() {
        tableOf(embedSchema, "zzz", "_id", i64).columns
    }
    @Test fun `embed struct`() {
        val table = tableOf(embedSchema, "zzz", "_id", i64) {
            arrayOf(
                embed(SnakeCase, embedSchema.Second)
            )
        }
        assertEquals(arrayOf("_id", "a", "b_a", "b_b"), table.columns.namesIn(embedSchema))
        assertEquals(
                arrayOf(
                        PkLens(table, i64),
                        embedSchema.First,
                        Telescope("", embedSchema.Second, shallowSchema.First),
                        Telescope("", embedSchema.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test fun `embed partial`() {
        val table = tableOf(embedPartial, "zzz", "_id", i64) {
            arrayOf(
                embed(SnakeCase, embedPartial.Second, "fieldsSet")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_fieldsSet", "b_a", "b_b"), table.columns.namesIn(embedPartial))
        assertEquals(
                arrayOf(
                        PkLens(table, i64),
                        embedPartial.First,
                        embedPartial.Second / FieldSetLens<ShallowSchema>("fieldsSet"),
                        Telescope("b_a", embedPartial.Second, shallowSchema.First),
                        Telescope("b_b", embedPartial.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test fun `embed nullable`() {
        val table = tableOf(embedNullable, "zzz", "_id", i64) {
            arrayOf(
                embed(SnakeCase, embedNullable.Second, "nullability")
            )
        }
        assertEquals(arrayOf("_id", "a", "b_nullability", "b_a", "b_b"), table.columns.namesIn(embedNullable))
        assertEquals(
                arrayOf(
                        PkLens(table, i64),
                        embedNullable.First,
                        embedNullable.Second / FieldSetLens("nullability"),
                        Telescope("b_a", embedNullable.Second, shallowSchema.First),
                        Telescope("b_b", embedNullable.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    @Test fun `embed nullable partial`() {
        val table = tableOf(embedNullablePartial, "zzz", "_id", i64) {
            arrayOf(
                embed(SnakeCase, embedNullablePartial.Second, "fieldSetAndNullability")
            )
        }
        assertEquals(
                arrayOf("_id", "a", "b_fieldSetAndNullability", "b_a", "b_b"),
                table.columns.namesIn(embedNullablePartial)
        )
        assertEquals(
                arrayOf(
                        PkLens(table, i64),
                        embedNullablePartial.First,
                        embedNullablePartial.Second / FieldSetLens<ShallowSchema>("fieldSetAndNullability"),
                        Telescope("b_a", embedNullablePartial.Second, shallowSchema.First),
                        Telescope("b_b", embedNullablePartial.Second, shallowSchema.Second)
                ),
                table.columns
        )
    }

    private fun <SCH : Schema<SCH>> Array<out Named<SCH>>.namesIn(schema: SCH): Array<out CharSequence> =
            mapIndexedToArray { _, it -> it.name(schema) }

    private fun assertEquals(expected: Array<out Any?>, actual: Array<out Any?>) {
        if (expected.size != actual.size) assertEquals(expected as Any, actual as Any) // fallback to JUnit impl
        expected.zip(actual).forEachIndexed { idx, (ex, ac) ->
            assertEquals("at $idx", ex, ac) // fail separately
        }
    }

}
