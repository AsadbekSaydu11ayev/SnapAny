package com.snapany.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snapany.app.model.FormatDownloadState
import com.snapany.app.model.FormatInfo
import com.snapany.app.model.ParseResponse
import com.snapany.app.model.PlatformType
import com.snapany.app.ui.components.HeroMediaPlayerDialog
import com.snapany.app.ui.components.MorphingDownloadButton
import com.snapany.app.ui.components.ShimmerResultCard
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
import com.snapany.app.ui.viewmodel.SnapAnyViewModel

@Composable
fun HomeScreen(viewModel: SnapAnyViewModel) {
    var previewMedia by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("SnapAny", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Universal Video Downloader", color = TextSecondary, fontSize = 11.sp)
                }
            }

            // Jonli server status indikatori
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, SurfaceBorder, CircleShape)
                    .clickable { viewModel.checkHealth() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (viewModel.isServerOnline) EmeraldOnline else ErrorRed)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (viewModel.isServerOnline) "Tizim Faol" else "Oflayn",
                    color = if (viewModel.isServerOnline) EmeraldOnline else ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Qidiruv va Havola kiritish kartasi (Hero morflanuvchi karta)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceGlass.copy(alpha = 0.88f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Har qanday video va audioni bir zumda yuklang",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = viewModel.urlInput,
                onValueChange = { viewModel.urlInput = it },
                placeholder = { Text("YouTube, TikTok yoki Instagram havolasi...", color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null, tint = IndigoAccent) },
                trailingIcon = {
                    if (viewModel.urlInput.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearInput() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tozalash", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PurpleAccent,
                    focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                        .clickable { viewModel.pasteFromClipboard() }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Joylash", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier
                        .weight(1.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryGradient)
                        .clickable(enabled = !viewModel.isLoading) { viewModel.analyzeUrl() }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (viewModel.isLoading) "Tahlil..." else "Tekshirish",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Platforma chipslari
            val activePlatform = viewModel.parsedMedia?.platformType
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlatformChip("YouTube", Color(0xFFEF4444), activePlatform == PlatformType.YOUTUBE)
                PlatformChip("TikTok", Color(0xFF06B6D4), activePlatform == PlatformType.TIKTOK)
                PlatformChip("Instagram", PinkAccent, activePlatform == PlatformType.INSTAGRAM)
            }
        }

        // Xatolik banneri
        AnimatedVisibility(
            visible = viewModel.errorMessage != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(alpha = 0.14f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ErrorRed)
                Spacer(Modifier.width(10.dp))
                Text(
                    viewModel.errorMessage.orEmpty(),
                    color = Color(0xFFFCA5A5),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hero Animatsiyali Shimmer <-> Natija kartasi almashinuvi
        AnimatedContent(
            targetState = Pair(viewModel.isLoading, viewModel.parsedMedia),
            transitionSpec = {
                (fadeIn(tween(280)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )).togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.96f))
            },
            label = "resultHero"
        ) { (loading, media) ->
            when {
                loading -> ShimmerResultCard()
                media != null -> MediaResultCard(
                    media = media,
                    downloadStates = viewModel.downloadStates,
                    onDownload = { format -> viewModel.startDownload(media, format) },
                    onOpen = { path, format ->
                        previewMedia = Triple(media.title, path, format.isAudio)
                    }
                )
                else -> Spacer(Modifier.height(4.dp))
            }
        }
    }

    previewMedia?.let { (title, path, isAudio) ->
        HeroMediaPlayerDialog(
            title = title,
            filePath = path,
            isAudio = isAudio,
            onDismiss = { previewMedia = null }
        )
    }
}

@Composable
private fun PlatformChip(name: String, color: Color, isHighlighted: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isHighlighted) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (isHighlighted) color.copy(alpha = 0.8f) else SurfaceBorder,
                CircleShape
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(name, color = if (isHighlighted) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MediaResultCard(
    media: ParseResponse,
    downloadStates: Map<String, FormatDownloadState>,
    onDownload: (FormatInfo) -> Unit,
    onOpen: (String, FormatInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass.copy(alpha = 0.92f))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thumbnail + Duration Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = media.thumbnail,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(media.duration, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                media.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!media.author.isNullOrBlank()) {
                Text(media.author, color = TextSecondary, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            media.formats.forEach { format ->
                val state = downloadStates[format.formatId] ?: FormatDownloadState.Idle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (format.isAudio) PinkAccent.copy(alpha = 0.18f)
                                else IndigoAccent.copy(alpha = 0.18f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (format.isAudio) Icons.Rounded.Audiotrack else Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = if (format.isAudio) PinkAccent else IndigoAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(format.quality, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${format.ext.uppercase()} • ${format.filesize}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    MorphingDownloadButton(
                        state = state,
                        onClick = { onDownload(format) },
                        onOpen = { path -> onOpen(path, format) }
                    )
                }
            }
        }
    }
}
