package com.brunocsantos.agoravai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Compromisso::class,
        SessaoPomodoro::class,
        Etapa::class,
        AutomationSetting::class,
        ItemLembrete::class // <-- Nova entidade adicionada
    ],
    version = 10, // <-- Versão atualizada para forçar a migração destrutiva
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agora_vai_database"
                )
                    .fallbackToDestructiveMigration() // Recria o banco se houver mudança de versão
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}