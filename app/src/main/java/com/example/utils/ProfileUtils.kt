package com.example.utils

fun getCountryFlag(countryOrCode: String?): String {
    if (countryOrCode == null) return "🌐"
    return when (countryOrCode.uppercase()) {
        "US", "USA", "UNITED STATES" -> "🇺🇸"
        "BR", "BRAZIL" -> "🇧🇷"
        "FR", "FRANCE" -> "🇫🇷"
        "JP", "JAPAN" -> "🇯🇵"
        "EE", "ESTONIA" -> "🇪🇪"
        "SG", "SINGAPORE" -> "🇸🇬"
        "NG", "NIGERIA" -> "🇳🇬"
        "IT", "ITALY" -> "🇮🇹"
        "IE", "IRELAND" -> "🇮🇪"
        "DE", "GERMANY" -> "🇩🇪"
        "CA", "CANADA" -> "🇨🇦"
        "IN", "INDIA" -> "🇮🇳"
        "PK", "PAKISTAN" -> "🇵🇰"
        "UK", "GB", "UNITED KINGDOM" -> "🇬🇧"
        else -> "🌐"
    }
}

fun getGenderIcon(gender: String?): String {
    return when (gender?.lowercase()) {
        "female", "woman", "f" -> "♀️"
        "male", "man", "m" -> "♂️"
        "non-binary", "nb" -> "⚧️"
        else -> "✨"
    }
}
