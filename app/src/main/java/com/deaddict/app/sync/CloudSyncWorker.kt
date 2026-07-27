package com.deaddict.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deaddict.database.DeAddictDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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
        val processor = SyncProcessor(
            store = RoomSyncStore(entryPoint.database()),
            remote = entryPoint.remoteSyncGateway(),
        )

        repeat(MAX_BATCHES_PER_RUN) {
            when (processor.runBatch()) {
                SyncRunResult.SUCCESS -> Unit
                SyncRunResult.RETRY -> return@withLock Result.retry()
                SyncRunResult.IDLE,
                SyncRunResult.UNAVAILABLE,
                SyncRunResult.SIGNED_OUT,
                -> return@withLock Result.success()
            }
        }
        Result.success()
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
