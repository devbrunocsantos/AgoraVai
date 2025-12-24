package com.brunocsantos.agoravai.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "compromissos")
data class Compromisso(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titulo: String,
    val dataHoraEvento: Long,
    val tempoDeslocamentoMinutos: Int,
    val tempoDeSegurancaMinutos: Int,
    val diasRepeticao: String = "",
    val origem: String,
    val destino: String,

    // Campos de Controle
    val isEnabled: Boolean = true,
    val deleteAfterComplete: Boolean = false,

    // Campos para Geofence e Monitoramento de Atraso
    val latitudeDestino: Double = 0.0,
    val longitudeDestino: Double = 0.0,
    val confirmouSaida: Boolean = false,
    val confirmouChegada: Boolean = false,

    val notificationType: NotificationType = NotificationType.PADRAO
)

data class CompromissoComEtapas(
    @Embedded val compromisso: Compromisso,

    @Relation(
        parentColumn = "id",
        entityColumn = "compromissoId"
    )
    val etapas: List<Etapa>,

    @Relation(
        parentColumn = "id",
        entityColumn = "compromissoId"
    )
    val itensLembrete: List<ItemLembrete>
)