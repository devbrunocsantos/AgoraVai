package com.brunocsantos.agoravai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // Adicione esta importação
import androidx.room.PrimaryKey

@Entity(
    tableName = "etapas",
    foreignKeys = [ForeignKey(
        entity = Compromisso::class,
        parentColumns = ["id"],
        childColumns = ["compromissoId"],
        onDelete = ForeignKey.CASCADE
    )],
    // ADICIONE ESTE BLOCO DE ÍNDICE
    indices = [Index(value = ["compromissoId"])]
)
data class Etapa(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val compromissoId: Long,
    val descricao: String,
    val duracaoMinutos: Int
)