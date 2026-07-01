package com.xl.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import xy.runtime.LaunchEngine
import com.xl.launcher.auth.AccountManager

@Composable
fun HomeScreenNav(navController: NavController) {
    val engine = remember { LaunchEngine() }
    var logs by remember { mutableStateOf(listOf<String>()) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left: big logo area
        Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_xl_logo),
                contentDescription = "XL Logo",
                modifier = Modifier.size(320.dp)
            )
        }

        // Right: profile column (matching design)
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(420.dp).background(color = Color(0xFFFFF6CC), shape = RoundedCornerShape(28.dp))) {
                Column(modifier = Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // avatar
                    Box(modifier = Modifier.size(96.dp).background(Color.LightGray, shape = RoundedCornerShape(12.dp)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = AccountManager.githubUserName() ?: "KIUA", fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    // GitHub label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(id = R.drawable.ic_github_marker), contentDescription = "github", tint = Color.Unspecified)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (AccountManager.isGithubLinked()) "GITHUB account" else "Offline account")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Edit Profile (opens profiles)
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(52.dp).background(Color.Black, shape = RoundedCornerShape(26.dp)).clickable { navController.navigate("profiles") }, contentAlignment = Alignment.Center) {
                        Text("Edit Profile", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Version Card (simplified)
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp).background(Color.Black, shape = RoundedCornerShape(18.dp)), contentAlignment = Alignment.CenterStart) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).background(Color.DarkGray, shape = RoundedCornerShape(8.dp)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column { Text("00"); Text("26.2") }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Version", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Large Play button
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(Color.Black, shape = RoundedCornerShape(42.dp))
                            .clickable {
                                engine.simulateLaunch { m -> logs = logs + m }
                            }, contentAlignment = Alignment.Center) {
                            Text("Play", color = Color.White, fontSize = 26.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logs area
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Status:")
                for (l in logs.takeLast(6)) {
                    Text(l, color = Color.White)
                }
            }
        }
    }

    // Floating small robot buttons (left and right)
    Box(modifier = Modifier.fillMaxSize()) {
        // Left bottom - Xai
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).size(64.dp).clickable { navController.navigate("assistant") }) {
            Image(painter = painterResource(id = R.drawable.ic_robot_left), contentDescription = "Xai")
        }

        // Right bottom - Smart Store
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).size(92.dp).clickable { navController.navigate("store") }) {
            Image(painter = painterResource(id = R.drawable.ic_robot_right), contentDescription = "Smart Store")
        }
    }
}
