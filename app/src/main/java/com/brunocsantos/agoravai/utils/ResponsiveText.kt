package com.brunocsantos.agoravai.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ResponsiveText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    var textStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        style = textStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Clip, // Importante para não mostrar "..." enquanto calcula
        softWrap = false,
        color = color,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                // Se não couber, reduz a fonte em 10%
                if (textStyle.fontSize.value > 12) { // Limite mínimo de segurança
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9f)
                } else {
                    readyToDraw = true // Desiste e desenha mesmo cortado se ficar muito pequeno
                }
            } else {
                readyToDraw = true
            }
        }
    )
}