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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapany.app.network.SnapAnyApi
import com.snapany.app.ui.theme.EmeraldOnline
import com.snapany.app.ui.theme.ErrorRed
import com.snapany.app.ui.theme.IndigoAccent
import com.snapany.app.ui.theme.PrimaryGradient
import com.snapany.app.ui.theme.SurfaceBorder
import com.snapany.app.ui.theme.SurfaceGlass
import com.snapany.app.ui.theme.TextPrimary
import com.snapany.app.ui.theme.TextSecondary
import com.snapany.app.ui.viewmodel.SnapAnyViewModel

@Composable
fun SettingsScreen(viewModel: SnapAnyViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Sozlamalar", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Server sozlamalari
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceGlass.copy(alpha = 0.9f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Backend Server URL", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isServerOnline) EmeraldOnline else ErrorRed)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (viewModel.isCheckingHealth) "Tekshirilmoqda..." else if (viewModel.isServerOnline) "Ulangan" else "Ulanmagan",
                        color = if (viewModel.isServerOnline) EmeraldOnline else ErrorRed,
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.serverUrlInput,
                onValueChange = { viewModel.serverUrlInput = it },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable {
                            viewModel.serverUrlInput = SnapAnyApi.DEFAULT_SERVER_URL
                            viewModel.saveServerUrl(SnapAnyApi.DEFAULT_SERVER_URL)
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Render", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGradient)
                        .clickable { viewModel.saveServerUrl(viewModel.serverUrlInput) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Saqlash", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Kesh / Saqlangan fayllar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SurfaceGlass.copy(alpha = 0.9f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(22.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Ilova ichidagi fayllar", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Jami hajm: ${viewModel.totalCacheSizeText()}", color = TextSecondary, fontSize = 13.sp)
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed.copy(alpha = 0.16f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.clearAllDownloads() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Tozalash", color = ErrorRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
