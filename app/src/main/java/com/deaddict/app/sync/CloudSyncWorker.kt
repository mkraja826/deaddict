package com.deaddict.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deaddict.database.DeAddictDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = workerMutex.withLock {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            CloudSyncEntryPoint::class.java,
        )
        val database = entryPoint.database()
        val remote = entryPoint.remoteSyncGateway()
        val uploadProcessor = SyncProcessor(
            store = RoomSyncStore(database),
            remote = remote,
        )

        var batchCount = 0
        while (batchCount < MAX_BATCHES_PER_RUN) {
            when (uploadProcessor.runBatch()) {
                SyncRunResult.SUCCESS -> batchCount += 1
                SyncRunResult.RETRY -> return@withLock Result.retry()
                SyncRunResult.IDLE -> break
                SyncRunResult.UNAVAILABLE,
                SyncRunResult.SIGNED_OUT,
                -> return@withLock Result.success()
            }
        }

        return@withLock try {
            CloudRestoreProcessor(
                store = RoomRestoreStore(database),
                remote = remote,
            ).restore()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private companion object {
        const val MAX_BATCHES_PER_RUN = 4
        val workerMutex = Mutex()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CloudSyncEntryPoint {
    fun database(): DeAddictDatabase

    fun remoteSyncGateway(): RemoteSyncGateway
}
