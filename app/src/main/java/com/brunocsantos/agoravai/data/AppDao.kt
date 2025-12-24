package com.brunocsantos.agoravai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- COMPROMISSOS (CRUD Básico e Status) ---
    @Insert
    suspend fun inserirCompromisso(compromisso: Compromisso): Long

    @Update
    suspend fun updateCompromisso(compromisso: Compromisso)

    @Delete
    suspend fun excluirCompromisso(compromisso: Compromisso)

    @Query("SELECT * FROM compromissos WHERE id = :id")
    suspend fun getCompromissoById(id: Long): Compromisso?

    @Transaction
    @Query("SELECT * FROM compromissos ORDER BY dataHoraEvento ASC")
    fun getAllCompromissosComEtapas(): Flow<List<CompromissoComEtapas>>

    // --- NOVAS QUERIES DE STATUS E LIMPEZA ---
    @Query("UPDATE compromissos SET confirmouSaida = 1 WHERE id = :id")
    suspend fun confirmarSaida(id: Long)

    @Query("UPDATE compromissos SET confirmouChegada = 1 WHERE id = :id")
    suspend fun confirmarChegada(id: Long)

    @Query("DELETE FROM compromissos WHERE deleteAfterComplete = 1 AND dataHoraEvento < :currentTime")
    suspend fun deleteExpiredCompromissos(currentTime: Long)

    // --- ETAPAS ---
    @Insert
    suspend fun inserirEtapas(etapas: List<Etapa>)

    @Query("DELETE FROM etapas WHERE compromissoId = :idDoCompromisso")
    suspend fun excluirEtapasPorCompromissoId(idDoCompromisso: Long)

    // --- ITENS DE LEMBRETE (Novo) ---
    @Insert
    suspend fun inserirItensLembrete(itens: List<ItemLembrete>)

    @Query("DELETE FROM itens_lembrete WHERE compromissoId = :idDoCompromisso")
    suspend fun excluirItensPorCompromissoId(idDoCompromisso: Long)


    // ===============================================================
    // --- SESSÃO POMODORO (Mantido sem alterações) ---
    @Insert
    suspend fun inserirSessaoPomodoroConcluida(sessao: SessaoPomodoro)

    @Query("SELECT COUNT(id) FROM sessoes_pomodoro_concluidas WHERE timestampConclusao >= :inicioDoDia")
    fun getSessoesConcluidasHoje(inicioDoDia: Long): Flow<Int>

    @Query("DELETE FROM sessoes_pomodoro_concluidas WHERE timestampConclusao < :timestampLimite")
    suspend fun limparSessoesAntigas(timestampLimite: Long)

    // --- AUTOMAÇÃO DO POMODORO (Mantido sem alterações) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAutomationSetting(setting: AutomationSetting)

    @Query("SELECT * FROM automation_settings WHERE isCompleted = 0 ORDER BY id DESC")
    fun getAllAutomationSettings(): Flow<List<AutomationSetting>>

    @Query("SELECT * FROM automation_settings WHERE isCompleted = 1 ORDER BY id DESC")
    fun getCompletedAutomationSettings(): Flow<List<AutomationSetting>>

    @Query("SELECT * FROM automation_settings WHERE id = :id")
    suspend fun getAutomationSettingById(id: Int): AutomationSetting?

    @Query("SELECT * FROM automation_settings WHERE isEnabled = 1")
    suspend fun getActiveAutomationSettings(): List<AutomationSetting>

    @Query("SELECT * FROM automation_settings")
    suspend fun getAllAutomationSettingsOnce(): List<AutomationSetting>

    @Query("UPDATE automation_settings SET isCompleted = 1, isEnabled = 0 WHERE id = :id")
    suspend fun markAsCompleted(id: Int)

    @Query("UPDATE automation_settings SET isCompleted = 0, isEnabled = 1 WHERE id = :id")
    suspend fun reactivateAutomation(id: Int)

    @Query("DELETE FROM automation_settings WHERE id = :id")
    suspend fun deleteAutomationSettingById(id: Int)

    @Query("DELETE FROM automation_settings WHERE isCompleted = 1")
    suspend fun deleteCompletedAutomations()
}