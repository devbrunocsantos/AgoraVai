package com.brunocsantos.agoravai.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.brunocsantos.agoravai.MainActivity
import com.brunocsantos.agoravai.R
import com.brunocsantos.agoravai.notifications.CHANNEL_ID
import com.brunocsantos.agoravai.ui.pomodoro.PomodoroState
import com.brunocsantos.agoravai.ui.pomodoro.TimerState
import com.brunocsantos.agoravai.utils.SettingsManager
import kotlinx.coroutines.*
import java.util.Locale

class PomodoroService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private var cicloAtual = 0
    private var ciclosParaPausaLonga = SettingsManager.DEFAULT_CYCLES
    private var metaTotalDaSessao = SettingsManager.DEFAULT_DAILY_GOAL

    private var duracaoFoco = SettingsManager.DEFAULT_FOCUS_MINS * 60 * 1000L
    private var duracaoPausaCurta = SettingsManager.DEFAULT_SHORT_BREAK_MINS * 60 * 1000L
    private var duracaoPausaLonga = SettingsManager.DEFAULT_LONG_BREAK_MINS * 60 * 1000L

    override fun onCreate() {
        super.onCreate()
        carregarConfiguracoes()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        carregarConfiguracoes()

        val customCycles = intent?.getIntExtra("EXTRA_CYCLES", -1) ?: -1
        metaTotalDaSessao = if (customCycles != -1) {
            customCycles
        } else {
            val prefs = getSharedPreferences("pomodoro_settings_prefs", Context.MODE_PRIVATE)
            prefs.getInt(SettingsManager.KEY_DAILY_GOAL, SettingsManager.DEFAULT_DAILY_GOAL)
        }

        when (intent?.action) {
            Actions.START.toString() -> start()
            Actions.PAUSE.toString() -> pause()
            Actions.RESET.toString() -> reset()
        }
        return START_STICKY
    }

    private fun carregarConfiguracoes() {
        val prefs = getSharedPreferences("pomodoro_settings_prefs", Context.MODE_PRIVATE)
        duracaoFoco = prefs.getInt(SettingsManager.KEY_FOCUS_DURATION, SettingsManager.DEFAULT_FOCUS_MINS) * 60 * 1000L
        duracaoPausaCurta = prefs.getInt(SettingsManager.KEY_SHORT_BREAK_DURATION, SettingsManager.DEFAULT_SHORT_BREAK_MINS) * 60 * 1000L
        duracaoPausaLonga = prefs.getInt(SettingsManager.KEY_LONG_BREAK_DURATION, SettingsManager.DEFAULT_LONG_BREAK_MINS) * 60 * 1000L
        ciclosParaPausaLonga = prefs.getInt(SettingsManager.KEY_CYCLES_BEFORE_LONG_BREAK, SettingsManager.DEFAULT_CYCLES)
    }

    private fun start() {
        if (PomodoroTimerState.estadoPomodoro.value == PomodoroState.PARADO) {
            PomodoroTimerState.setEstadoPomodoro(PomodoroState.FOCO)
            PomodoroTimerState.setTempo(duracaoFoco)
            cicloAtual = 0
        }
        PomodoroTimerState.setEstadoTimer(TimerState.RODANDO)
        startForeground(1, createNotification())
        startCycle()
    }

    private fun startCycle() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var tempoAtual = PomodoroTimerState.tempoRestante.value
            while (tempoAtual > 0) {
                PomodoroTimerState.setTempo(tempoAtual)
                updateNotification()
                delay(1000L)
                tempoAtual -= 1000L
            }
            PomodoroTimerState.setTempo(0)
            updateNotification()
            moveToNextState()
        }
    }

    // LÓGICA DE TRANSIÇÃO E PAUSA LONGA CORRIGIDA
    private fun moveToNextState() {
        playNotificationSound()

        val estadoAnterior = PomodoroTimerState.estadoPomodoro.value

        // Se a pausa longa acabou, a sessão terminou.
        if (estadoAnterior == PomodoroState.PAUSA_LONGA) {
            // A meta total de sessões foi atingida?
            // (Verificamos isso pelo cicloAtual ter sido zerado no início da pausa longa)
            if (cicloAtual == 0) {
                if (AppForegroundState.isAppInForeground.value) {
                    PomodoroCompletionState.onSessionCompleted()
                } else {
                    showCompletionNotification()
                }
                reset()
                return
            }
        }

        if (estadoAnterior == PomodoroState.FOCO) {
            serviceScope.launch { FocusSessionState.onFocusSessionCompleted() }
            cicloAtual++
        }

        val proximoEstado: PomodoroState
        val proximoTempo: Long

        if (estadoAnterior == PomodoroState.FOCO) {
            // Verifica se a meta total de sessões foi atingida
            if (cicloAtual >= metaTotalDaSessao) {
                if (AppForegroundState.isAppInForeground.value) {
                    PomodoroCompletionState.onSessionCompleted()
                } else {
                    showCompletionNotification()
                }
                reset()
                return
            }
            // Se não, verifica se é hora da pausa longa
            if (cicloAtual > 0 && cicloAtual % ciclosParaPausaLonga == 0) {
                proximoEstado = PomodoroState.PAUSA_LONGA
                proximoTempo = duracaoPausaLonga
            } else {
                proximoEstado = PomodoroState.PAUSA_CURTA
                proximoTempo = duracaoPausaCurta
            }
        } else { // Se estava em pausa (curta ou longa), o próximo é foco
            proximoEstado = PomodoroState.FOCO
            proximoTempo = duracaoFoco
        }

        if (!AppForegroundState.isAppInForeground.value) {
            val message = when(proximoEstado) {
                PomodoroState.PAUSA_CURTA -> "Seu tempo de foco acabou. Hora de uma pausa curta!"
                PomodoroState.PAUSA_LONGA -> "Ótimo trabalho! Hora da sua merecida pausa longa."
                else -> "A pausa terminou. Hora de voltar a focar!"
            }
            showTransitionNotification("Pomodoro", message)
        }

        PomodoroTimerState.setEstadoPomodoro(proximoEstado)
        PomodoroTimerState.setTempo(proximoTempo)

        serviceScope.launch {
            delay(1000L)
            startCycle()
        }
    }

    private fun pause() {
        timerJob?.cancel()
        PomodoroTimerState.setEstadoTimer(TimerState.PAUSADO)
        updateNotification()
    }

    private fun reset() {
        timerJob?.cancel()
        cicloAtual = 0
        PomodoroTimerState.setEstadoPomodoro(PomodoroState.PARADO)
        PomodoroTimerState.setEstadoTimer(TimerState.PARADO)
        PomodoroTimerState.resetToDefaults(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playNotificationSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("PomodoroService", "Não foi possível tocar o som de notificação", e)
        }
    }

    private fun showCompletionNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("destination_screen", "pomodoro")
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 10, notificationIntent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sessão Concluída!")
            .setContentText("Parabéns! Você completou todos os ciclos.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(this).notify(2, builder.build())
        } catch (e: SecurityException) {
            Log.e("PomodoroService", "Falha ao mostrar notificação de conclusão.", e)
        }
    }

    private fun showTransitionNotification(title: String, message: String) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("destination_screen", "pomodoro")
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 11, notificationIntent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(this).notify(3, builder.build())
        } catch (e: SecurityException) {
            Log.e("PomodoroService", "Falha ao mostrar notificação de transição.", e)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("destination_screen", "pomodoro")
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val estadoAtual = PomodoroTimerState.estadoPomodoro.value
        val timerState = PomodoroTimerState.estadoTimer.value

        val tituloNotificacao = when (estadoAtual) {
            PomodoroState.FOCO -> "Hora de Focar! 💪 (${cicloAtual + 1}/$metaTotalDaSessao)"
            PomodoroState.PAUSA_CURTA -> "Pausa Curta! ☕"
            PomodoroState.PAUSA_LONGA -> "Pausa Longa! ✨"
            PomodoroState.PARADO -> "Pomodoro Parado"
        }

        val textoNotificacao = if (timerState == TimerState.PAUSADO) {
            "Pausado em ${formatarTempo(PomodoroTimerState.tempoRestante.value)}"
        } else {
            "Tempo restante: ${formatarTempo(PomodoroTimerState.tempoRestante.value)}"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(tituloNotificacao)
            .setContentText(textoNotificacao)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPendingIntent)
            .setOnlyAlertOnce(true)

        val resetIntent = Intent(this, PomodoroService::class.java).apply { action = Actions.RESET.toString() }
        val resetPendingIntent = PendingIntent.getService(this, 1, resetIntent, pendingIntentFlags)

        when (timerState) {
            TimerState.RODANDO -> {
                val pauseIntent = Intent(this, PomodoroService::class.java).apply { action = Actions.PAUSE.toString() }
                val pausePendingIntent = PendingIntent.getService(this, 2, pauseIntent, pendingIntentFlags)
                builder.addAction(R.drawable.ic_launcher_foreground, "Pausar", pausePendingIntent)
            }
            TimerState.PAUSADO -> {
                val resumeIntent = Intent(this, PomodoroService::class.java).apply { action = Actions.START.toString() }
                val resumePendingIntent = PendingIntent.getService(this, 3, resumeIntent, pendingIntentFlags)
                builder.addAction(R.drawable.ic_launcher_foreground, "Retomar", resumePendingIntent)
            }
            TimerState.PARADO -> { /* Não adiciona botões */ }
        }

        if (timerState != TimerState.PARADO) {
            builder.addAction(R.drawable.ic_launcher_foreground, "Resetar", resetPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, createNotification())
    }

    private fun formatarTempo(millis: Long): String {
        val minutos = (millis / 1000) / 60
        val segundos = (millis / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    enum class Actions { START, PAUSE, RESET }
}