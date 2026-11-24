package az.karabakh.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Back = Color(0xFF0B0912)
private val Panel = Color(0xFF171321)
private val Purple = Color(0xFF9B5CFF)
private val Muted = Color(0xFFAAA3B8)
private val White = Color(0xFFF7F2FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContent { KarabakhApp() }
    }
}

@Composable
private fun KarabakhApp() {
    var page by remember { mutableStateOf("Ana səhifə") }
    var version by remember { mutableStateOf("1.21.1") }
    var ram by remember { mutableFloatStateOf(1.5f) }
    var notice by remember { mutableStateOf("KarabakhLauncher • İlk müstəqil versiya") }
    MaterialTheme(colorScheme = darkColorScheme(background=Back, surface=Panel, primary=Purple, onBackground=White, onSurface=White)) {
        Row(Modifier.fillMaxSize().background(Back).padding(14.dp)) {
            Column(Modifier.width(190.dp).fillMaxHeight().background(Panel, RoundedCornerShape(20.dp)).padding(14.dp), verticalArrangement=Arrangement.SpaceBetween) {
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Purple, RoundedCornerShape(12.dp)), Alignment.Center) { Text("K", color=White, fontSize=23.sp, fontWeight=FontWeight.Black) }
                        Spacer(Modifier.width(9.dp))
                        Column { Text("KARABAKH", fontWeight=FontWeight.Black, fontSize=14.sp); Text("LAUNCHER", color=Purple, fontSize=10.sp, letterSpacing=2.sp) }
                    }
                    Spacer(Modifier.height(12.dp))
                    listOf("Ana səhifə","Versiyalar","Parametrlər","Profil").forEach { item ->
                        val active=page==item
                        Row(Modifier.fillMaxWidth().background(if(active) Purple.copy(alpha=.20f) else Color.Transparent, RoundedCornerShape(12.dp)).clickable { page=item }.padding(horizontal=11.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                            Icon(when(item) { "Ana səhifə"->Icons.Default.Home; "Versiyalar"->Icons.Default.Extension; "Parametrlər"->Icons.Default.Settings; else->Icons.Default.Person }, null, tint=if(active) Purple else Muted, modifier=Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp)); Text(item, color=if(active) White else Muted, fontSize=13.sp, fontWeight=if(active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Text("BUILD 0.1.0 • ANDROID", color=Muted, fontSize=9.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(bottom=12.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                    Column { Text(page.uppercase(), color=Muted, fontSize=10.sp, letterSpacing=2.sp); Text("Salam! Karabakh dünyasına xoş gəldin.", color=White, fontSize=19.sp, fontWeight=FontWeight.Bold) }
                    Row(verticalAlignment=Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(Color(0xFF43D69A), CircleShape)); Spacer(Modifier.width(7.dp)); Text("Tətbiq aktivdir", color=Muted, fontSize=11.sp) }
                }
                when(page) {
                    "Ana səhifə" -> Row(Modifier.weight(1f), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1.5f).fillMaxHeight().background(Panel, RoundedCornerShape(18.dp)).padding(18.dp), verticalArrangement=Arrangement.SpaceBetween) {
                            Column {
                                Text("SƏNİN MACƏRAN", color=Purple, fontSize=10.sp, letterSpacing=2.sp, fontWeight=FontWeight.Bold)
                                Spacer(Modifier.height(8.dp)); Text("Minecraft Java", fontSize=25.sp, fontWeight=FontWeight.Black)
                                Text("Öz oyun mühitini burada idarə et.", color=Muted, fontSize=12.sp)
                                Spacer(Modifier.height(18.dp)); Text("Seçilmiş versiya", color=Muted, fontSize=11.sp); Text(version, fontSize=17.sp, fontWeight=FontWeight.Bold)
                            }
                            Button(onClick={notice="Oyun mühərriki hələ hazırlanır; bu düymə hazırda oyunu başlatmır."}, colors=ButtonDefaults.buttonColors(containerColor=Purple), shape=RoundedCornerShape(12.dp), modifier=Modifier.fillMaxWidth().height(48.dp)) {
                                Icon(Icons.Default.PlayArrow,null); Spacer(Modifier.width(8.dp)); Text("OYUNU BAŞLAT", fontWeight=FontWeight.Bold)
                            }
                        }
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                            InfoCard("OYUN VERSİYASI",version,"Seçilmiş versiya")
                            InfoCard("YADDAŞ","%.1f GB".format(ram),"İnterfeys parametri")
                            InfoCard("LAUNCHER","0.1.0","Müstəqil Android tətbiqi")
                        }
                    }
                    "Versiyalar" -> CardPanel {
                        Text("Minecraft versiyası", fontSize=18.sp, fontWeight=FontWeight.Bold)
                        Text("Versiya seçimi hazırdır. Oyun fayllarının endirilməsi hələ əlavə edilməyib.", color=Muted, fontSize=12.sp)
                        listOf("1.21.1","1.20.1","1.19.4").forEach { v ->
                            Row(Modifier.fillMaxWidth().clickable { version=v; notice="Seçilmiş versiya: $v" }.padding(vertical=9.dp), verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.ChevronRight,null,tint=Purple); Text(v,Modifier.weight(1f)); if(version==v) Text("SEÇİLİB",color=Purple,fontSize=10.sp,fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                    "Parametrlər" -> CardPanel {
                        Text("Yaddaş (RAM)", fontSize=18.sp, fontWeight=FontWeight.Bold)
                        Text("%.1f GB".format(ram), color=Purple, fontSize=24.sp, fontWeight=FontWeight.Bold)
                        Slider(value=ram,onValueChange={ram=it},valueRange=.75f..2f,steps=4)
                        Text("RAM seçimi hələ Java mühərrikinə qoşulmayıb.",color=Muted,fontSize=12.sp)
                    }
                    "Profil" -> CardPanel {
                        Text("Oyunçu profili",fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Text("Karabakh Player",fontSize=22.sp,color=Purple,fontWeight=FontWeight.Bold)
                        Text("Offline profil • Hesab bağlantısı sonrakı mərhələdə",color=Muted,fontSize=12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp)); Text(notice,color=Muted,fontSize=10.sp)
            }
        }
    }
}

@Composable
private fun InfoCard(title:String,value:String,sub:String) {
    Column(Modifier.fillMaxWidth().weight(1f).background(Panel,RoundedCornerShape(16.dp)).padding(15.dp),verticalArrangement=Arrangement.Center) {
        Text(title,color=Muted,fontSize=9.sp,letterSpacing=1.5.sp); Spacer(Modifier.height(6.dp)); Text(value,fontSize=21.sp,fontWeight=FontWeight.Bold); Text(sub,color=Muted,fontSize=10.sp)
    }
}
@Composable
private fun CardPanel(content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().background(Panel,RoundedCornerShape(18.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
