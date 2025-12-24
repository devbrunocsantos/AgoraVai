package com.brunocsantos.agoravai.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "automation_settings")
data class AutomationSetting(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,

    // Controle dos gatilhos
    val isEnabled: Boolean = true, // Switch geral da automação
    val isTimeEnabled: Boolean = true, // Switch para o gatilho de tempo
    val isLocationEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = true,
    val isCompleted: Boolean = false,

    // Dados do gatilho de tempo
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek>,

    // Novos dados do gatilho de localização
    val placeId: String? = null,
    val placeAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    // LINHA QUE PRECISA SER ADICIONADA:
    val cycles: Int
)