package ch.dulli.pause

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class GateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.getStringExtra("key") ?: "settings"
        val rule = Rules.all.find { it.key == key }
        val prefs = Prefs(this)
        setContent {
            PauseTheme {
                GateScreen(rule, prefs,
                    onLeave = {
                        if (rule?.whole == true) startActivity(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        setResult(RESULT_CANCELED); finish()
                    },
                    onDone = { min ->
                        if (rule == null) { setResult(RESULT_OK); finish(); return@GateScreen }
                        prefs.unlock(rule.key, min)
                        if (rule.whole) rule.pkgs.firstNotNullOfOrNull {
                            packageManager.getLaunchIntentForPackage(it)
                        }?.let { startActivity(it) }
                        finish()
                    })
            }
        }
    }
}

private val tips = listOf(
    "Trink ein Glas Wasser.", "Mach 10 Liegestütze.", "Geh 5 Minuten raus.",
    "Schreib jemandem, den du magst.", "Leg das Handy in einen anderen Raum.",
    "Streck dich einmal richtig durch.", "Räum eine Sache auf."
)

@Composable
fun GateScreen(rule: Rule?, prefs: Prefs, onLeave: () -> Unit, onDone: (Int) -> Unit) {
    val settings = rule == null
    val blocked = remember {
        when {
            prefs.isNight() -> "Nachtruhe bis 07:00.\nMorgen ist auch noch Zeit."
            !settings && prefs.unlocks >= prefs.dailyLimit ->
                "Tageslimit erreicht (${prefs.dailyLimit}/${prefs.dailyLimit}).\nFür heute ist Schluss – gut so."
            else -> null
        }
    }
    var phase by remember { mutableIntStateOf(if (blocked != null) -1 else 0) }
    var reason by remember { mutableStateOf("") }
    val tip = remember { tips.random() }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Logo(56.dp)
        Spacer(Modifier.height(20.dp))
        Text("Kurz innehalten.", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (settings) "Du willst eine Sperre lockern." else "Du wolltest gerade ${rule!!.title} öffnen.",
            color = C.Sub, fontSize = 15.sp, textAlign = TextAlign.Center
        )
        Text("Heute schon ${prefs.blocks}× gestoppt.", color = C.Mint, fontSize = 14.sp)

        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            when (phase) {
                -1 -> Text(blocked!!, fontSize = 20.sp, textAlign = TextAlign.Center, lineHeight = 28.sp)
                0 -> {
                    Text("Warum jetzt?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    listOf("Langeweile", "Aus Gewohnheit", "Ich will etwas Bestimmtes").forEach {
                        Choice(it) { reason = it; phase = 1 }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                1 -> {
                    if (reason != "Ich will etwas Bestimmtes") {
                        Text("Idee statt Scrollen: $tip", color = C.Mint, fontSize = 15.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                    }
                    if (prefs.mode == 2) MathBrake { phase = 2 } else BreathBrake(prefs.waitSec) { phase = 2 }
                }
                2 -> {
                    if (settings) {
                        BigButton("Änderung übernehmen", C.Coral) { onDone(0) }
                    } else {
                        Text("Wie lange?", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("Freischaltung ${prefs.unlocks + 1} von ${prefs.dailyLimit} heute",
                            color = C.Sub, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        listOf(5, 10, 15).filter { it <= prefs.maxMin }.forEach { m ->
                            Choice("$m Minuten") { onDone(m) }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
        BigButton("Zurück – ich brauch das nicht", onClick = onLeave)
    }
}

@Composable
private fun Choice(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Card)
            .border(1.dp, C.Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(16.dp), contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 16.sp) }
}

@Composable
private fun BreathBrake(seconds: Int, done: () -> Unit) {
    var left by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        done()
    }
    val t = rememberInfiniteTransition(label = "b")
    val s by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().scale(s).clip(CircleShape).background(C.Mint.copy(alpha = 0.18f)))
        Box(Modifier.size(90.dp).scale(s).clip(CircleShape).background(C.Mint.copy(alpha = 0.5f)))
        Text("$left", fontSize = 34.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
    Text("Atme mit dem Kreis. Ein … und aus.", color = C.Sub, fontSize = 15.sp)
}

@Composable
private fun MathBrake(done: () -> Unit) {
    var a by remember { mutableIntStateOf((13..39).random()) }
    var b by remember { mutableIntStateOf((6..9).random()) }
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    Text("$a × $b = ?", fontSize = 36.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        input, { input = it.filter(Char::isDigit).take(4); wrong = false },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = wrong, shape = RoundedCornerShape(16.dp), modifier = Modifier.width(180.dp)
    )
    if (wrong) Text("Falsch – neue Aufgabe.", color = C.Coral, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
    TextButton({
        if (input.toIntOrNull() == a * b) done()
        else { wrong = true; input = ""; a = (13..39).random(); b = (6..9).random() }
    }) { Text("Prüfen", color = C.Mint, fontSize = 16.sp) }
}
