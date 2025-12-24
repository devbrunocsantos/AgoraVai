package com.brunocsantos.agoravai.data

enum class NotificationType {
    PADRAO,         // Notificação simples (já implementada)
    ALARME,         // Tela cheia estilo despertador (botão único)
    INSISTENTE,     // Tela cheia, exige segurar botão por 3s
    DESAFIO         // Tela cheia, exige digitar uma palavra
}