package com.deaddict.database.repository

import com.deaddict.database.dao.SyncOutboxDao
import com.deaddict.database.entity.SyncOutboxEntity
import kotlin.math.min

class SyncQueue(
    private val dao: SyncOutboxDao,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
) {
    suspend fun nextBatch(limit: Int = 25): List<SyncOutboxEntity> {
        require(limit in 1..100)
        return dao.nextBatch(clock.nowMillis(), limit)
    }

    suspend fun claim(id: String): Boolean = dao.claim(id) == 1

    suspend fun complete(id: String): Boolean = dao.complete(id) == 1

    suspend fun fail(id: String, currentAttemptCount: Int, errorCode: String): Boolean {
        require(currentAttemptCount >= 0)
        require(errorCode.matches(Regex("[A-Z0-9_]{1,48}")))
        val nextAttemptCount = currentAttemptCount + 1
        val deadLetter = nextAttemptCount >= MAX_ATTEMPTS
        val delay = min(
            BASE_DELAY_MILLIS * (1L shl currentAttemptCount.coerceAtMost(10)),
            MAX_DELAY_MILLIS,
        )
        return dao.retry(
            id = id,
            nextAttempt = clock.nowMillis() + delay,
            errorCode = errorCode,
            deadLetter = deadLetter,
        ) == 1
    }

    private companion object {
        const val MAX_ATTEMPTS = 10
        const val BASE_DELAY_MILLIS = 30_000L
        const val MAX_DELAY_MILLIS = 6 * 60 * 60 * 1_000L
    }
}

