package ch.dulli.pause

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF0F1226)
    val Card = Color(0xFF1A1F3D)
    val Line = Color(0xFF2A3060)
    val Mint = Color(0xFF7CE3C3)
    val Coral = Color(0xFFFF8A7A)
    val Text = Color(0xFFEDEFFB)
    val Sub = Color(0xFF9AA0C3)
}

@Composable
fun PauseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Mint, onPrimary = C.Bg, secondary = C.Coral,
            background = C.Bg, surface = C.Card, onSurface = C.Text, onBackground = C.Text
        )
    ) { Surface(Modifier.fillMaxSize(), color = C.Bg, contentColor = C.Text) { content() } }
}

@Composable
fun Logo(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawCircle(C.Mint, radius = w * 0.44f, style = Stroke(w * 0.07f))
        val bw = w * 0.1f; val bh = w * 0.4f; val top = (w - bh) / 2
        drawRoundRect(C.Mint, Offset(w * 0.37f, top), Size(bw, bh), CornerRadius(bw / 2))
        drawRoundRect(C.Coral, Offset(w * 0.53f, top), Size(bw, bh), CornerRadius(bw / 2))
    }
}

@Composable
fun CardBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(C.Card).padding(18.dp),
        content = content
    )
}

@Composable
fun Title(t: String) = Text(t, fontSize = 13.sp, color = C.Sub, fontWeight = FontWeight.SemiBold,
    modifier = Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp))

@Composable
fun Pills(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.Bg).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                    .background(if (sel) C.Mint else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (sel) C.Bg else C.Sub, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun BigButton(text: String, color: Color = C.Mint, onClick: () -> Unit) {
    Button(
        onClick, Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = C.Bg)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}
