package com.xl.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xl.launcher.xy.store.StoreRepository
import com.xl.launcher.xy.store.DependencyResolver
import com.xl.launcher.xy.mod.ModSecurityScanner
import com.xl.launcher.xy.mod.ModDoctorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoreScreen() {
    val items = remember { StoreRepository.fetchTrending() }
    val scope = rememberCoroutineScope()
    var installing by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Smart Store", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(items) { it ->
                Box(modifier = Modifier.fillMaxWidth().padding(6.dp).background(Color(0xFF111111), shape = RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(it, color = Color.White)
                            Text("A trending mod", color = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (installing.containsKey(it)) {
                            Text(installing[it] ?: "Installing...", color = Color.Yellow)
                        } else {
                            Button(onClick = {
                                // simulate install
                                scope.launch {
                                    installing = installing + (it to "Scanning")
                                    delay(600)
                                    val safe = ModSecurityScanner.quickCheck(java.io.File("/fake/${it}"))
                                    installing = installing + (it to if (safe) "Installing" else "Blocked")
                                    delay(800)
                                    val deps = DependencyResolver.resolve(listOf("lib-a"))
                                    // run mod doctor
                                    ModDoctorManager.scanDirectory(java.io.File("/fake"))
                                    installing = installing + (it to "Installed")
                                    delay(400)
                                    installing = installing - it
                                }
                            }) { Text("Install") }
                        }
                    }
                }
            }
        }
    }
}
