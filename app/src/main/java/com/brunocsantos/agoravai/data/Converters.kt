package com.brunocsantos.agoravai.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String): Set<DayOfWeek> {
        return if (data.isEmpty()) {
            emptySet()
        } else {
            data.split(",").map { DayOfWeek.valueOf(it) }.toSet()
        }
    }

    // --- NOVOS CONVERSORES PARA NOTIFICATION TYPE ---
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String {
        return type.name
    }

    @TypeConverter
    fun toNotificationType(value: String): NotificationType {
        return try {
            NotificationType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            NotificationType.PADRAO // Fallback seguro
        }
    }
}