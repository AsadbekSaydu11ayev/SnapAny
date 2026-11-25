package com.snapany.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snapany.app.model.DownloadedItem
import com.snapany.app.ui.components.HeroMediaPlayerDialog
import com.snapany.app.ui.theme.ErrorRed
import com.snapany.app.ui.theme.IndigoAccent
import com.snapany.app.ui.theme.PinkAccent
import com.snapany.app.ui.theme.SurfaceBorder
import com.snapany.app.ui.theme.SurfaceGlass
import com.snapany.app.ui.theme.TextPrimary
import com.snapany.app.ui.theme.TextSecondary
import com.snapany.app.ui.viewmodel.SnapAnyViewModel

@Composable
fun DownloadsScreen(viewModel: SnapAnyViewModel) {
    val context = LocalContext.current
    var playingItem by remember { mutableStateOf<DownloadedItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text("Yuklab olishlar", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Telefonning Download/SnapAny papkasiga ham saqlanadi",
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        if (viewModel.downloadHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Hali hech narsa yuklanmagan", color = TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.downloadHistory, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceGlass.copy(alpha = 0.9f))
                            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
                            .clickable { playingItem = item }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (item.isAudio) Icons.Rounded.Audiotrack else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = if (item.isAudio) PinkAccent else IndigoAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${item.quality} • ${item.formattedSize()}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { viewModel.shareFile(context, item) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Ulashish", tint = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.deleteHistoryItem(item) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "O'chirish", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }

    playingItem?.let { item ->
        HeroMediaPlayerDialog(
            title = item.title,
            filePath = item.filePath,
            isAudio = item.isAudio,
            onDismiss = { playingItem = null }
        )
    }
}
