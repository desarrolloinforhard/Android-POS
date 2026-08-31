package com.inforhard.pos.core.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

enum class CommandWorkOutcome {
    COMPLETE,
    RETRY,
    BLOCKED,
}

fun interface CommandWorkDelegate {
    fun runOnce(): CommandWorkOutcome
}

class CommandSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val delegate: CommandWorkDelegate,
) : Worker(appContext, workerParameters) {
    override fun doWork(): Result = when (delegate.runOnce()) {
        CommandWorkOutcome.COMPLETE -> Result.success()
        CommandWorkOutcome.RETRY -> Result.retry()
        CommandWorkOutcome.BLOCKED -> Result.failure()
    }
}

class CommandSyncWorkerFactory(
    private val delegate: CommandWorkDelegate,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = if (workerClassName == CommandSyncWorker::class.java.name) {
        CommandSyncWorker(appContext, workerParameters, delegate)
    } else {
        null
    }
}
