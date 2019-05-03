package net.aquadc.properties.sql

import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.testing.assertReturnsGarbage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.sql.SQLException


class SqlPropTest {

    @Test fun record() = assertReturnsGarbage {
        val rec = createTestRecord()
        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = session[SomeTable].selectAll().value.single()
        assertSame(rec, sameRec) // DAO reuses and manages the only Record per table row

        val prop = rec prop SomeSchema.C
        val sameProp = sameRec prop SomeSchema.C
        assertSame(prop, sameProp)

        var called = 0
        val listener: ChangeListener<Long> = { old, new ->
            assertEquals(3, old)
            assertEquals(100, new)
            called++
        }
        prop.addUnconfinedChangeListener(listener)
        sameProp.addUnconfinedChangeListener(listener)

        session.withTransaction {
            rec[SomeSchema.C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        session.withTransaction { delete(rec) }

        rec
    }

    @Test fun count() = assertReturnsGarbage {
        val cnt = SomeDao.count()
        assertSame(cnt, SomeDao.count())
        assertEquals(0L, cnt.value)
        val rec = createTestRecord()
        assertEquals(1L, cnt.value)
        session.withTransaction { delete(rec) }
        assertEquals(0L, cnt.value)
        cnt
    }

    @Test fun select() = assertReturnsGarbage {
        val sel = SomeDao.selectAll()
        assertSame(sel, SomeDao.selectAll())
        assertEquals(emptyList<Nothing>(), sel.value)
        val rec = createTestRecord()
        assertEquals(listOf(rec), sel.value)
        session.withTransaction { delete(rec) }
        assertEquals(emptyList<Nothing>(), sel.value)
        sel
    }

    @Test fun selectConditionally() = assertReturnsGarbage {
        val rec = createTestRecord()
        val sel = SomeDao.select(SomeSchema.A eq "first", SomeSchema.A.asc)
        assertSame(sel, SomeDao.select(SomeSchema.A eq "first", SomeSchema.A.asc))
        session.withTransaction { delete(rec) }
        sel
    }

    @Test fun selectNone() = assertReturnsGarbage {
        val rec = createTestRecord()
        val sel = SomeDao.select(SomeSchema.A notEq "first")
        assertSame(sel, SomeDao.select(SomeSchema.A notEq "first"))
        assertEquals(emptyList<Nothing>(), sel.value)
        session.withTransaction { delete(rec) }
        sel
    }


    @Test fun transactionalWrapper() {
        val originalRec = createTestRecord()
        val rec = originalRec.transactional()

        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = session[SomeTable].selectAll().value.single()
        assertNotSame(rec, sameRec)
        assertEquals(rec, sameRec) // same data represented through different interfaces

        val prop = rec prop SomeSchema.C
        val sameProp = sameRec prop SomeSchema.C
        assertNotSame(prop, sameProp)
        assertEquals(prop.value, sameProp.value) // same here: different objects, same data

        var called = 0
        val listener: ChangeListener<Long> = { old, new ->
            assertEquals(3, old)
            assertEquals(100, new)
            called++
        }
        prop.addUnconfinedChangeListener(listener)
        sameProp.addUnconfinedChangeListener(listener)

        rec.transaction {
            it[C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        session.withTransaction {
            delete(originalRec)
        }
    }


    @Test fun pkAsField() {
        val rec = session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 19
                it[Value] = "zzz"
            })
        }
        assertEquals(19L, rec.primaryKey)
        assertEquals(19L, rec[SchWithId.Id])
        assertEquals("zzz", rec[SchWithId.Value])
    }

    @Test(expected = SQLException::class)
    fun `can't insert twice with the same PK in one transaction`() {
        session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 44
                it[Value] = "yyy"
            })
            insert(TableWithId, SchWithId.build {
                it[Id] = 44
                it[Value] = "zzz"
            })
        }
    }

    @Test(expected = SQLException::class)
    fun `can't insert twice with the same PK in different transactions`() {
        session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 44
                it[Value] = "yyy"
            })
        }
        session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 44
                it[Value] = "zzz"
            })
        }
    }

    @Test fun `poisoned statement evicted`() {
        try {
            `can't insert twice with the same PK in one transaction`()
        } catch (ignored: SQLException) {
            // the statement is poisoned
        }

        session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 86
                it[Value] = "aaa"
            })
        }
    }

}

// TODO: .shapshots() should change only one time per transaction
