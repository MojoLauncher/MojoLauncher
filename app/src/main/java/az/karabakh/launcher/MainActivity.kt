package az.karabakh.launcher

import android.graphics.BitmapFactory
import android.util.Base64
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private val Back=Color(0xFF090A0F)
private val Panel=Color(0xFF141720)
private val Purple=Color(0xFF9B5CFF)
private val Green=Color(0xFF65D69A)
private val Muted=Color(0xFF9CA3B4)
private val White=Color(0xFFF5F6FA)
private data class McVersion(val id:String,val type:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
  setContent{KarabakhApp()}
 }
}

@Composable private fun KarabakhApp(){
 val ctx=LocalContext.current
 val logoBitmap = remember {
  runCatching {
   val encoded = ctx.assets.open("karabakh_logo.webp.b64").bufferedReader().use { it.readText() }
   val bytes = Base64.decode(encoded, Base64.DEFAULT)
   BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
  }.getOrNull()
 }
 var page by remember{mutableStateOf("Ana səhifə")}
 var version by remember{mutableStateOf("1.21.1")}
 var ram by remember{mutableFloatStateOf(1.5f)}
 var notice by remember{mutableStateOf("KarabakhLauncher • Welcome back")}
 var versions by remember{mutableStateOf<List<McVersion>>(emptyList())}
 var loading by remember{mutableStateOf(false)}
 var query by remember{mutableStateOf("")}
 var snapshots by remember{mutableStateOf(false)}
 var accountMode by remember{mutableStateOf("Login")}
 var username by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 var email by remember{mutableStateOf("")}
 var adminName by remember{mutableStateOf("")}
 var adminCode by remember{mutableStateOf("")}
 var ownerName by remember{mutableStateOf("")}
 var ownerPassword by remember{mutableStateOf("")}
 var logoTapCount by remember{mutableIntStateOf(0)}
 var modSearch by remember{mutableStateOf("")}
 LaunchedEffect(page){
  if(page=="Versiyalar"&&versions.isEmpty()&&!loading){
   loading=true
   try{
    versions=withContext(Dispatchers.IO){
     val a=JSONObject(URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json").readText()).getJSONArray("versions")
     buildList{for(i in 0 until a.length()){val x=a.getJSONObject(i);add(McVersion(x.getString("id"),x.getString("type")))}}
    }
    notice="Minecraft versiyaları yükləndi."
   }catch(e:Exception){notice="Versiyalar yüklənmədi. İnterneti yoxla."}
   finally{loading=false}
  }
 }
 MaterialTheme(colorScheme=darkColorScheme(background=Back,surface=Panel,primary=Purple,onSurface=White)){
  Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Back,Color(0xFF101522)))).padding(12.dp)){
   Column(Modifier.width(190.dp).fillMaxHeight().background(Panel,RoundedCornerShape(18.dp)).padding(12.dp),verticalArrangement=Arrangement.SpaceBetween){
    Column{
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
      Box(Modifier.size(48.dp).background(Color.Black,RoundedCornerShape(12.dp)).clickable{logoTapCount++;if(logoTapCount>=7){page="Qurucu panel";logoTapCount=0}},contentAlignment=Alignment.Center){
       if(logoBitmap!=null) Image(bitmap=logoBitmap,contentDescription="KarabakhLauncher loqosu",modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
       else Text("K",fontSize=29.sp,fontWeight=FontWeight.Black,color=White)
      }
      Column{Text("KARABAKH",fontWeight=FontWeight.Black,fontSize=17.sp,color=White);Text("JAVA LAUNCHER",color=Purple,letterSpacing=2.sp,fontSize=9.sp)}
     }
     Spacer(Modifier.height(22.dp))
     listOf("Ana səhifə","Versiyalar","Modlar","Modpack-lər","Hesab","Parametrlər").forEach{p->
      Row(Modifier.fillMaxWidth().padding(vertical=2.dp).background(if(page==p)Purple.copy(alpha=.19f) else Color.Transparent,RoundedCornerShape(10.dp)).clickable{page=p}.padding(horizontal=10.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){
       Text(p,color=if(page==p)White else Muted,fontWeight=if(page==p)FontWeight.Bold else FontWeight.Medium,fontSize=13.sp)
      }
     }
    }
    Text("KARABAKH • BETA",color=Muted,fontSize=9.sp,letterSpacing=1.5.sp)
   }
   Spacer(Modifier.width(13.dp))
   Column(Modifier.weight(1f).fillMaxHeight()){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Column{Text("MINECRAFT JAVA",color=Muted,fontSize=9.sp,letterSpacing=2.sp);Text(page,fontSize=22.sp,fontWeight=FontWeight.Bold)}
     Text("ONLINE • READY",color=Green,fontSize=10.sp,fontWeight=FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))
    when(page){
     "Ana səhifə"->CardPanel{
      Box(Modifier.fillMaxWidth().weight(1f).background(Brush.linearGradient(listOf(Color(0xFF28203E),Color(0xFF141923))),RoundedCornerShape(16.dp)).padding(22.dp)){
       Column(Modifier.align(Alignment.CenterStart),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("YOUR WORLD. YOUR RULES.",color=Purple,fontSize=10.sp,letterSpacing=2.sp,fontWeight=FontWeight.Bold)
        Text("KarabakhLauncher",fontSize=31.sp,fontWeight=FontWeight.Black)
        Text("Minecraft Java üçün yeni başlanğıc.",color=Muted,fontSize=13.sp)
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
         Text("VERSİYA  $version",color=White,fontWeight=FontWeight.Bold)
         Text("•",color=Purple)
         Text("RAM  %.1f GB".format(ram),color=Muted)
        }
        Button(onClick={notice="Minecraft oyun mühərriki hələ inteqrasiya edilməyib."}){Text("OYUNU BAŞLAT")}
       }
      }
      Text("SÜRƏTLİ KEÇİD",color=Muted,fontSize=10.sp,letterSpacing=2.sp)
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       OutlinedButton(onClick={page="CurseForge"},modifier=Modifier.weight(1f)){Text("CurseForge")}
       OutlinedButton(onClick={page="Modrinth"},modifier=Modifier.weight(1f)){Text("Modrinth")}
       OutlinedButton(onClick={page="Versiyalar"},modifier=Modifier.weight(1f)){Text("Versiyalar")}
      }
     }
     "Versiyalar"->CardPanel{
      Text("Minecraft versiyaları",fontSize=18.sp,fontWeight=FontWeight.Bold)
      OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Versiya axtar")})
      Row(verticalAlignment=Alignment.CenterVertically){
       FilterChip(selected=!snapshots,onClick={snapshots=false},label={Text("Release")})
       Spacer(Modifier.width(6.dp));FilterChip(selected=snapshots,onClick={snapshots=true},label={Text("Snapshot / digər")})
       Spacer(Modifier.width(8.dp));if(loading)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)
      }
      LazyColumn(Modifier.fillMaxSize()){items(versions.filter{(if(snapshots)it.type!="release" else it.type=="release")&&it.id.contains(query,true)}){v->
       Row(Modifier.fillMaxWidth().clickable{version=v.id;notice="Seçildi: "+v.id}.padding(8.dp),verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){Text(v.id,fontWeight=FontWeight.Bold);Text(v.type,color=Muted,fontSize=10.sp)}
        if(version==v.id)Text("SEÇİLİB",color=Green,fontSize=10.sp)
       };HorizontalDivider(color=Muted.copy(alpha=.15f))
      }}
     }
     "Modlar"->ModHubPanel(modSearch,{modSearch=it},false)
     "Modpack-lər"->ModHubPanel(modSearch,{modSearch=it},true)
     "Hesab"->CardPanel{
      Text("Hesab mərkəzi",fontSize=19.sp,fontWeight=FontWeight.Bold)
      Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
       listOf("Login","Register","Admin Register").forEach{m->FilterChip(selected=accountMode==m,onClick={accountMode=m},label={Text(m,fontSize=11.sp)})}
      }
      when(accountMode){
       "Login"->{
        Text("Hesabına daxil ol",fontWeight=FontWeight.Bold)
        OutlinedTextField(username,{username=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("İstifadəçi adı")})
        OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("Şifrə")})
        Button(onClick={notice="Login üçün təhlükəsiz server autentifikasiyası qoşulmalıdır."}){Text("DAXİL OL")}
       }
       "Register"->{
        Text("Yeni launcher hesabı yarat",fontWeight=FontWeight.Bold)
        OutlinedTextField(username,{username=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("İstifadəçi adı")})
        OutlinedTextField(email,{email=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("E-poçt")})
        OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("Şifrə")})
        Button(onClick={notice="Qeydiyyat forması hazırdır; hesab yaratmaq üçün backend lazımdır."}){Text("QEYDİYYAT")}
       }
       else->{
        Text("Admin olmaq üçün müraciət",fontWeight=FontWeight.Bold)
        OutlinedTextField(adminName,{adminName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("İstifadəçi adı")})
        OutlinedTextField(adminCode,{adminCode=it},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("Dəvət kodu")})
        Button(onClick={notice="Admin müraciəti serverə göndərilmir: backend qoşulmayıb."}){Text("QURUCUYA MÜRACİƏT GÖNDƏR")}
       }
      }
      Text("Qeyd: bu ekranların real hesab yaratması üçün server bağlantısı hələ qurulmayıb.",color=Muted,fontSize=11.sp)
     }
     "Admin panel"->CardPanel{
      Text("ADMIN PANEL",fontSize=19.sp,fontWeight=FontWeight.Bold)
      Text("Admin alətləri",color=Purple,fontWeight=FontWeight.Bold)
      listOf("Oyunçuların siyahısı","Dəstək müraciətləri","Server elanları","Admin icazələri").forEach{Text("•  $it",color=White,modifier=Modifier.fillMaxWidth().background(Back,RoundedCornerShape(9.dp)).padding(12.dp))}
      Text("Admin hüquqları yalnız Qurucunun server təsdiqindən sonra aktiv olmalıdır.",color=Muted,fontSize=11.sp)
     }
     "Qurucu panel"->CardPanel{
      Text("QURUCU PANELİ",fontSize=20.sp,fontWeight=FontWeight.Black,color=Purple)
      Text("Gizli menyu • yalnız Qurucu hesabı üçün nəzərdə tutulub.",color=Muted,fontSize=12.sp)
      OutlinedTextField(ownerName,{ownerName=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Qurucu istifadəçi adı")})
      OutlinedTextField(ownerPassword,{ownerPassword=it},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("Şifrə")})
      Button(onClick={notice="Qurucu girişi serverə qoşulmayıb. Bu forma hələ heç kimi autentifikasiya etmir."}){Text("QURUCU KİMİ DAXİL OL")}
      HorizontalDivider(color=Muted.copy(alpha=.2f))
      Text("İDARƏETMƏ",fontWeight=FontWeight.Bold)
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       OutlinedButton(onClick={page="Admin panel"}){Text("Admin paneli")}
       OutlinedButton(onClick={page="Versiyalar"}){Text("Versiyaları idarə et")}
      }
      Text("PANEL ALƏTLƏRİ",fontWeight=FontWeight.Bold)
      listOf("Versiya əlavə et / sil","Yeni menyu bölməsi yarat","İstifadəçi və admin icazələri","Mod və modpack kataloqu").forEach{Text("•  $it",color=White,modifier=Modifier.fillMaxWidth().background(Back,RoundedCornerShape(9.dp)).padding(10.dp))}
      Text("Bu idarəetmə əməliyyatları server bağlantısı qurulduqdan sonra aktiv olacaq.",color=Muted,fontSize=11.sp)
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
       OutlinedButton(onClick={notice="Təsdiq funksiyası backend qoşulandan sonra işləyəcək."}){Text("Təsdiqlə")}
       OutlinedButton(onClick={notice="Rədd etmə funksiyası backend qoşulandan sonra işləyəcək."}){Text("Rədd et")}
      }
     }
     else->CardPanel{
      Text("Parametrlər",fontSize=19.sp,fontWeight=FontWeight.Bold)
      Text("Yaddaş (RAM)",color=Muted)
      Text("%.1f GB".format(ram),color=Purple,fontSize=24.sp,fontWeight=FontWeight.Bold)
      Slider(value=ram,onValueChange={ram=it},valueRange=.75f..2f,steps=4)
      Text("RAM seçimi oyun mühərriki qoşulandan sonra tətbiq ediləcək.",color=Muted,fontSize=11.sp)
     }
    }
    Spacer(Modifier.height(5.dp));Text(notice,color=Muted,fontSize=10.sp,maxLines=2)
   }
  }
 }
}

@Composable private fun ModHubPanel(query:String,onQuery:(String)->Unit,modpacks:Boolean){
 val ctx=LocalContext.current
 val title=if(modpacks)"Modpack kataloqu" else "Mod kataloqu"
 CardPanel{
  Text(title,fontSize=24.sp,fontWeight=FontWeight.Black)
  Text("CurseForge və Modrinth • Minecraft Java",color=Muted,fontSize=12.sp)
  OutlinedTextField(query,onQuery,modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text(if(modpacks)"Modpack axtar" else "Mod axtar")},leadingIcon={Icon(Icons.Filled.Search,null)})
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.weight(1f).background(Color(0xFF211B31),RoundedCornerShape(14.dp)).clickable{
    val base=if(modpacks)"https://www.curseforge.com/minecraft/search?class=modpacks" else "https://www.curseforge.com/minecraft/search?class=mc-mods"
    ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(base+"&search="+Uri.encode(query))))
   }.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("CF",fontSize=27.sp,fontWeight=FontWeight.Black,color=Color(0xFFFF9A45));Text("CurseForge",fontWeight=FontWeight.Bold);Text("Modlar və modpack-lər",fontSize=11.sp,color=Muted);Text("Kataloqa bax  →",fontSize=11.sp,color=Purple)
   }
   Column(Modifier.weight(1f).background(Color(0xFF172A2A),RoundedCornerShape(14.dp)).clickable{
    val base=if(modpacks)"https://modrinth.com/modpacks" else "https://modrinth.com/mods"
    ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(base+"?query="+Uri.encode(query))))
   }.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("M",fontSize=27.sp,fontWeight=FontWeight.Black,color=Color(0xFF58D69A));Text("Modrinth",fontWeight=FontWeight.Bold);Text("Açıq mod kataloqu",fontSize=11.sp,color=Muted);Text("Kataloqa bax  →",fontSize=11.sp,color=Green)
   }
  }
  Text("Axtarış hər iki kataloqda seçdiyin sözü açır. Modları birbaşa oyuna quraşdırmaq üçün launcher mühərriki və fayl inteqrasiyası ayrıca tamamlanmalıdır.",color=Muted,fontSize=11.sp)
 }
}

@Composable private fun ModSourcePanel(title:String,subtitle:String,url:String,button:String){
 val ctx=LocalContext.current
 CardPanel{
  Text(title,fontSize=24.sp,fontWeight=FontWeight.Black)
  Text(subtitle,color=Muted)
  Box(Modifier.fillMaxWidth().weight(1f).background(Brush.linearGradient(listOf(Color(0xFF24233B),Color(0xFF121722))),RoundedCornerShape(15.dp)).padding(22.dp),contentAlignment=Alignment.Center){
   Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text(if(title=="CurseForge")"CF" else "M",fontSize=46.sp,fontWeight=FontWeight.Black,color=Purple)
    Text("$title Minecraft",fontSize=21.sp,fontWeight=FontWeight.Bold)
    Text("Modları və modpack-ləri kataloqda araşdır.",color=Muted)
    Button(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}){Text(button)}
   }
  }
  Text("Kataloq hazırda rəsmi saytda açılır. Birbaşa launcher daxilində mod quraşdırılması ayrıca inteqrasiya tələb edir.",color=Muted,fontSize=11.sp)
 }
}

@Composable private fun CardPanel(content:@Composable ColumnScope.()->Unit){
 Column(Modifier.fillMaxSize().background(Panel,RoundedCornerShape(18.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
}
