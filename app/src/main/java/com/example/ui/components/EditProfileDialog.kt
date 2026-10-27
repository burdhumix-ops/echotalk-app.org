package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.data.repository.EchoTalkRepository
import com.example.ui.screens.DiscoverProfileCard
import com.example.utils.getCountryFlag

val PRESET_PHOTOS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=800&auto=format&fit=crop&q=80"
)

val PRESET_BANNERS = listOf(
    "https://images.unsplash.com/photo-1518770660439-4636190af475?w=1200&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=1200&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=1200&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=1200&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=1200&auto=format&fit=crop&q=80"
)

val AUDIO_PRESET_SNIPPETS = listOf(
    "Ambient Sunset Cafe (5s)",
    "Analog Modular Synth Pulse (5s)",
    "Rain On Glass & Vinyl Lo-Fi (5s)",
    "Tokyo Neon Bass Beat (5s)",
    "Acoustic Guitar Reverie (5s)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentUser: User,
    repository: EchoTalkRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(currentUser.name) }
    var handle by remember { mutableStateOf(currentUser.handle.removePrefix("@")) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var gender by remember { mutableStateOf(currentUser.gender) }
    var age by remember { mutableIntStateOf(currentUser.age) }
    var country by remember { mutableStateOf(currentUser.country) }
    var countryCode by remember { mutableStateOf(currentUser.countryCode) }
    var bannerUrl by remember { mutableStateOf(currentUser.bannerUrl) }
    var audioPreviewTitle by remember { mutableStateOf(currentUser.audioPreviewTitle) }

    // Photos list (up to 6)
    val photos = remember {
        mutableStateListOf<String>().apply {
            if (currentUser.photos.isNotEmpty()) {
                addAll(currentUser.photos)
            } else if (currentUser.avatarUrl.isNotBlank()) {
                add(currentUser.avatarUrl)
            }
        }
    }

    // Live 'View as Public' Toggle
    var isPublicPreviewOn by remember { mutableStateOf(false) }

    // Mock preview user reflecting current form state
    val previewUser = remember(name, handle, bio, gender, age, country, countryCode, photos.toList(), bannerUrl, audioPreviewTitle) {
        currentUser.copy(
            name = name.ifBlank { "You" },
            handle = "@$handle",
            bio = bio,
            gender = gender,
            age = age,
            country = country,
            countryCode = countryCode,
            avatarUrl = photos.firstOrNull() ?: currentUser.avatarUrl,
            photos = photos.toList(),
            bannerUrl = bannerUrl,
            audioPreviewTitle = audioPreviewTitle
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with Live Preview & Save Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }

                        Text(
                            text = if (isPublicPreviewOn) "Public Preview" else "Edit Profile",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // View as Public Toggle Button
                            IconButton(
                                onClick = { isPublicPreviewOn = !isPublicPreviewOn },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isPublicPreviewOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPublicPreviewOn) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "View as Public",
                                    tint = if (isPublicPreviewOn) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    repository.updateCurrentUserProfile(
                                        name = name.trim().ifBlank { currentUser.name },
                                        handle = handle.trim().ifBlank { currentUser.handle },
                                        bio = bio.trim(),
                                        gender = gender,
                                        age = age,
                                        country = country,
                                        countryCode = countryCode,
                                        photos = if (photos.isNotEmpty()) photos.toList() else listOf(currentUser.avatarUrl),
                                        bannerUrl = bannerUrl,
                                        audioPreviewTitle = audioPreviewTitle
                                    )
                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isPublicPreviewOn) {
                    // Live 'View as Public' Discover & Profile Card Preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = "How your card appears in Discover:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            DiscoverProfileCard(
                                user = previewUser,
                                onCardClick = {},
                                onLikeClick = {},
                                onAddFriendClick = {},
                                onChallengeClick = {}
                            )

                            Button(
                                onClick = { isPublicPreviewOn = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Back to Edit Mode")
                            }
                        }
                    }
                } else {
                    // Form Editor
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 1. Photo Carousel Manager (up to 6 photos, reorder/set thumbnail)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PHOTO CAROUSEL (${photos.size}/6)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "First photo is primary thumbnail",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    itemsIndexed(photos) { index, photoUrl ->
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp, 120.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = photoUrl,
                                                contentDescription = "Photo $index",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Primary Badge on First Item
                                            if (index == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Primary", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            // Remove Photo Button
                                            IconButton(
                                                onClick = {
                                                    if (photos.size > 1) {
                                                        photos.removeAt(index)
                                                    } else {
                                                        Toast.makeText(context, "Must keep at least 1 photo", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(26.dp)
                                                    .padding(2.dp)
                                                    .background(Color(0x99000000), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    // Add Photo from Presets
                                    if (photos.size < 6) {
                                        item {
                                            Surface(
                                                modifier = Modifier
                                                    .size(90.dp, 120.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        val nextPhoto = PRESET_PHOTOS.firstOrNull { it !in photos } ?: PRESET_PHOTOS.random()
                                                        photos.add(nextPhoto)
                                                    },
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("+ Add Photo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Banner Customization (animated GIF / video banner header)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "PROFILE BANNER HEADER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )

                                if (bannerUrl.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        AsyncImage(
                                            model = bannerUrl,
                                            contentDescription = "Active Banner",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(PRESET_BANNERS) { banner ->
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp, 50.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { bannerUrl = banner }
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = banner,
                                                contentDescription = "Preset banner",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Audio Preview Snippet (5-Second voice / music teaser)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "5-SECOND AUDIO PREVIEW SNIPPET",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Visitors can tap play when viewing your profile carousel.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AUDIO_PRESET_SNIPPETS) { snippet ->
                                        val isSelected = audioPreviewTitle == snippet
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { audioPreviewTitle = snippet },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = snippet,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Personal Information Stack
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "PERSONAL INFORMATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = handle,
                                    onValueChange = { handle = it },
                                    label = { Text("Username (@handle)") },
                                    prefix = { Text("@") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = { Text("Bio (Describe your audio passions & vibe)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    maxLines = 4
                                )

                                // Gender Selector
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Gender", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Male", "Female", "Non-Binary").forEach { g ->
                                            val isSelected = gender.equals(g, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { gender = g },
                                                label = { Text(g, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                // Age Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Age", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$age years", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = age.toFloat(),
                                        onValueChange = { age = it.toInt() },
                                        valueRange = 18f..65f,
                                        steps = 46
                                    )
                                }

                                // Country & Flag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = country,
                                        onValueChange = { country = it },
                                        label = { Text("Country") },
                                        modifier = Modifier.weight(2f),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = countryCode,
                                        onValueChange = { countryCode = it.take(2).uppercase() },
                                        label = { Text("Code (e.g. US)") },
                                        trailingIcon = { Text(getCountryFlag(countryCode), fontSize = 16.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
