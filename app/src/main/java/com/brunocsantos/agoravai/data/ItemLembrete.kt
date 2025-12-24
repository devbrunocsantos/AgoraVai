package com.brunocsantos.agoravai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_lembrete",
    foreignKeys = [ForeignKey(
        entity = Compromisso::class,
        parentColumns = ["id"],
        childColumns = ["compromissoId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["compromissoId"])]
)
data class ItemLembrete(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val compromissoId: Long,
    val nomeItem: String
)