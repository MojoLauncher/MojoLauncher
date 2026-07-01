package com.xl.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xl.launcher.xy.ai.XYAIManager
import com.xl.launcher.xy.ai.XYAIProvider
import com.xl.launcher.xy.ai.XYAIResponse

@Composable
fun AssistantScreen() {
    val provider = remember { XYAIProvider() }
    val manager = remember { XYAIManager(provider) }
    var messages by remember { mutableStateOf(listOf<Pair<String,String>>()) }
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Xai Assistant", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            for ((u, m) in messages) {
                Text("${'$'}u: ${'$'}m", color = Color.White)
            }
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    messages = messages + ("You" to input)
                    manager.submitPrompt(input) { resp: XYAIResponse ->
                        messages = messages + ("Xai" to resp.text)
                    }
                    input = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
