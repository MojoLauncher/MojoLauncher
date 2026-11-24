package az.karabakh.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val Back=Color(0xFF0B0912); private val Panel=Color(0xFF171321)
private val Purple=Color(0xFF9B5CFF); private val Muted=Color(0xFFAAA3B8); private val White=Color(0xFFF7F2FF)
private data class McVersion(val id:String,val type:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; setContent{KarabakhApp()}}
}

@Composable private fun KarabakhApp(){
 val ctx= LocalContext.current
 var page by remember{mutableStateOf("Ana səhifə")}
 var version by remember{mutableStateOf("1.21.1")}
 var ram by remember{mutableFloatStateOf(1.5f)}
 var notice by remember{mutableStateOf("KarabakhLauncher 0.2.0")}
 var versions by remember{mutableStateOf<List<McVersion>>(emptyList())}
 var loading by remember{mutableStateOf(false)}
 var query by remember{mutableStateOf("")}
 var snapshots by remember{mutableStateOf(false)}
 var mods by remember{mutableStateOf(listOf<String>())}
 var accountMode by remember{mutableStateOf("Login")}
 var adminName by remember{mutableStateOf("")}
 var adminEmail by remember{mutableStateOf("")}
 var adminCode by remember{mutableStateOf("")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){ uri:Uri? ->
  if(uri!=null){runCatching{ctx.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}; val n=uri.lastPathSegment?.substringAfterLast('/')?: "Mod.jar"; mods=(mods+n).distinct(); notice="Mod siyahıya əlavə edildi: "+n}
 }
 LaunchedEffect(page){if(page=="Versiyalar"&&versions.isEmpty()&&!loading){loading=true;try{versions=withContext(Dispatchers.IO){val a=JSONObject(URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").readText()).getJSONArray("versions");buildList{for(i in 0 until a.length()){val x=a.getJSONObject(i);add(McVersion(x.getString("id"),x.getString("type")))}}};notice="Rəsmi Minecraft versiya siyahısı yükləndi."}catch(e:Exception){notice="Versiyalar yüklənmədi. İnterneti yoxla."}finally{loading=false}}}
 MaterialTheme(colorScheme=darkColorScheme(background=Back,surface=Panel,primary=Purple,onSurface=White)){
  Row(Modifier.fillMaxSize().background(Back).padding(12.dp)){
   Column(Modifier.width(175.dp).fillMaxHeight().background(Panel,RoundedCornerShape(18.dp)).padding(12.dp),verticalArrangement=Arrangement.SpaceBetween){
    Column{
     Text("KARABAKH",fontWeight=FontWeight.Black,fontSize=19.sp,color=White);Text("LAUNCHER",color=Purple,letterSpacing=3.sp)
     Spacer(Modifier.height(16.dp))
     listOf("Ana səhifə","Versiyalar","Modlar","Hesab","Parametrlər").forEach{p->
      Text(p,Modifier.fillMaxWidth().background(if(page==p)Purple.copy(alpha=.2f) else Color.Transparent,RoundedCornerShape(10.dp)).clickable{page=p}.padding(12.dp),color=if(page==p)White else Muted,fontWeight=if(page==p)FontWeight.Bold else FontWeight.Normal)
     }
    }
    Text("BUILD 0.2.0",color=Muted,fontSize=10.sp)
   }
   Spacer(Modifier.width(12.dp))
   Column(Modifier.weight(1f).fillMaxHeight()){
    Text(page.uppercase(),color=Muted,fontSize=10.sp,letterSpacing=2.sp);Text("KarabakhLauncher",fontSize=20.sp,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    when(page){
     "Ana səhifə"->CardPanel{
      Text("Minecraft Java",fontSize=25.sp,fontWeight=FontWeight.Black)
      Text("Seçilmiş versiya: "+version,color=Purple)
      Text("Mod sayı: "+mods.size,color=Muted)
      Button(onClick={notice="Minecraft mühərriki hələ əlavə edilməyib."}){Text("OYUNU BAŞLAT")}
     }
     "Versiyalar"->CardPanel{
      Text("Bütün Minecraft versiyaları",fontSize=18.sp,fontWeight=FontWeight.Bold)
      OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Axtar")})
      Row(verticalAlignment=Alignment.CenterVertically){
       FilterChip(selected=!snapshots,onClick={snapshots=false},label={Text("Release")})
       Spacer(Modifier.width(6.dp));FilterChip(selected=snapshots,onClick={snapshots=true},label={Text("Snapshot / digər")})
       Spacer(Modifier.width(8.dp));if(loading)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)
      }
      val list=versions.filter{(if(snapshots)it.type!="release" else it.type=="release")&&it.id.contains(query,true)}
      LazyColumn(Modifier.fillMaxSize()){items(list){v->
       Row(Modifier.fillMaxWidth().clickable{version=v.id;notice="Seçildi: "+v.id}.padding(7.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){Text(v.id,fontWeight=FontWeight.Bold);Text(v.type,color=Muted,fontSize=10.sp)}
        if(version==v.id)Text("SEÇİLİB",color=Purple,fontSize=10.sp)
       };HorizontalDivider(color=Muted.copy(alpha=.15f))
      }}
     }
     "Modlar"->CardPanel{
      Text("Mod meneceri",fontSize=18.sp,fontWeight=FontWeight.Bold)
      Text("JAR mod fayllarını telefondan seç. Uyğun Fabric / Forge loader tələb olunur.",color=Muted,fontSize=12.sp)
      Button(onClick={picker.launch(arrayOf("application/java-archive","application/octet-stream","*/*"))}){Icon(Icons.Default.Add,null);Text("MOD JAR ƏLAVƏ ET")}
      LazyColumn(Modifier.fillMaxSize()){items(mods){m->Row(verticalAlignment=Alignment.CenterVertically){
       Text(m,Modifier.weight(1f),fontSize=12.sp);IconButton(onClick={mods=mods-m}){Icon(Icons.Default.Delete,null)}
      }}}
     }
     "Hesab"->CardPanel{
      Text("Hesab mərkəzi",fontSize=18.sp,fontWeight=FontWeight.Bold)
      Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
       listOf("Login","Register","Admin Register").forEach{mode->
        FilterChip(selected=accountMode==mode,onClick={accountMode=mode},label={Text(mode,fontSize=11.sp)})
       }
      }
      when(accountMode){
       "Login"->{
        Text("Microsoft / Minecraft hesabına giriş",fontWeight=FontWeight.Bold)
        Text("Real launcher girişi hələ aktiv deyil. Təhlükəsiz Microsoft OAuth inteqrasiyası tələb olunur.",color=Muted,fontSize=12.sp)
        Button(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.microsoft.com/")))}){Text("MICROSOFT LOGIN")}
       }
       "Register"->{
        Text("Yeni Minecraft hesabı yarat",fontWeight=FontWeight.Bold)
        Text("Qeydiyyat yalnız rəsmi Microsoft səhifəsində aparılır.",color=Muted,fontSize=12.sp)
        Button(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.microsoft.com/")))}){Text("REGISTER")}
        OutlinedButton(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.minecraft.net/msaprofile")))}){Text("MINECRAFT PROFİLİ")}
       }
       else->{
        Text("Admin qeydiyyatı",fontWeight=FontWeight.Bold)
        Text("Admin olmaq üçün server sahibinin verdiyi dəvət kodu lazımdır. Bu forma hələ serverə sorğu göndərmir və kodu təsdiqləmir.",color=Muted,fontSize=12.sp)
        OutlinedTextField(adminName,{adminName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("İstifadəçi adı")})
        OutlinedTextField(adminEmail,{adminEmail=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("E-poçt")})
        OutlinedTextField(adminCode,{adminCode=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Admin dəvət kodu")})
        Button(onClick={notice=if(adminName.isBlank()||adminEmail.isBlank()||adminCode.isBlank()) "Bütün xanaları doldur." else "Forma hazırdır. Admin təsdiqi üçün backend/server inteqrasiyası lazımdır."}){Text("ADMIN QEYDİYYAT SORĞUSU")}
       }
      }
      Text("Şifrənı bu tətbiqə yazma; hesab təhlükəsizliyi rəsmi Microsoft girişindən istifadə etməlidir.",color=Muted,fontSize=11.sp)
     }
     else->CardPanel{
      Text("Yaddaş (RAM)",fontSize=18.sp,fontWeight=FontWeight.Bold);Text("%.1f GB".format(ram),color=Purple,fontSize=23.sp)
      Slider(value=ram,onValueChange={ram=it},valueRange=.75f..2f,steps=4)
      Text("RAM seçimi mühərrikə hələ qoşulmayıb.",color=Muted,fontSize=12.sp)
     }
    }
    Spacer(Modifier.height(5.dp));Text(notice,color=Muted,fontSize=10.sp,maxLines=2)
   }
  }
 }
}
@Composable private fun CardPanel(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().background(Panel,RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)}
