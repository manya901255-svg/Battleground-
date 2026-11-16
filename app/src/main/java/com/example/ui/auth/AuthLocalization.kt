package com.example.ui.auth

import androidx.compose.ui.graphics.Color

val WarXRed = Color(0xFFE5192B)
val WarXRedBright = Color(0xFFFF263B)
val WarXGold = Color(0xFFFFCA18)
val WarXGoldBright = Color(0xFFFFDF4D)
val WarXBlack = Color(0xFF0A0C12)
val WarXPanelBg = Color(0xCC0D1422)
val WarXRoyalBlue = Color(0xFF1D6CE0)
val WarXFacebookBlue = Color(0xFF1877F2)

data class GameServerRegion(
    val id: String,
    val label: String,
    val pingMs: Int
)

val WarXServerRegions = listOf(
    GameServerRegion("in", "India", 18),
    GameServerRegion("sg", "Singapore", 34),
    GameServerRegion("eu", "Europe", 62),
    GameServerRegion("na", "North America", 74),
    GameServerRegion("me", "Middle East", 41)
)

enum class AuthLanguage(
    val code: String,
    val displayLabel: String
) {
    EN("en", "English"),
    HI("hi", "हिन्दी"),
    ES("es", "Español"),
    AR("ar", "العربية"),
    PT("pt", "Português")
}

data class AuthLocalizedCopy(
    val serverPrefix: String,
    val languageButtonLabel: String,
    val tagline: String,
    val loginHeader: String,
    val continueGoogle: String,
    val continueApple: String,
    val loginEmail: String,
    val continueFacebook: String,
    val orDivider: String,
    val playGuest: String,
    val privacyPolicy: String,
    val termsOfService: String,
    val forgotPassword: String,
    val joinPrefix: String,
    val battleRoyale: String,
    val revolution: String,
    val subBrandNote: String,
    val welcomeSoldierTop: String,
    val welcomeSoldierBottom: String
)

fun authCopyFor(lang: AuthLanguage): AuthLocalizedCopy = when (lang) {
    AuthLanguage.EN -> AuthLocalizedCopy(
        serverPrefix = "Server",
        languageButtonLabel = "Language",
        tagline = "BATTLE  •  SURVIVE  •  BE THE HERO",
        loginHeader = "LOGIN TO CONTINUE",
        continueGoogle = "Continue with Google",
        continueApple = "Continue with Apple",
        loginEmail = "Login with Email",
        continueFacebook = "Continue with Facebook",
        orDivider = "OR",
        playGuest = "PLAY AS GUEST",
        privacyPolicy = "Privacy Policy",
        termsOfService = "Terms of Service",
        forgotPassword = "Forgot Password",
        joinPrefix = "JOIN THE",
        battleRoyale = "BATTLE ROYALE",
        revolution = "REVOLUTION",
        subBrandNote = "WARX BATTLEGROUND  •  ORIGINAL BATTLE ROYALE GAME",
        welcomeSoldierTop = "WELCOME",
        welcomeSoldierBottom = "SOLDIER"
    )
    AuthLanguage.HI -> AuthLocalizedCopy(
        serverPrefix = "सर्वर",
        languageButtonLabel = "भाषा",
        tagline = "लड़ो  •  बचो  •  हीरो बनो",
        loginHeader = "जारी रखने के लिए लॉगिन करें",
        continueGoogle = "Google से जारी रखें",
        continueApple = "Apple से जारी रखें",
        loginEmail = "Email से लॉगिन करें",
        continueFacebook = "Facebook से जारी रखें",
        orDivider = "या",
        playGuest = "गेस्ट के रूप में खेलें",
        privacyPolicy = "गोपनीयता नीति",
        termsOfService = "सेवा की शर्तें",
        forgotPassword = "पासवर्ड भूल गए",
        joinPrefix = "शामिल हों",
        battleRoyale = "बैटल रॉयल",
        revolution = "क्रांति में",
        subBrandNote = "WARX BATTLEGROUND  •  असली बैटल रॉयल गेम",
        welcomeSoldierTop = "स्वागत है",
        welcomeSoldierBottom = "सैनिक"
    )
    AuthLanguage.ES -> AuthLocalizedCopy(
        serverPrefix = "Servidor",
        languageButtonLabel = "Idioma",
        tagline = "BATALLA  •  SOBREVIVE  •  SÉ EL HÉROE",
        loginHeader = "INICIA SESIÓN",
        continueGoogle = "Continuar con Google",
        continueApple = "Continuar con Apple",
        loginEmail = "Entrar con Email",
        continueFacebook = "Continuar con Facebook",
        orDivider = "O",
        playGuest = "JUGAR COMO INVITADO",
        privacyPolicy = "Privacidad",
        termsOfService = "Términos",
        forgotPassword = "Olvidé mi clave",
        joinPrefix = "ÚNETE A LA",
        battleRoyale = "BATTLE ROYALE",
        revolution = "REVOLUCIÓN",
        subBrandNote = "WARX BATTLEGROUND  •  JUEGO BATTLE ROYALE ORIGINAL",
        welcomeSoldierTop = "BIENVENIDO",
        welcomeSoldierBottom = "SOLDADO"
    )
    AuthLanguage.AR -> AuthLocalizedCopy(
        serverPrefix = "الخادم",
        languageButtonLabel = "اللغة",
        tagline = "قاتل  •  انجُ  •  كن البطل",
        loginHeader = "سجل الدخول للمتابعة",
        continueGoogle = "المتابعة عبر Google",
        continueApple = "المتابعة عبر Apple",
        loginEmail = "الدخول بالبريد الإلكتروني",
        continueFacebook = "المتابعة عبر Facebook",
        orDivider = "أو",
        playGuest = "العب كضيف",
        privacyPolicy = "سياسة الخصوصية",
        termsOfService = "شروط الخدمة",
        forgotPassword = "نسيت كلمة المرور",
        joinPrefix = "انضم إلى",
        battleRoyale = "باتل رويال",
        revolution = "الثورة",
        subBrandNote = "WARX BATTLEGROUND  •  لعبة باتل رويال الأصلية",
        welcomeSoldierTop = "مرحباً",
        welcomeSoldierBottom = "أيها الجندي"
    )
    AuthLanguage.PT -> AuthLocalizedCopy(
        serverPrefix = "Servidor",
        languageButtonLabel = "Idioma",
        tagline = "BATALHE  •  SOBREVIVA  •  SEJA O HERÓI",
        loginHeader = "FAÇA LOGIN",
        continueGoogle = "Continuar com Google",
        continueApple = "Continuar com Apple",
        loginEmail = "Entrar com Email",
        continueFacebook = "Continuar com Facebook",
        orDivider = "OU",
        playGuest = "JOGAR COMO CONVIDADO",
        privacyPolicy = "Privacidade",
        termsOfService = "Termos de Uso",
        forgotPassword = "Esqueci a senha",
        joinPrefix = "JUNTE-SE À",
        battleRoyale = "BATTLE ROYALE",
        revolution = "REVOLUÇÃO",
        subBrandNote = "WARX BATTLEGROUND  •  JOGO BATTLE ROYALE ORIGINAL",
        welcomeSoldierTop = "BEM-VINDO",
        welcomeSoldierBottom = "SOLDADO"
    )
}
