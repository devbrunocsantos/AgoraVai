package com.brunocsantos.agoravai.services

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.brunocsantos.agoravai.data.AutomationSetting
import com.brunocsantos.agoravai.workers.PomodoroWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PomodoroScheduler {

    // O nome de cada worker agora inclui o ID da automação para ser único
    private fun getWorkTagFor(setting: AutomationSetting) = "pomodoro_automation_${setting.id}"

    // NOVA FUNÇÃO: Agenda um worker para uma automação específica
    fun schedule(context: Context, setting: AutomationSetting) {
        val workManager = WorkManager.getInstance(context)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, setting.hour)
            set(Calendar.MINUTE, setting.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (now.after(target)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        // O worker agora se repete a cada 24 horas
        val workRequest = PeriodicWorkRequestBuilder<PomodoroWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(getWorkTagFor(setting)) // Adiciona uma tag única
            .build()

        workManager.enqueueUniquePeriodicWork(
            getWorkTagFor(setting),
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        val delayInMinutes = initialDelay / 1000 / 60
        Log.d("AgoraVai-Debug", "Automação '${setting.title}' agendada para rodar em $delayInMinutes minutos.")
    }

    // NOVA FUNÇÃO: Cancela todos os agendamentos
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        Log.d("AgoraVai-Debug", "Todos os agendamentos de Pomodoro foram cancelados.")
    }

    // Mantemos a função antiga caso seja útil no futuro, mas ela não é mais usada
    fun cancel(context: Context, setting: AutomationSetting) {
        WorkManager.getInstance(context).cancelAllWorkByTag(getWorkTagFor(setting))
        Log.d("AgoraVai-Debug", "Automação '${setting.title}' cancelada.")
    }
}