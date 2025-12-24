package com.brunocsantos.agoravai.ui.compromissos

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info // Import necessário
import androidx.compose.material.icons.outlined.Info // Import necessário
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest

@Composable
fun AddressAutocompleteField(
    label: String,
    initialValue: String,
    infoDescription: String? = null, // Novo parâmetro opcional
    onAddressSelected: (String) -> Unit,
    onCurrentLocationClick: () -> Unit
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val token = remember { AutocompleteSessionToken.newInstance() }

    var query by remember { mutableStateOf(initialValue) }
    var predictions by remember { mutableStateOf<List<com.google.android.libraries.places.api.model.AutocompletePrediction>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }

    LaunchedEffect(initialValue) {
        if (query != initialValue) {
            query = initialValue
            isTyping = false
        }
    }

    LaunchedEffect(query) {
        if (isTyping && query.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build()
            placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
                predictions = response.autocompletePredictions
                showSuggestions = true
            }.addOnFailureListener { exception ->
                Log.e("AgoraVai-Debug", "Erro na busca de endereço: ", exception)
                showSuggestions = false
            }
        } else if (!isTyping) {
            // Não faz nada
        } else {
            predictions = emptyList()
            showSuggestions = false
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = {
                isTyping = true
                query = it
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                // Row para comportar múltiplos ícones (Info + GPS)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    // Ícone de Info (Se houver descrição)
                    if (infoDescription != null) {
                        InfoIconWithDialog(title = label, description = infoDescription)
                    }

                    // Ícone de GPS
                    IconButton(onClick = onCurrentLocationClick) {
                        Icon(Icons.Default.GpsFixed, contentDescription = "Usar localização atual")
                    }
                }
            }
        )

        if (showSuggestions && predictions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                shadowElevation = 8.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction.getFullText(null).toString(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val selectedAddress = prediction.getFullText(null).toString()
                                    isTyping = false
                                    query = selectedAddress
                                    onAddressSelected(selectedAddress)
                                    showSuggestions = false
                                    predictions = emptyList()
                                }
                                .padding(16.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}