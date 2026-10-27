package com.example.ui.components

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.User
import com.example.data.repository.EchoTalkRepository
import com.example.utils.getCountryFlag
import com.example.utils.getGenderIcon
import kotlinx.coroutines.delay

data class VirtualGiftItem(
    val id: String,
    val name: String,
    val emoji: String,
    val cost: String
)

val VIRTUAL_GIFTS = listOf(
    VirtualGiftItem("diamond", "Blue Diamond", "\uD83D\uDC8E", "50 Sparks"),
    VirtualGiftItem("rose", "Velvet Rose", "\uD83C\uDF39", "20 Sparks"),
    VirtualGiftItem("headphones", "Gold Studio Phones", "\uD83C\uDFA7", "100 Sparks"),
    VirtualGiftItem("coffee", "Artisan Coffee", "\u2615", "10 Sparks"),
    VirtualGiftItem("rocket", "Cosmic Rocket", "\uD83D\uDE80", "200 Sparks"),
    VirtualGiftItem("crown", "Crown of Honor", "\uD83D\uDC51", "500 Sparks")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullBleedProfileDialog(
    user: User,
    onStartDM: (User) -> Unit,
    onDismiss: () -> Unit,
    repository: EchoTalkRepository = EchoTalkRepository.getInstance(),
    onLaunchGameChallenge: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Audio Preview playback simulation
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableFloatStateOf(0f) }

    // Heart Like animation
    var heartPop by remember { mutableStateOf(false) }
    val heartScale by animateFloatAsState(
        targetValue = if (heartPop) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        finishedListener = { heartPop = false },
        label = "HeartScalePop"
    )

    // Virtual Gift Bottom Sheet
    var showGiftSheet by remember { mutableStateOf(false) }

    // Multi-photo list (at least 1, up to 6)
    val photoList = remember(user) {
        if (user.photos.isNotEmpty()) user.photos else listOf(user.avatarUrl)
    }
    val pagerState = rememberPagerState(pageCount = { photoList.size })

    // Audio waveform simulation
    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            val steps = 50
            for (i in 0..steps) {
                audioProgress = i / steps.toFloat()
                delay(100)
            }
            isPlayingAudio = false
            audioProgress = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B0F17)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // Top Slidable Photo Carousel Container with Rounded Bottom (28.dp)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    ) {
                        // Horizontal Photo Pager
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val currentPhoto = photoList.getOrElse(page) { "" }
                            if (currentPhoto.isNotBlank()) {
                                AsyncImage(
                                    model = currentPhoto,
                                    contentDescription = "${user.name} photo $page",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFFEC4899)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.take(2).uppercase(),
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Gradient mask for top controls & bottom info
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x99000000),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color(0xD9000000)
                                        )
                                    )
                                )
                        )

                        // Top Navigation / Close Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() },
                                shape = CircleShape,
                                color = Color(0x66000000)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // IP-Verified Status Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x66000000)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = getCountryFlag(user.countryCode), fontSize = 13.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (user.online) Color(0xFF10B981) else Color(0xFF9CA3AF))
                                    )
                                    Text(
                                        text = if (user.online) "Online Active" else "Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.online) Color(0xFF10B981) else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }

                        // Expandable Pagination Dots overlayed at bottom center
                        if (photoList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(photoList.size) { index ->
                                    val isSelected = pagerState.currentPage == index
                                    val dotWidth by animateDpAsState(
                                        targetValue = if (isSelected) 18.dp else 6.dp,
                                        animationSpec = tween(durationMillis = 300),
                                        label = "DotWidth"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(dotWidth)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Bar (Like, Message, Friend, Gift 🎁)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button: Pill container with heart icon and total like counter
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    heartPop = true
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    repository.toggleLikeUser(user.id)
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x26EF4444)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (user.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .scale(heartScale)
                                )
                                Text(
                                    text = "${user.likesCount}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        // Message Button: Primary accent color, triggers lead-to-request DM
                        Button(
                            onClick = {
                                onDismiss()
                                onStartDM(user)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Message", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // + Friend Button: State-driven container
                        val isPending = user.friendStatus == "pending"
                        val isAccepted = user.friendStatus == "accepted" || user.isFriend
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    repository.sendFriendRequest(user.id)
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                isAccepted -> Color(0x3310B981)
                                isPending -> Color(0x33F59E0B)
                                else -> Color(0x26FFFFFF)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when {
                                        isAccepted -> "✓ Friends"
                                        isPending -> "Pending"
                                        else -> "+ Friend"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isAccepted -> Color(0xFF10B981)
                                        isPending -> Color(0xFFFBBF24)
                                        else -> Color.White
                                    }
                                )
                            }
                        }

                        // Gift 🎁 Button: Gradient accent button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showGiftSheet = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF8B5CF6),
                                                Color(0xFFEC4899)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "\uD83C\uDF81", fontSize = 14.sp)
                                    Text("Gift", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 1v1 Challenge Shortcut Banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onDismiss()
                                onLaunchGameChallenge?.invoke("chess", user.name)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x1F6366F1),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x336366F1))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "\uD83C\uDFAE", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Challenge to 1v1 Arcade Game",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Send instant Chess or Ludo invite to ${user.name}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // User Details Stack Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Name, Age & Handle
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = user.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = "${user.age}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                if (user.isVerified) {
                                    BlueTickBadge(size = 18.dp)
                                } else if (user.ipVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified IP",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${user.handle} • ${user.country} ${getCountryFlag(user.countryCode)}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Chips Row: Gender, Country, Role
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gender Chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1AFFFFFF)
                            ) {
                                Text(
                                    text = "${getGenderIcon(user.gender)} ${user.gender}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Role Chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1A6366F1)
                            ) {
                                Text(
                                    text = user.role.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF818CF8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // IP Verified Chip
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1A10B981)
                            ) {
                                Text(
                                    text = "IP-Verified \uD83D\uDD12",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // 5-Second Audio Preview Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isPlayingAudio = !isPlayingAudio },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x14FFFFFF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1FFFFFFF))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6366F1)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play Audio Preview",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = user.audioPreviewTitle.ifBlank { "5-Second Ambient Voice / Music Snippet" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isPlayingAudio) "Playing audio snippet... (5s)" else "Tap to listen to ${user.name}'s voice / audio track",
                                                fontSize = 10.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "5.0s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF818CF8)
                                    )
                                }

                                if (isPlayingAudio || audioProgress > 0f) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { audioProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp),
                                        color = Color(0xFF818CF8),
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                }
                            }
                        }

                        // Bio Text Block
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ABOUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x0FFFFFFF)
                            ) {
                                Text(
                                    text = user.bio.ifBlank { "No bio added yet. Discover more on EchoTalk!" },
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 19.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        // Profile Banner Preview if available
                        if (user.bannerUrl.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "BANNER HIGHLIGHT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    AsyncImage(
                                        model = user.bannerUrl,
                                        contentDescription = "User Banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Virtual Gifting Bottom Sheet Modal
            if (showGiftSheet) {
                AlertDialog(
                    onDismissRequest = { showGiftSheet = false },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showGiftSheet = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                    },
                    containerColor = Color(0xFF131B2B),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "\uD83C\uDF81", fontSize = 22.sp)
                            Text("Send a Gift to ${user.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Gifts give instant spark boosts and send celebratory animations in DMs!",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(VIRTUAL_GIFTS) { gift ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                repository.sendVirtualGift(user.id, gift.name, gift.emoji)
                                                Toast.makeText(context, "Sent ${gift.emoji} ${gift.name} to ${user.name}!", Toast.LENGTH_SHORT).show()
                                                showGiftSheet = false
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0x1AFFFFFF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26FFFFFF))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = gift.emoji, fontSize = 28.sp)
                                            Text(text = gift.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(text = gift.cost, fontSize = 9.sp, color = Color(0xFFFBBF24))
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
