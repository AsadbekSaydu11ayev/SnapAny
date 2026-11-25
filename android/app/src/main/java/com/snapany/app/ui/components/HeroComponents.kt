package com.snapany.app.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snapany.app.model.FormatDownloadState
import com.snapany.app.ui.theme.CosmicBg
import com.snapany.app.ui.theme.EmeraldOnline
import com.snapany.app.ui.theme.ErrorRed
import com.snapany.app.ui.theme.IndigoAccent
import com.snapany.app.ui.theme.PinkAccent
import com.snapany.app.ui.theme.PrimaryGradient
import com.snapany.app.ui.theme.PurpleAccent
import com.snapany.app.ui.theme.SurfaceBorder
import com.snapany.app.ui.theme.SurfaceGlass
import com.snapany.app.ui.theme.TextPrimary
import com.snapany.app.ui.theme.TextSecondary
import java.io.File

@Composable
fun AnimatedCosmicBackground() {
    val infinite = rememberInfiniteTransition(label = "bg")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbShift"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(CosmicBg)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(IndigoAccent.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(size.width * (0.2f + 0.12f * shift), size.height * 0.15f),
                radius = size.width * 0.65f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PurpleAccent.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * (0.85f - 0.1f * shift), size.height * 0.35f),
                radius = size.width * 0.55f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PinkAccent.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * (0.35f + 0.2f * shift), size.height * 0.85f),
                radius = size.width * 0.6f
            )
        )
    }
}

@Composable
fun ShimmerResultCard() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val alpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass.copy(alpha = 0.85f))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f * alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.10f * alpha))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.07f * alpha))
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f * alpha))
            )
        }
    }
}

@Composable
fun MorphingDownloadButton(
    state: FormatDownloadState,
    onClick: () -> Unit,
    onOpen: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.9f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.9f))
        },
        label = "morphButton"
    ) { current ->
        when (current) {
            is FormatDownloadState.Idle -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGradient)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick()
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Yuklash", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            is FormatDownloadState.Downloading -> {
                val animatedProgress by animateFloatAsState(
                    targetValue = current.progress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "dlProgress"
                )
                Column(
                    modifier = Modifier
                        .width(105.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndigoAccent.copy(alpha = 0.2f))
                        .border(1.dp, IndigoAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = PurpleAccent,
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )
                }
            }

            is FormatDownloadState.Completed -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldOnline.copy(alpha = 0.2f))
                        .border(1.dp, EmeraldOnline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable { onOpen(current.filePath) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldOnline, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Ochish", color = EmeraldOnline, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            is FormatDownloadState.Failed -> {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRed.copy(alpha = 0.2f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Qayta", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun HeroMediaPlayerDialog(
    title: String,
    filePath: String,
    isAudio: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBg.copy(alpha = 0.96f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Yopish", tint = TextPrimary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isAudio) {
                    var isPlaying by remember { mutableStateOf(true) }
                    val mediaPlayer = remember(filePath) {
                        MediaPlayer().apply {
                            setDataSource(context, Uri.fromFile(File(filePath)))
                            prepare()
                            start()
                        }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                mediaPlayer.release()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("MP3 Audio ijro etilmoqda", color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        IconButton(
                            onClick = {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setVideoURI(Uri.fromFile(File(filePath)))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
