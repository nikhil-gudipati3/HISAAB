package app.hisaab

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.hisaab.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoInstrumentedTest {
    private lateinit var db: HisaabDb
    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, HisaabDb::class.java).allowMainThreadQueries().build()
    }
    @After fun tear() { db.close() }
    @Test fun bucketAndTxnCrud() = runBlocking {
        val bid = db.buckets().insert(BucketEntity(name = "Needs", monthlyBudgetPaise = 50000L))
        val id = db.txns().insert(TxnEntity(amountPaise = 12000L, payeeName = "Chai Point",
            payeeVpa = "chaipoint@okhdfcbank", note = null, categoryKey = "Food and drink",
            bucketId = bid, accountId = null, timestamp = 1000L, status = "SUCCESS", source = "MANUAL"))
        val got = db.txns().byId(id)
        assertNotNull(got); assertEquals(12000L, got!!.amountPaise)
        assertEquals(1, db.txns().inRange(0L, 2000L).size)
        val pending = db.txns().insert(TxnEntity(amountPaise = 5000L, payeeName = "X", payeeVpa = "x@okhdfcbank",
            note = null, categoryKey = null, bucketId = bid, accountId = null, timestamp = 1500L,
            status = "PENDING", source = "APP_PAY"))
        assertEquals(1, db.txns().pending().size)
        db.txns().delete(pending)
        assertEquals(0, db.txns().pending().size)
        val mem = db.txns().lastSuccessForPayee("chaipoint@okhdfcbank")
        assertNotNull(mem)
    }
}
