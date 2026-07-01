package com.xl.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xl.launcher.auth.AccountManager

@Composable
fun ProfilesScreen() {
    var linked by remember { mutableStateOf(AccountManager.isGithubLinked()) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Profiles", color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(Color(0xFF222222), shape = RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = AccountManager.githubUserName() ?: "Offline User")
                Spacer(modifier = Modifier.height(8.dp))
                if (!linked) {
                    Button(onClick = {
                        // Mock GitHub sign in - create a fake user
                        AccountManager.linkGithub("kiua_github")
                        linked = true
                    }) { Text("Sign in with GitHub") }
                } else {
                    Button(onClick = {
                        AccountManager.unlinkGithub()
                        linked = false
                    }) { Text("Sign out GitHub") }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (AccountManager.isGithubLinked()) {
            Text("GitHub-connected features enabled: Upload to Smart Store")
        } else {
            Text("Connect GitHub to enable uploads to Smart Store")
        }
    }
}
