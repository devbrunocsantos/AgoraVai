package com.brunocsantos.agoravai.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsManager {

    private const val PREFS_NAME = "pomodoro_settings_prefs"
    const val KEY_FOCUS_DURATION = "focus_duration_mins"
    const val KEY_SHORT_BREAK_DURATION = "short_break_duration_mins"
    const val KEY_LONG_BREAK_DURATION = "long_break_duration_mins"
    const val KEY_CYCLES_BEFORE_LONG_BREAK = "cycles_before_long_break"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val KEY_SAFETY_TIME = "safety_time_mins" // <-- NOVA CHAVE

    const val DEFAULT_FOCUS_MINS = 25
    const val DEFAULT_SHORT_BREAK_MINS = 5
    const val DEFAULT_LONG_BREAK_MINS = 15
    const val DEFAULT_CYCLES = 4
    const val DEFAULT_DAILY_GOAL = 8
    const val DEFAULT_SAFETY_TIME_MINS = 15 // <-- NOVO PADRÃO

    private val _focusDuration = MutableStateFlow(DEFAULT_FOCUS_MINS)
    val focusDuration = _focusDuration.asStateFlow()

    private val _shortBreakDuration = MutableStateFlow(DEFAULT_SHORT_BREAK_MINS)
    val shortBreakDuration = _shortBreakDuration.asStateFlow()

    private val _longBreakDuration = MutableStateFlow(DEFAULT_LONG_BREAK_MINS)
    val longBreakDuration = _longBreakDuration.asStateFlow()

    private val _cyclesBeforeLongBreak = MutableStateFlow(DEFAULT_CYCLES)
    val cyclesBeforeLongBreak = _cyclesBeforeLongBreak.asStateFlow()

    private val _dailyGoal = MutableStateFlow(DEFAULT_DAILY_GOAL)
    val dailyGoal = _dailyGoal.asStateFlow()

    private val _safetyTime = MutableStateFlow(DEFAULT_SAFETY_TIME_MINS) // <-- NOVO STATEFLOW
    val safetyTime = _safetyTime.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _focusDuration.value = prefs.getInt(KEY_FOCUS_DURATION, DEFAULT_FOCUS_MINS)
        _shortBreakDuration.value = prefs.getInt(KEY_SHORT_BREAK_DURATION, DEFAULT_SHORT_BREAK_MINS)
        _longBreakDuration.value = prefs.getInt(KEY_LONG_BREAK_DURATION, DEFAULT_LONG_BREAK_MINS)
        _cyclesBeforeLongBreak.value = prefs.getInt(KEY_CYCLES_BEFORE_LONG_BREAK, DEFAULT_CYCLES)
        _dailyGoal.value = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
        _safetyTime.value = prefs.getInt(KEY_SAFETY_TIME, DEFAULT_SAFETY_TIME_MINS)
    }

    fun saveSettings(
        context: Context,
        focusMins: Int,
        shortBreakMins: Int,
        longBreakMins: Int,
        cycles: Int,
        dailyGoal: Int,
        safetyTime: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_FOCUS_DURATION, focusMins)
            .putInt(KEY_SHORT_BREAK_DURATION, shortBreakMins)
            .putInt(KEY_LONG_BREAK_DURATION, longBreakMins)
            .putInt(KEY_CYCLES_BEFORE_LONG_BREAK, cycles)
            .putInt(KEY_DAILY_GOAL, dailyGoal)
            .putInt(KEY_SAFETY_TIME, safetyTime)
            .apply()

        _focusDuration.value = focusMins
        _shortBreakDuration.value = shortBreakMins
        _longBreakDuration.value = longBreakMins
        _cyclesBeforeLongBreak.value = cycles
        _dailyGoal.value = dailyGoal
        _safetyTime.value = safetyTime
    }

    fun getFocusDuration(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FOCUS_DURATION, DEFAULT_FOCUS_MINS)
    }
}