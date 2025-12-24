package com.brunocsantos.agoravai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessoes_pomodoro_concluidas")
data class SessaoPomodoro(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestampConclusao: Long = System.currentTimeMillis()
)