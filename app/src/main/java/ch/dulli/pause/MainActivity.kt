package ch.dulli.pause

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        setContent { PauseTheme { Home(prefs, tick, ::serviceOn) } }
    }

    override fun onResume() { super.onResume(); tick++ }

    private fun serviceOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(this, GuardService::class.java).flattenToString()
        return s.split(':').any { it.equals(me, ignoreCase = true) }
    }
}

@Composable
fun Home(prefs: Prefs, tick: Int, serviceOn: () -> Boolean) {
    val ctx = LocalContext.current
    var ver by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val gate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) pending?.invoke()
        pending = null; ver++
    }
    // Lockern geht nur über die Bremse
    fun guarded(action: () -> Unit) {
        pending = action
        gate.launch(Intent(ctx, GateActivity::class.java).putExtra("key", "settings"))
    }

    val v = ver + tick
    val active = remember(v) { serviceOn() }
    val on = remember(v) { Rules.all.associate { it.key to prefs.on(it.key) } }
    val blocks = remember(v) { prefs.blocks }
    val unlocks = remember(v) { prefs.unlocks }
    val mode = remember(v) { prefs.mode }
    val maxMin = remember(v) { prefs.maxMin }
    val limit = remember(v) { prefs.dailyLimit }
    val night = remember(v) { prefs.night }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Logo(44.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Pause", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Weniger scrollen. Mehr leben.", color = C.Sub, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(22.dp))

        if (!active) {
            CardBox {
                Text("Schutz ist aus", color = C.Coral, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("Aktiviere »Pause – Scroll-Bremse« unter Bedienungshilfen → Installierte Apps.",
                    color = C.Sub, fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                BigButton("Jetzt aktivieren", C.Coral) {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        CardBox {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("$blocks", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = C.Mint)
                    Text("Reflexe heute gestoppt", color = C.Sub, fontSize = 13.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("$unlocks/$limit", fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        color = if (unlocks >= limit) C.Coral else C.Text)
                    Text("Freischaltungen", color = C.Sub, fontSize = 13.sp)
                }
            }
        }

        Title("NUR DEN ENDLOS-FEED SPERREN")
        CardBox { Rules.all.filter { !it.whole }.forEach { r -> RuleRow(r, on[r.key] == true, ::guarded, prefs) { ver++ } } }

        Title("GANZE APP SPERREN")
        CardBox { Rules.all.filter { it.whole }.forEach { r -> RuleRow(r, on[r.key] == true, ::guarded, prefs) { ver++ } } }

        Title("BREMSE")
        CardBox {
            Text("Bevor du freischalten kannst", fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Pills(listOf("30 s atmen", "60 s atmen", "Rechnen"), mode) { i -> guarded { prefs.mode = i } }
            Spacer(Modifier.height(18.dp))
            Text("Freischaltung hält maximal", fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            val mins = listOf(5, 10, 15)
            Pills(mins.map { "$it min" }, mins.indexOf(maxMin)) { i -> guarded { prefs.maxMin = mins[i] } }
            Spacer(Modifier.height(18.dp))
            Text("Freischaltungen pro Tag", fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            val lims = listOf(1, 3, 5)
            Pills(lims.map { "$it×" }, lims.indexOf(limit)) { i -> guarded { prefs.dailyLimit = lims[i] } }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nachtruhe 23–7 Uhr", fontSize = 15.sp)
                    Text("Nachts keine Freischaltung möglich", color = C.Sub, fontSize = 13.sp)
                }
                Switch(night, { new ->
                    if (new) { prefs.night = true; ver++ } else guarded { prefs.night = false }
                }, colors = SwitchDefaults.colors(checkedTrackColor = C.Mint, checkedThumbColor = C.Bg))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Lockern geht nur über die Bremse – damit du dich nicht selbst austrickst.",
            color = C.Sub, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp))
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RuleRow(r: Rule, checked: Boolean, guarded: (() -> Unit) -> Unit, prefs: Prefs, changed: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(r.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(r.sub, color = C.Sub, fontSize = 13.sp)
        }
        Switch(checked, { new ->
            if (new) { prefs.set(r.key, true); changed() } else guarded { prefs.set(r.key, false) }
        }, colors = SwitchDefaults.colors(checkedTrackColor = C.Mint, checkedThumbColor = C.Bg))
    }
}
