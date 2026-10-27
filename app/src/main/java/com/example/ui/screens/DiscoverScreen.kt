package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.data.repository.EchoTalkRepository
import com.example.ui.components.BlueTickBadge
import com.example.utils.getCountryFlag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    repository: EchoTalkRepository,
    onStartDM: (User) -> Unit,
    onViewProfile: (User) -> Unit,
    onLaunchGameChallenge: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val users by repository.users.collectAsState()
    val currentUser by repository.currentUser.collectAsState()

    // Real-time Multi-filters
    var selectedGender by remember { mutableStateOf("All") }
    var minAge by remember { mutableFloatStateOf(18f) }
    var maxAge by remember { mutableFloatStateOf(35f) }
    var selectedCountry by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAgeFilterMenu by remember { mutableStateOf(false) }

    // Distinct countries for filter
    val availableCountries = remember(users) {
        listOf("All") + users.map { it.country }.distinct().sorted()
    }

    // Filter computation
    val filteredUsers = remember(users, selectedGender, minAge, maxAge, selectedCountry, searchQuery, currentUser.id) {
        users.filter { user ->
            user.id != currentUser.id &&
            (selectedGender == "All" || user.gender.equals(selectedGender, ignoreCase = true)) &&
            (user.age in minAge.toInt()..maxAge.toInt()) &&
            (selectedCountry == "All" || user.country.equals(selectedCountry, ignoreCase = true)) &&
            (searchQuery.isBlank() || user.name.contains(searchQuery, ignoreCase = true) || user.handle.contains(searchQuery, ignoreCase = true) || user.country.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Discover Search & Stats Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with online presence indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Discover",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = "${filteredUsers.count { it.online }} Online",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Zero-lag live discovery with verified IP flags & 1v1 challenges",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reset Filters Button if any active
                    if (selectedGender != "All" || selectedCountry != "All" || minAge > 18f || maxAge < 35f || searchQuery.isNotBlank()) {
                        TextButton(
                            onClick = {
                                selectedGender = "All"
                                selectedCountry = "All"
                                minAge = 18f
                                maxAge = 35f
                                searchQuery = ""
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, handle, country...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        }

        // Multi-Filter Bar: Gender Chips, Age Slider Trigger, Country Chips
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                // Filter Row: Gender & Age Range Trigger
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gender Options: All, Male, Female, Non-Binary
                    val genderOptions = listOf("All", "Male", "Female", "Non-Binary")
                    items(genderOptions) { gender ->
                        val isSelected = selectedGender == gender
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGender = gender },
                            label = {
                                Text(
                                    text = when (gender) {
                                        "Male" -> "\u2642\uFE0F Male"
                                        "Female" -> "\u2640\uFE0F Female"
                                        "Non-Binary" -> "\u2728 Non-Binary"
                                        else -> "All Genders"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    // Age Range Picker Trigger Chip
                    item {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showAgeFilterMenu = !showAgeFilterMenu },
                            shape = RoundedCornerShape(16.dp),
                            color = if (minAge > 18f || maxAge < 35f) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (minAge > 18f || maxAge < 35f) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Age Range", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "Age: ${minAge.toInt()}–${maxAge.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Expandable Age Range Slider Drawer
                AnimatedVisibility(visible = showAgeFilterMenu) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 3.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Target Age Range: ${minAge.toInt()} to ${maxAge.toInt()} years",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = {
                                        minAge = 18f
                                        maxAge = 35f
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("Reset Age", fontSize = 10.sp)
                                }
                            }

                            RangeSlider(
                                value = minAge..maxAge,
                                onValueChange = { range ->
                                    minAge = range.start
                                    maxAge = range.endInclusive
                                },
                                valueRange = 18f..45f,
                                steps = 26,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Country Chips Row with Locked IP Verified Flags
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(availableCountries) { country ->
                        val isSelected = selectedCountry == country
                        val flag = if (country == "All") "\uD83C\uDF10" else getCountryFlag(users.find { it.country == country }?.countryCode.orEmpty())
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCountry = country },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = flag, fontSize = 12.sp)
                                Text(
                                    text = country,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Profile Cards Grid (Virtualised, responsive 3:4 borderless glassmorphic cards)
        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "\uD83D\uDD0D", fontSize = 42.sp)
                    Text(
                        text = "No Profiles Match Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try adjusting your gender, country, or age sliders to discover more members.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    DiscoverProfileCard(
                        user = user,
                        onCardClick = { onViewProfile(user) },
                        onLikeClick = { repository.toggleLikeUser(user.id) },
                        onAddFriendClick = { repository.sendFriendRequest(user.id) },
                        onChallengeClick = { onLaunchGameChallenge("chess", user.name) }
                    )
                }
            }
        }
    }
}

/**
 * High-production, borderless glassmorphic Discover profile card (3:4 aspect ratio).
 * Smooth scale down on press, full-bleed media, locked IP Verified badge, and instant interaction triggers.
 */
@Composable
fun DiscoverProfileCard(
    user: User,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onChallengeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "CardPressScale"
    )

    // Heart spring pop animation
    var heartPop by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartPop) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        finishedListener = { heartPop = false },
        label = "HeartPop"
    )

    val coroutineScope = rememberCoroutineScope()
    val photoUrl = user.photos.firstOrNull() ?: user.avatarUrl

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .scale(scaleAnim)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x14FFFFFF))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onCardClick()
                    }
                )
            }
    ) {
        // Full-Bleed Media Thumbnail with smooth ContentScale.Crop
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "${user.name}'s profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant fallback gradient with initials
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFEC4899)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(2).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Scrim Gradient for maximum name legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x10000000),
                            Color(0x80000000),
                            Color(0xD9000000)
                        ),
                        startY = 140f
                    )
                )
        )

        // Top Right: Locked IP-Verified Flag with active status dot in soft glass pill
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x66000000))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = getCountryFlag(user.countryCode), fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (user.online) Color(0xFF10B981) else Color(0xFF9CA3AF))
                )
            }
        }

        // Top Left: Audio Preview indicator if user attached music/voice snippet
        if (user.audioPreviewTitle.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Has Audio Preview",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Bottom Content Stack: Name, Age, and Instant Interaction Triggers
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Name & Age with verified check
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${user.name}, ${user.age}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isVerified) {
                    BlueTickBadge(size = 14.dp)
                } else if (user.ipVerified) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified IP",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Subtitle handle & country
            Text(
                text = "${user.handle} • ${user.country}",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Instant Interaction Triggers Bar: Heart Like, Quick Friend (+), 1v1 Challenge (🎮)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart Like Button with count and spring scale pop
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            heartPop = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onLikeClick()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (user.isLiked) Color(0x33EF4444) else Color(0x33000000)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (user.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (user.isLiked) Color(0xFFEF4444) else Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .scale(heartScale)
                        )
                        Text(
                            text = "${user.likesCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick Game Challenge 1v1 (🎮)
                    Surface(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onChallengeClick()
                            },
                        shape = CircleShape,
                        color = Color(0x336366F1)
                    ) {
                        Box(
                            modifier = Modifier.padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "1v1 Game Challenge",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Quick Friend Request (+) / Accepted
                    val isPending = user.friendStatus == "pending"
                    val isAccepted = user.friendStatus == "accepted" || user.isFriend
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onAddFriendClick()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isAccepted -> Color(0x3310B981)
                            isPending -> Color(0x33F59E0B)
                            else -> Color(0x33FFFFFF)
                        }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    isAccepted -> "✓ Friends"
                                    isPending -> "Pending"
                                    else -> "+ Add"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isAccepted -> Color(0xFF10B981)
                                    isPending -> Color(0xFFFBBF24)
                                    else -> Color.White
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
