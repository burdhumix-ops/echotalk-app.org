package com.example.data.model

data class User(
    val id: String,
    val name: String,
    val handle: String,
    val country: String,
    val countryCode: String,
    val gender: String, // Female, Male, Non-Binary
    val age: Int = 24,
    val online: Boolean = true,
    val isFriend: Boolean = true,
    val reqCount: Int = 0, // Capped at 3 for pending message requests
    val bio: String = "",
    val avatarUrl: String = "",
    val role: String = "member", // owner, admin, member
    val portfolio: List<String> = emptyList(),
    val photos: List<String> = emptyList(),
    val likesCount: Int = 120,
    val isLiked: Boolean = false,
    val friendStatus: String = if (isFriend) "accepted" else "none", // "none", "pending", "accepted"
    val bannerUrl: String = "",
    val audioPreviewTitle: String = "Ambient Chillout (5s)",
    val ipVerified: Boolean = true,
    val isVerified: Boolean = false, // Blue tick verification badge
    val verificationStatus: String = "none", // "none", "pending", "approved", "rejected"
    val verificationPhotoUrl: String? = null,
    val verificationSubmittedAt: Long? = null
)
