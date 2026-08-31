package com.inforhard.pos.core.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommandWorkManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val directExecutor = Executor(Runnable::run)

    @Test
    fun workerMapsDelegateOutcomesWithoutTransportKnowledge() {
        assertEquals(
            ListenableWorker.Result.success(),
            worker(CommandWorkOutcome.COMPLETE).doWork(),
        )
        assertEquals(
            ListenableWorker.Result.retry(),
            worker(CommandWorkOutcome.RETRY).doWork(),
        )
        assertEquals(
            ListenableWorker.Result.failure(),
            worker(CommandWorkOutcome.BLOCKED).doWork(),
        )
    }

    @Test
    fun uniqueKeepSchedulingDoesNotDuplicatePendingWork() {
        val configuration = Configuration.Builder()
            .setExecutor(directExecutor)
            .setWorkerFactory(CommandSyncWorkerFactory { CommandWorkOutcome.BLOCKED })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        val workManager = WorkManager.getInstance(context)
        val scheduler = CommandWorkScheduler(workManager)

        scheduler.schedule().result.get()
        scheduler.schedule().result.get()

        val work = workManager
            .getWorkInfosForUniqueWork(CommandWorkScheduler.UNIQUE_WORK_NAME)
            .get()
        assertEquals(1, work.size)
        assertEquals(WorkInfo.State.ENQUEUED, work.single().state)
    }

    private fun worker(outcome: CommandWorkOutcome): CommandSyncWorker =
        TestWorkerBuilder<CommandSyncWorker>(
            context = context,
            executor = directExecutor,
            inputData = Data.EMPTY,
        ).setWorkerFactory(CommandSyncWorkerFactory { outcome }).build()
}
