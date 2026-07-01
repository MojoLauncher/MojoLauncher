package com.xl.launcher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xy.runtime.LaunchEngine

class MockMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val engine = remember { LaunchEngine() }
    var logs by remember { mutableStateOf(listOf<String>()) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left: big logo
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_xl_logo),
                contentDescription = "XL Logo",
                modifier = Modifier.size(280.dp)
            )
        }

        // Right: profile column
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(380.dp).background(color = Color(0xFFFFF6CC), shape = RoundedCornerShape(24.dp))) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar placeholder
                    Box(modifier = Modifier.size(96.dp).background(Color.LightGray, shape = RoundedCornerShape(12.dp)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("KIUA", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Play button
                    Spacer(modifier = Modifier.height(30.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(Color.Black, shape = RoundedCornerShape(36.dp))
                            .clickable {
                                // simulate
                                engine.simulateLaunch { m -> logs = logs + m }
                            }, contentAlignment = Alignment.Center) {
                            Text("Play", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Logs:")
                for (l in logs.takeLast(6)) {
                    Text(l, color = Color.White)
                }
            }
        }
    }
}
