@file:Suppress("DefaultLocale")

package com.example.teduh

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.SecureRandom
import java.time.LocalDate
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
// =====================
// WARNA & TEMA
// =====================

private val TeduhGreen = Color(0xFF477A68)
private val TeduhGreenDark = Color(0xFF2F5A4B)
private val TeduhLight = Color(0xFFE3EFEA)
private val TeduhBackground = Color(0xFFF4F7F5)
private val TeduhDark = Color(0xFF263A33)
private val TeduhPeach = Color(0xFFF6C89F)
private val TeduhGray = Color(0xFF7A8782)

private val HeaderBrush = Brush.verticalGradient(
    listOf(Color(0xFF5E9A84), TeduhGreenDark)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseHelper(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = TeduhGreen,
                    background = TeduhBackground
                )
            ) {
                TeduhApp(database)
            }
        }
    }
}

// =====================
// DATABASE
// =====================

data class Mood(
    val id: Int = 0,
    val date: String,
    val score: Int,
    val emotion: String,
    val trigger: String,
    val journal: String
)

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "teduh.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE moods (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                score INTEGER,
                emotion TEXT,
                trigger_name TEXT,
                journal TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE dass_results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT,
                depression INTEGER,
                anxiety INTEGER,
                stress INTEGER
            )
            """.trimIndent()
        )

        createUsersTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Data lama tidak dihapus, hanya menambah tabel baru.
        if (oldVersion < 2) createUsersTable(db)
    }

    // ---------- AKUN PENGGUNA ----------

    private fun createUsersTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes() =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun hashPassword(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 10000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        return bytes.toHex()
    }

    /** Mengembalikan false jika username sudah dipakai. */
    fun registerUser(username: String, password: String): Boolean {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val values = ContentValues().apply {
            put("username", username.trim().lowercase())
            put("password_hash", hashPassword(password, salt))
            put("salt", salt.toHex())
        }
        return writableDatabase.insert("users", null, values) != -1L
    }

    fun loginUser(username: String, password: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT password_hash, salt FROM users WHERE username = ?",
            arrayOf(username.trim().lowercase())
        )
        cursor.use {
            if (it.moveToFirst()) {
                val savedHash = it.getString(0)
                val salt = it.getString(1).hexToBytes()
                return hashPassword(password, salt) == savedHash
            }
        }
        return false
    }

    /** Mengembalikan false jika username baru sudah dipakai. */
    fun updateUsername(oldName: String, newName: String): Boolean {
        val values = ContentValues().apply {
            put("username", newName.trim().lowercase())
        }
        return try {
            writableDatabase.update(
                "users", values, "username = ?",
                arrayOf(oldName.trim().lowercase())
            ) > 0
        } catch (e: SQLiteConstraintException) {
            false
        }
    }

    fun insertMood(mood: Mood): Boolean {
        val values = ContentValues().apply {
            put("date", mood.date)
            put("score", mood.score)
            put("emotion", mood.emotion)
            put("trigger_name", mood.trigger)
            put("journal", mood.journal)
        }
        return writableDatabase.insert("moods", null, values) != -1L
    }

    fun getAllMoods(): List<Mood> {
        val result = mutableListOf<Mood>()
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM moods ORDER BY date DESC, id DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    Mood(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        date = it.getString(it.getColumnIndexOrThrow("date")),
                        score = it.getInt(it.getColumnIndexOrThrow("score")),
                        emotion = it.getString(it.getColumnIndexOrThrow("emotion")),
                        trigger = it.getString(it.getColumnIndexOrThrow("trigger_name")),
                        journal = it.getString(it.getColumnIndexOrThrow("journal"))
                    )
                )
            }
        }
        return result
    }

    fun insertDassResult(date: String, depression: Int, anxiety: Int, stress: Int): Boolean {
        val values = ContentValues().apply {
            put("date", date)
            put("depression", depression)
            put("anxiety", anxiety)
            put("stress", stress)
        }
        return writableDatabase.insert("dass_results", null, values) != -1L
    }

    fun getLatestDassResult(): Map<String, Int>? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM dass_results ORDER BY id DESC LIMIT 1",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return mapOf(
                    "depression" to it.getInt(it.getColumnIndexOrThrow("depression")),
                    "anxiety" to it.getInt(it.getColumnIndexOrThrow("anxiety")),
                    "stress" to it.getInt(it.getColumnIndexOrThrow("stress"))
                )
            }
        }
        return null
    }
}

// =====================
// RECOMMENDATION
// =====================

data class ActivityRecommendation(
    val name: String,
    val description: String,
    val emotions: List<String>,
    val triggers: List<String>,
    val duration: String
)

object RecommendationHelper {

    private val activities = listOf(
        ActivityRecommendation(
            "Latihan Pernapasan",
            "Atur napas secara perlahan untuk membantu tubuh lebih rileks.",
            listOf("Cemas", "Stres", "Marah"),
            listOf("Pekerjaan", "Akademik", "Hubungan"),
            "5 menit"
        ),
        ActivityRecommendation(
            "Mendengarkan Musik",
            "Dengarkan musik yang menenangkan dan nikmati suasananya.",
            listOf("Sedih", "Stres", "Lelah"),
            listOf("Pekerjaan", "Akademik", "Lingkungan"),
            "10 menit"
        ),
        ActivityRecommendation(
            "Menulis Jurnal",
            "Tuliskan perasaan dan hal-hal yang sedang kamu pikirkan.",
            listOf("Sedih", "Cemas", "Marah"),
            listOf("Hubungan", "Keluarga", "Akademik"),
            "10 menit"
        ),
        ActivityRecommendation(
            "Berjalan Santai",
            "Lakukan jalan santai untuk menyegarkan pikiran.",
            listOf("Stres", "Lelah", "Bosan"),
            listOf("Pekerjaan", "Akademik", "Lingkungan"),
            "15 menit"
        ),
        ActivityRecommendation(
            "Meditasi",
            "Luangkan waktu untuk menenangkan pikiran dan fokus pada diri sendiri.",
            listOf("Cemas", "Stres", "Marah"),
            listOf("Pekerjaan", "Hubungan", "Akademik"),
            "10 menit"
        ),
        ActivityRecommendation(
            "Berbicara dengan Teman",
            "Ceritakan perasaanmu kepada orang yang kamu percaya.",
            listOf("Sedih", "Cemas", "Kesepian"),
            listOf("Hubungan", "Keluarga", "Lingkungan"),
            "15 menit"
        ),
        ActivityRecommendation(
            "Istirahat Sejenak",
            "Berikan waktu bagi tubuh dan pikiran untuk beristirahat.",
            listOf("Lelah", "Stres", "Bosan"),
            listOf("Pekerjaan", "Akademik", "Lingkungan"),
            "5 menit"
        )
    )

    fun getRecommendations(
        emotion: String,
        trigger: String,
        stressLevel: String
    ): List<ActivityRecommendation> {

        return activities.map { activity ->

            val emotionMatch =
                if (activity.emotions.any { it.equals(emotion, ignoreCase = true) }) 1 else 0

            val triggerMatch =
                if (activity.triggers.any { it.equals(trigger, ignoreCase = true) }) 1 else 0

            val durationMatch =
                if (stressLevel == "Normal" || stressLevel == "Mild") {
                    if (activity.duration == "15 menit") 1 else 0
                } else {
                    if (activity.duration == "5 menit" || activity.duration == "10 menit") 1 else 0
                }

            val score = (emotionMatch * 3) + (triggerMatch * 2) + (durationMatch * 1)

            Pair(activity, score)

        }.sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
}

// =====================
// HELPER TAMPILAN
// =====================

private fun emotionEmoji(emotion: String): String = when (emotion) {
    "Senang" -> "😊"
    "Sedih" -> "😢"
    "Cemas" -> "😰"
    "Stres" -> "😫"
    "Marah" -> "😠"
    "Lelah" -> "😴"
    "Bosan" -> "😑"
    "Kesepian" -> "🥺"
    else -> "🙂"
}

private fun scoreEmoji(score: Int): String = when (score) {
    1 -> "😞"
    2 -> "🙁"
    3 -> "😐"
    4 -> "🙂"
    else -> "😄"
}

private fun scoreColor(score: Double): Color = when {
    score <= 0.0 -> Color(0xFFD5DDD9)
    score < 2.0 -> Color(0xFFE57373)
    score < 3.0 -> Color(0xFFF2A65A)
    score < 4.0 -> Color(0xFFE6CF63)
    else -> Color(0xFF6DB38F)
}

private fun categoryColor(category: String): Color = when (category) {
    "Normal" -> Color(0xFF4CAF82)
    "Mild" -> Color(0xFF9CCB5A)
    "Moderate" -> Color(0xFFF2A65A)
    "Severe" -> Color(0xFFE8744F)
    else -> Color(0xFFD64545)
}

private fun categoryIndo(category: String): String = when (category) {
    "Normal" -> "Normal"
    "Mild" -> "Ringan"
    "Moderate" -> "Sedang"
    "Severe" -> "Berat"
    else -> "Sangat Berat"
}

// =====================
// MAIN APP
// =====================

/** Aksi "kembali ke Beranda". Null berarti sedang di Beranda. */
private val LocalOnBack = compositionLocalOf<(() -> Unit)?> { null }

/*
 * Nomor layar:
 * 0 = Beranda (halaman utama)
 * 1 = Catat Mood      2 = Analisis Mood   3 = Tes DASS-21
 * 4 = Rekomendasi     5 = Ruang Bersama   6 = Profil
 */
@Composable
fun TeduhApp(database: DatabaseHelper) {

    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var currentScreen by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val moods = remember(refresh) { database.getAllMoods() }
    val dassResult = remember(refresh) { database.getLatestDassResult() }

    if (!isLoggedIn) {

        LoginScreen(
            onLogin = { u, p -> database.loginUser(u, p) },
            onRegister = { u, p -> database.registerUser(u, p) },
            onLoginSuccess = { name ->
                username = name
                isLoggedIn = true
                currentScreen = 0
            }
        )

    } else {

        val goHome: () -> Unit = { currentScreen = 0 }

        // Tombol back HP: dari fitur kembali ke Beranda dulu.
        BackHandler(enabled = currentScreen != 0) { currentScreen = 0 }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TeduhBackground)
                .navigationBarsPadding()
        ) {
            CompositionLocalProvider(
                LocalOnBack provides (if (currentScreen == 0) null else goHome)
            ) {
                when (currentScreen) {

                    0 -> HomeScreen(
                        username = username,
                        moods = moods,
                        dassResult = dassResult,
                        onNavigate = { screen -> currentScreen = screen }
                    )

                    1 -> MoodScreen(
                        moods = moods,
                        onSave = { mood ->
                            val success = database.insertMood(mood)
                            if (success) refresh++
                            success
                        }
                    )

                    2 -> AnalysisScreen(moods)

                    3 -> DassScreen(
                        onSave = { depression, anxiety, stress ->
                            val success = database.insertDassResult(
                                LocalDate.now().toString(),
                                depression,
                                anxiety,
                                stress
                            )
                            if (success) refresh++
                            success
                        }
                    )

                    4 -> RecommendationScreen(moods = moods, dassResult = dassResult)

                    5 -> CommunityScreen()

                    6 -> ProfileScreen(
                        username = username,
                        moodCount = moods.size,
                        onChangeUsername = { newName ->
                            val ok = database.updateUsername(username, newName)
                            if (ok) username = newName.trim().lowercase()
                            ok
                        },
                        onLogout = {
                            isLoggedIn = false
                            username = ""
                            currentScreen = 0
                        }
                    )
                }
            }
        }
    }
}

// =====================
// KOMPONEN UMUM
// =====================

/** Header hijau bergradien. Menampilkan tombol "Beranda" jika bukan di halaman utama. */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String = "",
    emoji: String = "",
    action: (@Composable () -> Unit)? = null
) {
    val onBack = LocalOnBack.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(HeaderBrush)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onBack() }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "‹  Beranda",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (emoji == "LOGO") {
                    Image(
                        painter = painterResource(id = R.drawable.foto_teduh),
                        contentDescription = "Foto TEDUH",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                } else if (emoji.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 26.sp)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                if (action != null) action()
            }
        }
    }
}

/** Layout standar halaman: header + isi yang bisa di-scroll. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String = "",
    emoji: String = "",
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(title, subtitle, emoji, action)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun TeduhCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun CardTitle(text: String, emoji: String = "") {
    Text(
        if (emoji.isNotEmpty()) "$emoji  $text" else text,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = TeduhDark
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TeduhGreen)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TeduhGreen else TeduhLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TeduhDark,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// =====================
// LOGIN
// =====================

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Boolean,
    onRegister: (String, String) -> Boolean,
    onLoginSuccess: (String) -> Unit
) {

    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val passwordTransformation =
        if (showPassword) VisualTransformation.None else PasswordVisualTransformation()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HeaderBrush)
            .statusBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.foto_teduh),
                contentDescription = "Foto TEDUH",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )

            Text(
                "TEDUH",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Text(
                "Temukan ketenangan,\nkenali dirimu.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            TeduhCard {

                Text(
                    if (isRegister) "Buat akun 🌱" else "Selamat datang 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeduhDark
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = "" },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = "" },
                    label = { Text("Password") },
                    visualTransformation = passwordTransformation,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) "Sembunyi" else "Lihat",
                                color = TeduhGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                )

                if (isRegister) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; error = "" },
                        label = { Text("Ulangi password") },
                        visualTransformation = passwordTransformation,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                PrimaryButton(if (isRegister) "Daftar" else "Masuk") {
                    val name = username.trim()

                    when {
                        name.isEmpty() || password.isEmpty() ->
                            error = "Username dan password wajib diisi."

                        isRegister && name.length < 3 ->
                            error = "Username minimal 3 karakter."

                        isRegister && password.length < 6 ->
                            error = "Password minimal 6 karakter."

                        isRegister && password != confirm ->
                            error = "Konfirmasi password tidak sama."

                        isRegister -> {
                            if (onRegister(name, password)) {
                                onLoginSuccess(name.lowercase())
                            } else {
                                error = "Username sudah dipakai."
                            }
                        }

                        else -> {
                            if (onLogin(name, password)) {
                                onLoginSuccess(name.lowercase())
                            } else {
                                error = "Username atau password salah."
                            }
                        }
                    }
                }

                TextButton(
                    onClick = {
                        isRegister = !isRegister
                        error = ""
                        confirm = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isRegister) "Sudah punya akun? Masuk"
                        else "Belum punya akun? Daftar",
                        color = TeduhGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "TEDUH · Mood & Mental Wellness",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

// =====================
// HOME
// =====================

private data class HomeFeature(
    val emoji: String,
    val title: String,
    val description: String,
    val screen: Int
)

@Composable
fun HomeScreen(
    username: String,
    moods: List<Mood>,
    dassResult: Map<String, Int>?,
    onNavigate: (Int) -> Unit
) {

    val latestMood = moods.firstOrNull()
    val stressScore = dassResult?.get("stress") ?: 0
    val stressCategory = if (dassResult != null) getDassCategory(stressScore, "stress") else null

    val features = listOf(
        HomeFeature("📝", "Catat Mood", "Tulis perasaanmu hari ini.", 1),
        HomeFeature("📊", "Analisis Mood", "Lihat perkembangan mingguan.", 2),
        HomeFeature("🧠", "Tes DASS-21", "Kenali depresi, cemas, dan stres.", 3),
        HomeFeature("✨", "Rekomendasi", "Aktivitas sesuai kondisimu.", 4),
        HomeFeature("🤝", "Ruang Bersama", "Berbagi cerita dengan sesama.", 5),
        HomeFeature("👤", "Profil", "Kelola akun dan username.", 6)
    )

    ScreenScaffold(
        title = "Halo, $username!",
        subtitle = "Bagaimana perasaanmu hari ini?",
        emoji = "LOGO",
        action = { ProfileAvatar(username) { onNavigate(6) } }
    ) {

        // Ringkasan kondisi
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            TeduhCard(modifier = Modifier.weight(1f)) {
                Text("Mood Terakhir", fontSize = 12.sp, color = TeduhGray)
                if (latestMood != null) {
                    Text(emotionEmoji(latestMood.emotion), fontSize = 36.sp)
                    Text(
                        latestMood.emotion,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TeduhDark
                    )
                    Text("Skor ${latestMood.score}/5", fontSize = 12.sp, color = TeduhGray)
                } else {
                    Text("🫥", fontSize = 36.sp)
                    Text("Belum ada catatan", fontSize = 13.sp, color = TeduhGray)
                }
            }

            TeduhCard(modifier = Modifier.weight(1f)) {
                Text("Tingkat Stres", fontSize = 12.sp, color = TeduhGray)
                if (stressCategory != null) {
                    Text(
                        "$stressScore",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor(stressCategory)
                    )
                    Badge(categoryIndo(stressCategory), categoryColor(stressCategory))
                } else {
                    Text("🧠", fontSize = 36.sp)
                    Text("Belum ada hasil", fontSize = 13.sp, color = TeduhGray)
                }
            }
        }

        Text(
            "Fitur TEDUH",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TeduhDark
        )

        // Semua fitur diakses dari sini
        features.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { f ->
                    HomeTile(
                        emoji = f.emoji,
                        title = f.title,
                        description = f.description,
                        modifier = Modifier.weight(1f)
                    ) { onNavigate(f.screen) }
                }
            }
        }
    }
}

@Composable
fun ProfileAvatar(username: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            username.take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HomeTile(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(156.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(TeduhLight),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TeduhDark)
            Text(description, fontSize = 12.sp, color = TeduhGray, maxLines = 2)
        }
    }
}

// =====================
// PROFIL
// =====================

@Composable
fun ProfileScreen(
    username: String,
    moodCount: Int,
    onChangeUsername: (String) -> Boolean,
    onLogout: () -> Unit
) {

    val context = LocalContext.current

    var newName by remember { mutableStateOf(username) }
    var error by remember { mutableStateOf("") }
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Keluar dari akun?", fontWeight = FontWeight.Bold) },
            text = { Text("Kamu bisa masuk lagi kapan saja dengan username dan password.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Keluar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("Batal", color = TeduhGray)
                }
            }
        )
    }

    ScreenScaffold(
        title = "Profil",
        subtitle = "Kelola akunmu.",
        emoji = "👤"
    ) {

        TeduhCard {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(TeduhLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        username.take(1).uppercase().ifEmpty { "?" },
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeduhGreen
                    )
                }
            }

            Text(
                username,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(
                "📝 $moodCount catatan mood",
                fontSize = 13.sp,
                color = TeduhGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        TeduhCard {
            CardTitle("Ganti username", "✏️")

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it; error = "" },
                label = { Text("Username baru") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            PrimaryButton("Simpan username") {
                val name = newName.trim().lowercase()

                when {
                    name.length < 3 -> error = "Username minimal 3 karakter."
                    name == username -> error = "Username sama dengan yang sekarang."
                    !name.matches(Regex("[a-z0-9_.]+")) ->
                        error = "Gunakan huruf, angka, titik, atau underscore."
                    onChangeUsername(name) -> {
                        newName = name
                        Toast.makeText(context, "Username berhasil diganti.", Toast.LENGTH_SHORT).show()
                    }
                    else -> error = "Username sudah dipakai."
                }
            }
        }

        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Keluar",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =====================
// MOOD JOURNAL
// =====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodScreen(
    moods: List<Mood>,
    onSave: (Mood) -> Boolean
) {

    val context = LocalContext.current

    var score by remember { mutableIntStateOf(3) }
    var emotion by remember { mutableStateOf("Senang") }
    var trigger by remember { mutableStateOf("Akademik") }
    var journal by remember { mutableStateOf("") }

    val emotions = listOf("Senang", "Sedih", "Cemas", "Stres", "Marah", "Lelah", "Bosan", "Kesepian")
    val triggers = listOf("Akademik", "Pekerjaan", "Hubungan", "Keluarga", "Lingkungan", "Lainnya")
    val scoreLabels = listOf("Sangat buruk", "Buruk", "Biasa saja", "Baik", "Sangat baik")

    ScreenScaffold(
        title = "Catatan Mood",
        subtitle = "Ceritakan bagaimana perasaanmu hari ini.",
        emoji = "📝"
    ) {

        TeduhCard {
            CardTitle("Seberapa baik harimu?", "🌤️")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { value ->
                    val selected = score == value
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (selected) scoreColor(value.toDouble()).copy(alpha = 0.25f) else TeduhBackground)
                            .border(
                                BorderStroke(
                                    if (selected) 2.5.dp else 0.dp,
                                    if (selected) scoreColor(value.toDouble()) else Color.Transparent
                                ),
                                CircleShape
                            )
                            .clickable { score = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(scoreEmoji(value), fontSize = if (selected) 30.sp else 24.sp)
                    }
                }
            }

            Text(
                "$score/5 · ${scoreLabels[score - 1]}",
                fontWeight = FontWeight.Bold,
                color = TeduhGreen,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        TeduhCard {
            CardTitle("Emosi yang dirasakan", "💭")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emotions.forEach { item ->
                    SelectChip("${emotionEmoji(item)} $item", emotion == item) { emotion = item }
                }
            }
        }

        TeduhCard {
            CardTitle("Pemicu perasaan", "🎯")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                triggers.forEach { item ->
                    SelectChip(item, trigger == item) { trigger = item }
                }
            }
        }

        TeduhCard {
            CardTitle("Jurnal", "📖")

            OutlinedTextField(
                value = journal,
                onValueChange = { journal = it },
                placeholder = { Text("Tuliskan apa yang kamu rasakan...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp)
            )

            PrimaryButton("Simpan Mood") {
                val mood = Mood(
                    date = LocalDate.now().toString(),
                    score = score,
                    emotion = emotion,
                    trigger = trigger,
                    journal = journal
                )

                if (onSave(mood)) {
                    Toast.makeText(context, "Mood berhasil disimpan.", Toast.LENGTH_SHORT).show()
                    score = 3
                    emotion = "Senang"
                    trigger = "Akademik"
                    journal = ""
                } else {
                    Toast.makeText(context, "Gagal menyimpan mood.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Text(
            "Riwayat Mood",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhDark
        )

        if (moods.isEmpty()) {
            Text("Belum ada catatan.", color = TeduhGray)
        }

        moods.take(10).forEach { mood -> MoodHistoryItem(mood, showJournal = true) }
    }
}

@Composable
fun MoodHistoryItem(mood: Mood, showJournal: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(scoreColor(mood.score.toDouble()).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emotionEmoji(mood.emotion), fontSize = 24.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mood.emotion, fontWeight = FontWeight.Bold, color = TeduhDark)
                    Text(mood.date, fontSize = 12.sp, color = TeduhGray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge("Skor ${mood.score}/5", scoreColor(mood.score.toDouble()))
                    Badge(mood.trigger, TeduhGreen)
                }
                if (showJournal && mood.journal.isNotBlank()) {
                    Text(mood.journal, fontSize = 13.sp, color = TeduhGray)
                }
            }
        }
    }
}

// =====================
// ANALISIS MOOD
// =====================

@Composable
fun AnalysisScreen(moods: List<Mood>) {

    val today = LocalDate.now()
    val currentStart = today.minusDays(6)
    val previousStart = today.minusDays(13)
    val previousEnd = today.minusDays(7)

    val currentWeek = moods.filter {
        val date = LocalDate.parse(it.date)
        !date.isBefore(currentStart) && !date.isAfter(today)
    }

    val previousWeek = moods.filter {
        val date = LocalDate.parse(it.date)
        !date.isBefore(previousStart) && !date.isAfter(previousEnd)
    }

    val currentAverage = if (currentWeek.isNotEmpty()) currentWeek.map { it.score }.average() else 0.0
    val previousAverage = if (previousWeek.isNotEmpty()) previousWeek.map { it.score }.average() else 0.0

    val lowestMood = currentWeek.minByOrNull { it.score }

    val dominantTrigger = currentWeek.groupingBy { it.trigger }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: "-"

    val difference = currentAverage - previousAverage

    // Data grafik 7 hari
    val chartDays = (0..6).map { today.minusDays((6 - it).toLong()) }
    val chartData = chartDays.map { day ->
        val items = moods.filter { it.date == day.toString() }
        val avg = if (items.isNotEmpty()) items.map { it.score }.average() else 0.0
        Pair(day, avg)
    }

    val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    ScreenScaffold(
        title = "Analisis Mood",
        subtitle = "Ringkasan perkembangan mood dalam 7 hari.",
        emoji = "📊"
    ) {

        TeduhCard {
            CardTitle("Rata-rata minggu ini", "📈")
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    if (currentWeek.isNotEmpty()) String.format("%.1f", currentAverage) else "–",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeduhGreen
                )
                Text(
                    "/ 5",
                    fontSize = 18.sp,
                    color = TeduhGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text("${currentWeek.size} catatan dalam 7 hari terakhir", fontSize = 13.sp, color = TeduhGray)
        }

        TeduhCard {
            CardTitle("Grafik 7 hari", "🗓️")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                chartData.forEach { (day, avg) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (avg > 0) {
                            Text(String.format("%.1f", avg), fontSize = 10.sp, color = TeduhGray)
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(if (avg > 0) (avg.toFloat() / 5f * 100f).dp else 6.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(scoreColor(avg))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            dayNames[day.dayOfWeek.value - 1],
                            fontSize = 11.sp,
                            color = TeduhGray
                        )
                    }
                }
            }
        }

        TeduhCard {
            CardTitle("Perbandingan mingguan", "⚖️")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minggu lalu", fontSize = 12.sp, color = TeduhGray)
                    Text(
                        if (previousWeek.isNotEmpty()) String.format("%.2f", previousAverage) else "-",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeduhDark
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minggu ini", fontSize = 12.sp, color = TeduhGray)
                    Text(
                        if (currentWeek.isNotEmpty()) String.format("%.2f", currentAverage) else "-",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TeduhGreen
                    )
                }
            }

            if (currentWeek.isNotEmpty() && previousWeek.isNotEmpty()) {
                val (text, color) = when {
                    difference > 0 -> Pair("📈 Mood meningkat ${String.format("%.2f", difference)}", Color(0xFF4CAF82))
                    difference < 0 -> Pair("📉 Mood menurun ${String.format("%.2f", -difference)}", Color(0xFFE8744F))
                    else -> Pair("➖ Mood tidak berubah", TeduhGray)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text, color = color, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Perbandingan membutuhkan data dari kedua minggu.",
                    fontSize = 13.sp,
                    color = TeduhGray
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            TeduhCard(modifier = Modifier.weight(1f)) {
                Text("Mood terendah", fontSize = 12.sp, color = TeduhGray)
                if (lowestMood != null) {
                    Text(emotionEmoji(lowestMood.emotion), fontSize = 32.sp)
                    Text(lowestMood.emotion, fontWeight = FontWeight.Bold, color = TeduhDark)
                    Text("Skor ${lowestMood.score}/5 · ${lowestMood.date}", fontSize = 11.sp, color = TeduhGray)
                } else {
                    Text("Belum ada data", fontSize = 13.sp, color = TeduhGray)
                }
            }

            TeduhCard(modifier = Modifier.weight(1f)) {
                Text("Pemicu dominan", fontSize = 12.sp, color = TeduhGray)
                Text("🎯", fontSize = 32.sp)
                Text(dominantTrigger, fontWeight = FontWeight.Bold, color = TeduhDark)
            }
        }

        Text(
            "Riwayat Mood",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhDark
        )

        moods.forEach { mood -> MoodHistoryItem(mood) }

        if (moods.isEmpty()) {
            Text(
                "Belum ada data mood. Mulai mencatat mood untuk melihat analisis.",
                color = TeduhGray
            )
        }
    }
}

// =====================
// DASS-21
// =====================

private val dassQuestions = listOf(
    "Saya merasa sulit untuk menenangkan diri.",
    "Saya menyadari mulut saya terasa kering.",
    "Saya merasa tidak dapat mengalami perasaan positif.",
    "Saya mengalami kesulitan bernapas tanpa aktivitas fisik.",
    "Saya merasa sulit untuk memulai sesuatu.",
    "Saya cenderung bereaksi berlebihan terhadap suatu situasi.",
    "Saya mengalami gemetar.",
    "Saya merasa menggunakan banyak energi untuk merasa tenang.",
    "Saya merasa khawatir terhadap situasi yang membuat saya panik.",
    "Saya merasa tidak memiliki sesuatu yang dapat dinantikan.",
    "Saya merasa gelisah.",
    "Saya merasa sulit untuk bersantai.",
    "Saya merasa sedih dan tertekan.",
    "Saya tidak dapat mentoleransi gangguan terhadap aktivitas saya.",
    "Saya merasa hampir panik.",
    "Saya tidak merasa antusias terhadap apa pun.",
    "Saya merasa tidak berharga.",
    "Saya merasa mudah tersinggung.",
    "Saya menyadari detak jantung tanpa aktivitas fisik.",
    "Saya merasa takut tanpa alasan yang jelas.",
    "Saya merasa hidup tidak berarti."
)

private val depressionIndexes = listOf(2, 4, 9, 12, 15, 16, 20)
private val anxietyIndexes = listOf(1, 3, 6, 8, 14, 18, 19)
private val stressIndexes = listOf(0, 5, 7, 10, 11, 13, 17)

private fun calculateDass(answers: Map<Int, Int>, indexes: List<Int>): Int {
    return indexes.sumOf { answers[it] ?: 0 } * 2
}

private fun getDassCategory(score: Int, type: String): String {
    return when (type) {
        "depression" -> when {
            score <= 9 -> "Normal"
            score <= 13 -> "Mild"
            score <= 20 -> "Moderate"
            score <= 27 -> "Severe"
            else -> "Extremely Severe"
        }
        "anxiety" -> when {
            score <= 7 -> "Normal"
            score <= 9 -> "Mild"
            score <= 14 -> "Moderate"
            score <= 19 -> "Severe"
            else -> "Extremely Severe"
        }
        else -> when {
            score <= 14 -> "Normal"
            score <= 18 -> "Mild"
            score <= 25 -> "Moderate"
            score <= 33 -> "Severe"
            else -> "Extremely Severe"
        }
    }
}

@Composable
fun DassScreen(onSave: (Int, Int, Int) -> Boolean) {

    val context = LocalContext.current
    val answers = remember { mutableStateMapOf<Int, Int>() }

    var resultDepression by remember { mutableStateOf<Int?>(null) }
    var resultAnxiety by remember { mutableStateOf<Int?>(null) }
    var resultStress by remember { mutableStateOf<Int?>(null) }

    ScreenScaffold(
        title = "Tes DASS-21",
        subtitle = "Jawab berdasarkan pengalamanmu selama 7 hari terakhir.",
        emoji = "🧠"
    ) {

        TeduhCard {
            CardTitle("Petunjuk", "ℹ️")
            Text(
                "Pilih angka yang paling sesuai dengan kondisimu.",
                fontSize = 13.sp,
                color = TeduhGray
            )
            Text("0 = Tidak pernah", fontSize = 13.sp)
            Text("1 = Kadang-kadang", fontSize = 13.sp)
            Text("2 = Sering", fontSize = 13.sp)
            Text("3 = Hampir selalu", fontSize = 13.sp)

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Progres", fontSize = 12.sp, color = TeduhGray)
                Text("${answers.size}/21", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TeduhGreen)
            }
            LinearProgressIndicator(
                progress = { answers.size / 21f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = TeduhGreen,
                trackColor = TeduhLight
            )
        }

        dassQuestions.forEachIndexed { index, question ->

            TeduhCard {
                Text(
                    "${index + 1}. $question",
                    fontWeight = FontWeight.SemiBold,
                    color = TeduhDark,
                    fontSize = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..3).forEach { value ->
                        val selected = answers[index] == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) TeduhGreen else TeduhLight)
                                .clickable { answers[index] = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$value",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (selected) Color.White else TeduhDark
                            )
                        }
                    }
                }
            }
        }

        PrimaryButton("Lihat Hasil") {
            if (answers.size != 21) {
                Toast.makeText(
                    context,
                    "Jawab seluruh 21 pertanyaan terlebih dahulu.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val depression = calculateDass(answers, depressionIndexes)
                val anxiety = calculateDass(answers, anxietyIndexes)
                val stress = calculateDass(answers, stressIndexes)

                if (onSave(depression, anxiety, stress)) {
                    resultDepression = depression
                    resultAnxiety = anxiety
                    resultStress = stress
                    Toast.makeText(context, "Hasil tes berhasil disimpan.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal menyimpan hasil tes.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val d = resultDepression
        val a = resultAnxiety
        val s = resultStress

        if (d != null && a != null && s != null) {

            Text(
                "Hasil Tes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhDark
            )

            DassResultCard("😔", "Depresi", d, getDassCategory(d, "depression"))
            DassResultCard("😰", "Kecemasan", a, getDassCategory(a, "anxiety"))
            DassResultCard("😫", "Stres", s, getDassCategory(s, "stress"))

            TeduhCard {
                CardTitle("Catatan", "💚")
                Text(
                    "Hasil ini merupakan skrining awal, bukan diagnosis psikologis atau medis. " +
                            "Jika mengalami kesulitan, pertimbangkan untuk berkonsultasi dengan tenaga profesional.",
                    fontSize = 13.sp,
                    color = TeduhGray
                )
            }
        }
    }
}

@Composable
fun DassResultCard(
    emoji: String,
    title: String,
    score: Int,
    category: String
) {
    val color = categoryColor(category)

    TeduhCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(emoji, fontSize = 26.sp)
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TeduhDark)
            }
            Badge(categoryIndo(category), color)
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text("$score", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                " / 42",
                fontSize = 14.sp,
                color = TeduhGray,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }

        LinearProgressIndicator(
            progress = { (score / 42f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

// =====================
// REKOMENDASI
// =====================

private fun activityEmoji(name: String): String = when (name) {
    "Latihan Pernapasan" -> "🌬️"
    "Mendengarkan Musik" -> "🎧"
    "Menulis Jurnal" -> "✍️"
    "Berjalan Santai" -> "🚶"
    "Meditasi" -> "🧘"
    "Berbicara dengan Teman" -> "💬"
    else -> "🛋️"
}

@Composable
fun RecommendationScreen(
    moods: List<Mood>,
    dassResult: Map<String, Int>?
) {

    val latestMood = moods.firstOrNull()
    val stressScore = dassResult?.get("stress") ?: 0

    val stressLevel = if (dassResult != null) getDassCategory(stressScore, "stress") else "Moderate"

    val recommendations = if (latestMood != null) {
        RecommendationHelper.getRecommendations(
            latestMood.emotion,
            latestMood.trigger,
            stressLevel
        )
    } else {
        emptyList()
    }

    ScreenScaffold(
        title = "Rekomendasi",
        subtitle = "Aktivitas yang dapat membantu menjaga kesejahteraanmu.",
        emoji = "✨"
    ) {

        if (latestMood == null) {

            TeduhCard {
                Text(
                    "🌱",
                    fontSize = 40.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Belum tersedia rekomendasi",
                    fontWeight = FontWeight.Bold,
                    color = TeduhDark,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Catat mood terlebih dahulu agar TEDUH dapat menyesuaikan rekomendasi aktivitas dengan kondisimu.",
                    fontSize = 13.sp,
                    color = TeduhGray,
                    textAlign = TextAlign.Center
                )
            }

        } else {

            TeduhCard {
                CardTitle("Berdasarkan mood terakhir", "🔎")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge("${emotionEmoji(latestMood.emotion)} ${latestMood.emotion}", TeduhGreen)
                    Badge(latestMood.trigger, Color(0xFFE8A04F))
                }
                if (dassResult != null) {
                    Text(
                        "Kategori stres: ${categoryIndo(stressLevel)}",
                        fontSize = 13.sp,
                        color = TeduhGray
                    )
                } else {
                    Text("Tes stres: belum tersedia", fontSize = 13.sp, color = TeduhGray)
                }
            }

            recommendations.forEachIndexed { index, activity ->

                TeduhCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(TeduhLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(activityEmoji(activity.name), fontSize = 28.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Rekomendasi ${index + 1}",
                                color = TeduhGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                activity.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TeduhDark
                            )
                        }
                    }

                    Text(activity.description, fontSize = 14.sp, color = TeduhGray)
                    Badge("⏱ ${activity.duration}", TeduhGreen)
                }
            }
        }
    }
}