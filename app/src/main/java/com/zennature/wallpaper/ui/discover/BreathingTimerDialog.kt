package com.zennature.wallpaper.ui.discover

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zennature.wallpaper.ui.components.BodyText
import com.zennature.wallpaper.ui.components.IconSquareButton
import com.zennature.wallpaper.ui.components.InverseButton
import com.zennature.wallpaper.ui.components.OutlineButton
import com.zennature.wallpaper.ui.components.PrimaryButton
import com.zennature.wallpaper.ui.components.SerifText
import com.zennature.wallpaper.ui.theme.Zen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

private const val TOTAL = 60
private const val CYCLE = 15
private const val INHALE = 4
private const val HOLD = 4
private const val EXHALE = 4

private enum class Phase { INHALE, HOLD, EXHALE, REST }

/** Zen Moments: 60 s box breathing (ZenBreathingTimer.tsx), 4 cycles of 4/4/4/3. */
@Composable
fun BreathingTimerDialog(onClose: () -> Unit, onStartBuilding: () -> Unit) {
    val c = Zen.colors
    var secondsLeft by rememberSaveable { mutableIntStateOf(TOTAL) }
    var active by rememberSaveable { mutableStateOf(true) }
    var sound by rememberSaveable { mutableStateOf(false) }
    val completed = secondsLeft == 0
    val scope = rememberCoroutineScope()
    val chime: (Double) -> Unit = { freq -> if (sound) scope.launch(Dispatchers.Default) { playChime(freq) } }

    LaunchedEffect(active, completed) {
        while (active && !completed) {
            delay(1000)
            secondsLeft = (secondsLeft - 1).coerceAtLeast(0)
            if (secondsLeft == 0) {
                active = false
                chime(528.0)
            }
        }
    }

    val elapsed = TOTAL - secondsLeft
    val cycleIndex = min(elapsed / CYCLE + 1, 4)
    val t = elapsed % CYCLE
    val phase = when {
        completed -> Phase.REST
        t < INHALE -> Phase.INHALE
        t < INHALE + HOLD -> Phase.HOLD
        t < INHALE + HOLD + EXHALE -> Phase.EXHALE
        else -> Phase.REST
    }
    val (title, subtitle) = when {
        completed -> "Mind Clear & Centered" to "Carry this quiet focus into your wallpaper creation."
        phase == Phase.INHALE -> "Breathe In Slowly" to "Expand awareness and invite stillness"
        phase == Phase.HOLD -> "Hold in Stillness" to "Rest quietly in full presence"
        phase == Phase.EXHALE -> "Exhale Gently" to "Release tension, clutter, and distractions"
        else -> "Pause in Quiet" to "Remain centered before the next breath"
    }
    val targetScale = when {
        completed -> 1f
        phase == Phase.INHALE -> 0.9f + (t + 0.5f) / INHALE * 0.45f
        phase == Phase.HOLD -> 1.35f
        phase == Phase.EXHALE -> 1.35f - (t - INHALE - HOLD + 0.5f) / EXHALE * 0.45f
        else -> 0.9f
    }
    val glowAlpha = when (phase) {
        Phase.INHALE -> 0.45f
        Phase.HOLD -> 0.4f
        Phase.EXHALE -> 0.2f
        Phase.REST -> if (completed) 0.25f else 0.15f
    }
    val scale by animateFloatAsState(targetScale, tween(1000), label = "orb")
    val progress by animateFloatAsState(elapsed / TOTAL.toFloat(), tween(1000, easing = LinearEasing), label = "ring")

    var lastPhase by remember { mutableStateOf(phase) }
    LaunchedEffect(phase) {
        if (active && !completed && phase != lastPhase) {
            if (phase == Phase.INHALE) chime(432.0)
            if (phase == Phase.EXHALE) chime(360.0)
        }
        lastPhase = phase
    }

    fun restart() {
        secondsLeft = TOTAL
        active = true
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 512.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(if (c.isDark) Color(0xFF181A19) else Color(0xFFFAF8F5))
                    .border(1.dp, c.border, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(c.forest, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    SerifText("Zen Moments · 60s Mindfulness", 16.sp, Modifier.weight(1f))
                    IconSquareButton(
                        if (sound) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                        if (sound) "Mute mindfulness chime" else "Enable mindfulness chime",
                        { sound = !sound }, bordered = false, tint = if (sound) c.accent else c.textSecondary,
                    )
                    IconSquareButton(Icons.Outlined.Close, "Close Zen Moments", onClose, bordered = false)
                }
                Box(Modifier.size(288.dp).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    val track = c.surfaceMuted
                    val ring = if (c.isDark) c.sage else c.forest
                    Canvas(Modifier.size(280.dp)) {
                        val stroke = 4.dp.toPx()
                        val inset = stroke / 2 + size.minDimension * (20f / 300f)
                        val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                        drawArc(ring, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Box(Modifier.size(208.dp).scale(scale * 1.15f).blur(28.dp).background(Color(0xFF8A9A86).copy(alpha = glowAlpha), CircleShape))
                    Box(Modifier.size(176.dp).scale(scale * 1.05f).border(1.dp, c.sage.copy(alpha = 0.3f), CircleShape))
                    Column(
                        Modifier.size(144.dp).scale(scale).clip(CircleShape)
                            .background(
                                if (c.isDark) Brush.verticalGradient(listOf(Color(0xFF2A302B), Color(0xFF1D211E), Color(0xFF38433A)))
                                else Brush.verticalGradient(listOf(c.sage.copy(alpha = 0.2f), Color(0xCCEFECE6), c.forest.copy(alpha = 0.3f))),
                            )
                            .border(1.dp, c.sage.copy(alpha = 0.4f), CircleShape),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.Air, null, tint = c.accent, modifier = Modifier.size(if (completed) 40.dp else 32.dp))
                        BodyText("${secondsLeft}s", size = 24.sp, mono = true, weight = FontWeight.SemiBold, color = c.textPrimary)
                    }
                }
                Column(Modifier.padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SerifText(title, 22.sp, weight = FontWeight.Medium)
                    androidx.compose.material3.Text(subtitle, color = c.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                    if (!completed) BodyText("BREATH $cycleIndex OF 4 · BOX RHYTHM", size = 11.sp, mono = true, color = c.accent, letterSpacing = 2.sp)
                }
                if (completed) {
                    PrimaryButton("Start Building Wallpaper", onStartBuilding, Modifier.fillMaxWidth(), icon = Icons.Outlined.AutoAwesome, minHeight = 48.dp)
                    Spacer(Modifier.height(10.dp))
                    OutlineButton("Breathe Again (60s)", ::restart, Modifier.fillMaxWidth(), icon = Icons.Outlined.RestartAlt)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InverseButton(
                            if (active) "Pause Exercise" else "Resume Breath", { active = !active }, Modifier.fillMaxWidth(),
                            icon = if (active) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, minHeight = 46.dp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton("Reset", ::restart, Modifier.weight(1f), icon = Icons.Outlined.RestartAlt, minHeight = 46.dp)
                            PrimaryButton("Start Building", onStartBuilding, Modifier.weight(1f), icon = Icons.Outlined.AutoAwesome)
                        }
                    }
                }
            }
        }
    }
}

/** Soft sine bell: 100 ms attack to 0.06 gain then exponential decay over 1.2 s, as the Web Audio version. */
private fun playChime(frequency: Double) {
    runCatching {
        val rate = 44100
        val n = (rate * 1.3).toInt()
        val pcm = ShortArray(n)
        val attack = rate * 0.1
        for (i in 0 until n) {
            val tSec = i / rate.toDouble()
            val env = if (i < attack) 0.06 * i / attack else 0.06 * exp(-(tSec - 0.1) / 0.12)
            pcm[i] = (sin(2 * PI * frequency * tSec) * env * Short.MAX_VALUE * 4).toInt().coerceIn(-32767, 32767).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, n)
        track.play()
        Thread.sleep(1400)
        track.release()
    }
}
