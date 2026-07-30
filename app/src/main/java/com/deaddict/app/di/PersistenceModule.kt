package com.deaddict.app.di

import android.content.Context
import androidx.room.Room
import com.deaddict.app.auth.AuthGateway
import com.deaddict.app.auth.SupabaseAuthGateway
import com.deaddict.app.auth.SupabaseClientProvider
import com.deaddict.app.coach.RookPreferenceStore
import com.deaddict.app.insights.LocalInsightsRepository
import com.deaddict.app.notifications.NotificationPreferenceStore
import com.deaddict.app.notifications.NotificationScheduler
import com.deaddict.app.privacy.PrivacyPreferenceStore
import com.deaddict.app.session.OwnerSessionStore
import com.deaddict.app.sync.RemoteSyncGateway
import com.deaddict.app.sync.SupabaseRemoteSyncGateway
import com.deaddict.app.sync.SyncScheduler
import com.deaddict.app.usage.DigitalUsageRepository
import com.deaddict.database.DAILY_CHECK_IN_DATABASE_CALLBACK
import com.deaddict.database.DeAddictDatabase
import com.deaddict.database.MIGRATION_1_2
import com.deaddict.database.MIGRATION_2_3
import com.deaddict.database.MIGRATION_3_4
import com.deaddict.database.MIGRATION_4_5
import com.deaddict.database.RECOVERY_EVENT_DATABASE_CALLBACK
import com.deaddict.database.RECOVERY_TRACK_DATABASE_CALLBACK
import com.deaddict.database.repository.LocalDailyCheckInRepository
import com.deaddict.database.repository.LocalProgramRepository
import com.deaddict.database.repository.LocalRecoveryTrackRepository
import com.deaddict.database.repository.LocalRescueRepository
import com.deaddict.database.repository.LocalTrackingRepository
import com.deaddict.database.repository.RecoveryOwnerContext
import com.deaddict.database.repository.RecoveryOwnerSelection
import com.deaddict.programs.DefaultProgramRegistry
import com.deaddict.programs.ProgramRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DeAddictDatabase =
        Room.databaseBuilder(
            context,
            DeAddictDatabase::class.java,
            "deaddict.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(RECOVERY_TRACK_DATABASE_CALLBACK)
            .addCallback(RECOVERY_EVENT_DATABASE_CALLBACK)
            .addCallback(DAILY_CHECK_IN_DATABASE_CALLBACK)
            .build()

    @Provides
    @Singleton
    fun ownerSessionStore(
        @ApplicationContext context: Context,
    ): OwnerSessionStore = OwnerSessionStore(context)

    @Provides
    @Singleton
    fun recoveryOwnerContext(ownerSessionStore: OwnerSessionStore): RecoveryOwnerContext =
        RecoveryOwnerContext {
            val session = ownerSessionStore.state.first()
            val owner = session.ownerKey
            val track = session.selectedRecoveryTrackId
            if (owner == null || track == null) null else RecoveryOwnerSelection(owner, track)
        }

    @Provides
    @Singleton
    fun authGateway(
        clientProvider: SupabaseClientProvider,
        syncScheduler: SyncScheduler,
        ownerSessionStore: OwnerSessionStore,
    ): AuthGateway = SupabaseAuthGateway(
        client = clientProvider.client,
        onAuthenticated = { user ->
            ownerSessionStore.establishAuthenticated(user.id)
            syncScheduler.scheduleNow()
        },
        onSignedOut = {
            ownerSessionStore.establishGuest()
        },
    )

    @Provides
    @Singleton
    fun remoteSyncGateway(clientProvider: SupabaseClientProvider): RemoteSyncGateway =
        SupabaseRemoteSyncGateway(clientProvider.client)

    @Provides
    @Singleton
    fun programRegistry(): ProgramRegistry = DefaultProgramRegistry()

    @Provides
    @Singleton
    fun programRepository(database: DeAddictDatabase): LocalProgramRepository =
        LocalProgramRepository(database)

    @Provides
    @Singleton
    fun recoveryTrackRepository(database: DeAddictDatabase): LocalRecoveryTrackRepository =
        LocalRecoveryTrackRepository(database)

    @Provides
    @Singleton
    fun dailyCheckInRepository(database: DeAddictDatabase): LocalDailyCheckInRepository =
        LocalDailyCheckInRepository(database)

    @Provides
    @Singleton
    fun trackingRepository(
        database: DeAddictDatabase,
        ownerContext: RecoveryOwnerContext,
    ): LocalTrackingRepository = LocalTrackingRepository(database, ownerContext)

    @Provides
    @Singleton
    fun digitalUsageRepository(@ApplicationContext context: Context): DigitalUsageRepository =
        DigitalUsageRepository(context)

    @Provides
    @Singleton
    fun rescueRepository(
        database: DeAddictDatabase,
        ownerContext: RecoveryOwnerContext,
    ): LocalRescueRepository = LocalRescueRepository(database, ownerContext)

    @Provides
    @Singleton
    fun notificationPreferenceStore(
        @ApplicationContext context: Context,
    ): NotificationPreferenceStore = NotificationPreferenceStore(context)

    @Provides
    @Singleton
    fun rookPreferenceStore(
        @ApplicationContext context: Context,
    ): RookPreferenceStore = RookPreferenceStore(context)

    @Provides
    @Singleton
    fun notificationScheduler(
        @ApplicationContext context: Context,
    ): NotificationScheduler = NotificationScheduler(context)

    @Provides
    @Singleton
    fun insightsRepository(
        database: DeAddictDatabase,
        ownerContext: RecoveryOwnerContext,
    ): LocalInsightsRepository = LocalInsightsRepository(database, ownerContext)

    @Provides
    @Singleton
    fun privacyPreferenceStore(
        @ApplicationContext context: Context,
    ): PrivacyPreferenceStore = PrivacyPreferenceStore(context)
}
