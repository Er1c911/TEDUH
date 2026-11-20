
package com.example.teduh

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teduh.model.Mood
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val TeduhGreen = Color(0xFF477A68)
private val TeduhBackground = Color(0xFFF4F7F5)
private val TeduhDark = Color(0xFF263A33)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseHelper(this)

        setContent {
            MaterialTheme {
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
    SQLiteOpenHelper(context, "teduh.db", null, 1) {

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
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Tidak menghapus data yang sudah tersimpan.
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

    fun insertDassResult(
        date: String,
        depression: Int,
        anxiety: Int,
        stress: Int
    ): Boolean {
        val values = ContentValues().apply {
            put("date", date)
            put("depression", depression)
            put("anxiety", anxiety)
            put("stress", stress)
        }

        return writableDatabase.insert(
            "dass_results",
            null,
            values
        ) != -1L
    }

    fun getLatestDassResult(): Map<String, Int>? {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM dass_results ORDER BY id DESC LIMIT 1",
            null
        )

        cursor.use {
            if (it.moveToFirst()) {
                return mapOf(
                    "depression" to it.getInt(
                        it.getColumnIndexOrThrow("depression")
                    ),
                    "anxiety" to it.getInt(
                        it.getColumnIndexOrThrow("anxiety")
                    ),
                    "stress" to it.getInt(
                        it.getColumnIndexOrThrow("stress")
                    )
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
                if (activity.emotions.any {
                        it.equals(emotion, ignoreCase = true)
                    }) 1 else 0

            val triggerMatch =
                if (activity.triggers.any {
                        it.equals(trigger, ignoreCase = true)
                    }) 1 else 0

            val durationMatch =
                if (stressLevel == "Normal" || stressLevel == "Mild") {
                    if (activity.duration == "15 menit") 1 else 0
                } else {
                    if (activity.duration == "5 menit" ||
                        activity.duration == "10 menit"
                    ) 1 else 0
                }

            val score =
                (emotionMatch * 3) +
                        (triggerMatch * 2) +
                        (durationMatch * 1)

            Pair(activity, score)

        }.sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
}

// =====================
// MAIN APP
// =====================

@Composable
fun TeduhApp(database: DatabaseHelper) {

    var isLoggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val moods = remember(refresh) {
        database.getAllMoods()
    }

    val dassResult = remember(refresh) {
        database.getLatestDassResult()
    }

    if (!isLoggedIn) {

        LoginScreen(
            onLoginSuccess = { name ->
                username = name
                isLoggedIn = true
                selectedTab = 0
            }
        )

    } else {

        Scaffold(
            containerColor = TeduhBackground,
            bottomBar = {

                NavigationBar(
                    containerColor = Color.White
                ) {

                    val menus = listOf(
                        "Home",
                        "Mood",
                        "Analisis",
                        "Tes Stres",
                        "Rekomendasi"
                    )

                    menus.forEachIndexed { index, menu ->

                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                            },
                            icon = {
                                Text(
                                    when (index) {
                                        0 -> "⌂"
                                        1 -> "♡"
                                        2 -> "▤"
                                        3 -> "◎"
                                        else -> "✦"
                                    },
                                    fontSize = 18.sp
                                )
                            },
                            label = {
                                Text(
                                    menu,
                                    fontSize = 10.sp
                                )
                            }
                        )
                    }
                }
            }
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                when (selectedTab) {

                    0 -> HomeScreen(
                        username = username,
                        moods = moods,
                        dassResult = dassResult,
                        onNavigate = { tab ->
                            selectedTab = tab
                        }
                    )

                    1 -> MoodScreen(
                        moods = moods,
                        onSave = { mood ->
                            val success = database.insertMood(mood)

                            if (success) {
                                refresh++
                            }

                            success
                        }
                    )

                    2 -> AnalysisScreen(moods)

                    3 -> DassScreen(
                        onSave = { depression, anxiety, stress ->

                            val success =
                                database.insertDassResult(
                                    LocalDate.now().toString(),
                                    depression,
                                    anxiety,
                                    stress
                                )

                            if (success) {
                                refresh++
                            }

                            success
                        }
                    )

                    4 -> RecommendationScreen(
                        moods = moods,
                        dassResult = dassResult
                    )
                }
            }
        }
    }
}

// =====================
// COMPONENT
// =====================

@Composable
fun SectionTitle(
    title: String,
    subtitle: String = ""
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        Text(
            title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TeduhCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

// =====================
// LOGIN
// =====================

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit
) {

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "TEDUH",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhGreen
            )

            Text(
                "Temukan ketenangan,\nkenali dirimu.",
                fontSize = 18.sp,
                color = TeduhDark
            )

            TeduhCard {

                Text(
                    "Login",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        error = ""
                    },
                    label = {
                        Text("Username")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    label = {
                        Text("Password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {

                        if (username == "teduh" &&
                            password == "123456"
                        ) {
                            onLoginSuccess(username)
                        } else {
                            error = "Username atau password salah."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TeduhGreen
                    )
                ) {
                    Text("Masuk")
                }
            }

            Text(
                "TEDUH - Mood & Mental Wellness",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

// =====================
// HOME
// =====================

@Composable
fun HomeScreen(
    username: String,
    moods: List<Mood>,
    dassResult: Map<String, Int>?,
    onNavigate: (Int) -> Unit
) {

    val latestMood = moods.firstOrNull()

    val stressScore = dassResult?.get("stress") ?: 0

    val stressCategory =
        if (dassResult != null) {
            getDassCategory(stressScore, "stress")
        } else {
            "Belum ada hasil"
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "TEDUH",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        Text(
            "Halo, $username!",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhDark
        )

        TeduhCard {

            Text(
                "Selamat datang di TEDUH",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhGreen
            )

            Text(
                "Ruang untuk mengenali perasaan, " +
                        "memahami kondisi diri, dan menjaga " +
                        "kesehatan mental secara mandiri."
            )
        }

        TeduhCard {

            Text(
                "Mood Terakhir",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (latestMood != null) {

                Text("Tanggal: ${latestMood.date}")
                Text("Mood: ${latestMood.emotion}")
                Text("Skor: ${latestMood.score}/5")
                Text("Pemicu: ${latestMood.trigger}")

            } else {

                Text("Belum ada catatan mood.")
            }

            Button(
                onClick = {
                    onNavigate(1)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeduhGreen
                )
            ) {
                Text("Catat Mood")
            }
        }

        TeduhCard {

            Text(
                "Hasil Tes Stres Terakhir",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (dassResult != null) {

                Text("Skor: $stressScore")
                Text("Kategori: $stressCategory")

            } else {

                Text("Belum ada hasil tes.")
            }

            Button(
                onClick = {
                    onNavigate(3)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeduhGreen
                )
            ) {
                Text("Mulai Tes")
            }
        }

        Text(
            "Menu TEDUH",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        HomeMenuCard(
            "Analisis Mood",
            "Lihat perkembangan mood mingguan.",
            onClick = {
                onNavigate(2)
            }
        )

        HomeMenuCard(
            "Tes DASS-21",
            "Kenali tingkat depresi, kecemasan, dan stres.",
            onClick = {
                onNavigate(3)
            }
        )

        HomeMenuCard(
            "Rekomendasi Aktivitas",
            "Temukan aktivitas yang sesuai dengan kondisimu.",
            onClick = {
                onNavigate(4)
            }
        )
    }
}

@Composable
fun HomeMenuCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {

    TeduhCard {

        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        Text(description)

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = TeduhGreen
            )
        ) {
            Text("Buka")
        }
    }
}

// =====================
// MOOD JOURNAL
// =====================

@Composable
fun MoodScreen(
    moods: List<Mood>,
    onSave: (Mood) -> Boolean
) {

    val context = LocalContext.current

    var score by remember { mutableFloatStateOf(3f) }
    var emotion by remember { mutableStateOf("Senang") }
    var trigger by remember { mutableStateOf("Akademik") }
    var journal by remember { mutableStateOf("") }

    val emotions = listOf(
        "Senang",
        "Sedih",
        "Cemas",
        "Stres",
        "Marah",
        "Lelah",
        "Bosan",
        "Kesepian"
    )

    val triggers = listOf(
        "Akademik",
        "Pekerjaan",
        "Hubungan",
        "Keluarga",
        "Lingkungan",
        "Lainnya"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SectionTitle(
            "Catatan Mood",
            "Ceritakan bagaimana perasaanmu hari ini."
        )

        TeduhCard {

            Text(
                "Skor Mood: ${score.toInt()}/5",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = score,
                onValueChange = {
                    score = it
                },
                valueRange = 1f..5f,
                steps = 3
            )

            Text("1 = Sangat buruk")
            Text("2 = Buruk")
            Text("3 = Biasa saja")
            Text("4 = Baik")
            Text("5 = Sangat baik")
        }

        TeduhCard {

            Text(
                "Emosi",
                fontWeight = FontWeight.Bold
            )

            emotions.forEach { item ->

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = emotion == item,
                        onClick = {
                            emotion = item
                        }
                    )

                    Text(item)
                }
            }
        }

        TeduhCard {

            Text(
                "Pemicu Perasaan",
                fontWeight = FontWeight.Bold
            )

            triggers.forEach { item ->

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    RadioButton(
                        selected = trigger == item,
                        onClick = {
                            trigger = item
                        }
                    )

                    Text(item)
                }
            }
        }

        TeduhCard {

            Text(
                "Jurnal",
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = journal,
                onValueChange = {
                    journal = it
                },
                placeholder = {
                    Text("Tuliskan apa yang kamu rasakan...")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Button(
                onClick = {

                    val mood = Mood(
                        date = LocalDate.now().toString(),
                        score = score.toInt(),
                        emotion = emotion,
                        trigger = trigger,
                        journal = journal
                    )

                    if (onSave(mood)) {

                        Toast.makeText(
                            context,
                            "Mood berhasil disimpan.",
                            Toast.LENGTH_SHORT
                        ).show()

                        score = 3f
                        emotion = "Senang"
                        trigger = "Akademik"
                        journal = ""

                    } else {

                        Toast.makeText(
                            context,
                            "Gagal menyimpan mood.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeduhGreen
                )
            ) {
                Text("Simpan Mood")
            }
        }

        Text(
            "Riwayat Mood",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        moods.take(10).forEach { mood ->

            TeduhCard {

                Text(
                    mood.date,
                    color = TeduhGreen,
                    fontWeight = FontWeight.Bold
                )

                Text("Emosi: ${mood.emotion}")
                Text("Skor: ${mood.score}/5")
                Text("Pemicu: ${mood.trigger}")

                if (mood.journal.isNotBlank()) {
                    Text("Jurnal: ${mood.journal}")
                }
            }
        }
    }
}

// =====================
// MOOD ANALYSIS
// =====================

@Composable
fun AnalysisScreen(
    moods: List<Mood>
) {

    val today = LocalDate.now()

    val currentStart = today.minusDays(6)
    val previousStart = today.minusDays(13)
    val previousEnd = today.minusDays(7)

    val currentWeek = moods.filter {
        val date = LocalDate.parse(it.date)

        !date.isBefore(currentStart) &&
                !date.isAfter(today)
    }

    val previousWeek = moods.filter {
        val date = LocalDate.parse(it.date)

        !date.isBefore(previousStart) &&
                !date.isAfter(previousEnd)
    }

    val currentAverage =
        if (currentWeek.isNotEmpty()) {
            currentWeek.map { it.score }.average()
        } else {
            0.0
        }

    val previousAverage =
        if (previousWeek.isNotEmpty()) {
            previousWeek.map { it.score }.average()
        } else {
            0.0
        }

    val lowestMood = currentWeek.minByOrNull { it.score }

    val dominantTrigger =
        currentWeek.groupingBy { it.trigger }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "-"

    val difference = currentAverage - previousAverage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SectionTitle(
            "Analisis Mood",
            "Ringkasan perkembangan mood dalam 7 hari."
        )

        TeduhCard {

            Text(
                "Rata-rata Mood Minggu Ini",
                fontWeight = FontWeight.Bold
            )

            Text(
                if (currentWeek.isNotEmpty()) {
                    String.format("%.2f / 5", currentAverage)
                } else {
                    "Belum ada data"
                },
                fontSize = 30.sp,
                color = TeduhGreen,
                fontWeight = FontWeight.Bold
            )

            Text("Jumlah catatan: ${currentWeek.size}")
        }

        TeduhCard {

            Text(
                "Perbandingan Mingguan",
                fontWeight = FontWeight.Bold
            )

            Text(
                "Minggu sebelumnya: " +
                        if (previousWeek.isNotEmpty()) {
                            String.format("%.2f", previousAverage)
                        } else {
                            "-"
                        }
            )

            Text(
                "Minggu ini: " +
                        if (currentWeek.isNotEmpty()) {
                            String.format("%.2f", currentAverage)
                        } else {
                            "-"
                        }
            )

            if (currentWeek.isNotEmpty() &&
                previousWeek.isNotEmpty()
            ) {

                Text(
                    when {
                        difference > 0 ->
                            "Mood meningkat sebesar " +
                                    String.format("%.2f", difference)

                        difference < 0 ->
                            "Mood menurun sebesar " +
                                    String.format("%.2f", -difference)

                        else ->
                            "Mood tidak mengalami perubahan."
                    },
                    color = TeduhGreen,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "Perbandingan membutuhkan data " +
                            "dari kedua minggu."
                )
            }
        }

        TeduhCard {

            Text(
                "Mood Terendah",
                fontWeight = FontWeight.Bold
            )

            if (lowestMood != null) {

                Text("Emosi: ${lowestMood.emotion}")
                Text("Skor: ${lowestMood.score}/5")
                Text("Tanggal: ${lowestMood.date}")

            } else {

                Text("Belum ada data minggu ini.")
            }
        }

        TeduhCard {

            Text(
                "Pemicu Dominan",
                fontWeight = FontWeight.Bold
            )

            Text(dominantTrigger)
        }

        Text(
            "Riwayat Mood",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        moods.forEach { mood ->

            TeduhCard {

                Text(
                    mood.date,
                    color = TeduhGreen,
                    fontWeight = FontWeight.Bold
                )

                Text("Mood: ${mood.emotion}")
                Text("Skor: ${mood.score}/5")
                Text("Pemicu: ${mood.trigger}")
            }
        }

        if (moods.isEmpty()) {

            Text(
                "Belum ada data mood. " +
                        "Mulai mencatat mood untuk melihat analisis."
            )
        }
    }
}

// =====================
// DASS-21 QUESTIONS
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

private val depressionIndexes = listOf(
    2, 4, 9, 12, 15, 16, 20
)

private val anxietyIndexes = listOf(
    1, 3, 6, 8, 14, 18, 19
)

private val stressIndexes = listOf(
    0, 5, 7, 10, 11, 13, 17
)

private fun calculateDass(
    answers: Map<Int, Int>,
    indexes: List<Int>
): Int {

    return indexes.sumOf {
        answers[it] ?: 0
    } * 2
}

private fun getDassCategory(
    score: Int,
    type: String
): String {

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

// =====================
// DASS SCREEN
// =====================

@Composable
fun DassScreen(
    onSave: (Int, Int, Int) -> Boolean
) {

    val context = LocalContext.current

    val answers = remember {
        mutableStateMapOf<Int, Int>()
    }

    var resultDepression by remember {
        mutableStateOf<Int?>(null)
    }

    var resultAnxiety by remember {
        mutableStateOf<Int?>(null)
    }

    var resultStress by remember {
        mutableStateOf<Int?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SectionTitle(
            "Tes DASS-21",
            "Jawab 21 pertanyaan berdasarkan pengalaman " +
                    "selama 7 hari terakhir."
        )

        TeduhCard {

            Text(
                "Petunjuk",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Text(
                "Pilih jawaban yang paling sesuai " +
                        "dengan kondisi yang kamu alami " +
                        "selama 7 hari terakhir."
            )

            Text("0 = Tidak pernah")
            Text("1 = Kadang-kadang")
            Text("2 = Sering")
            Text("3 = Hampir selalu")
        }

        dassQuestions.forEachIndexed { index, question ->

            TeduhCard {

                Text(
                    "${index + 1}. $question",
                    fontWeight = FontWeight.Bold
                )

                val options = listOf(
                    "0 - Tidak pernah",
                    "1 - Kadang-kadang",
                    "2 - Sering",
                    "3 - Hampir selalu"
                )

                options.forEachIndexed { value, label ->

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = answers[index] == value,
                            onClick = {
                                answers[index] = value
                            }
                        )

                        Text(label)
                    }
                }
            }
        }

        Button(
            onClick = {

                if (answers.size != 21) {

                    Toast.makeText(
                        context,
                        "Jawab seluruh 21 pertanyaan terlebih dahulu.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    val depression = calculateDass(
                        answers,
                        depressionIndexes
                    )

                    val anxiety = calculateDass(
                        answers,
                        anxietyIndexes
                    )

                    val stress = calculateDass(
                        answers,
                        stressIndexes
                    )

                    if (onSave(depression, anxiety, stress)) {

                        resultDepression = depression
                        resultAnxiety = anxiety
                        resultStress = stress

                        Toast.makeText(
                            context,
                            "Hasil tes berhasil disimpan.",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            context,
                            "Gagal menyimpan hasil tes.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeduhGreen
            )
        ) {
            Text("Lihat Hasil")
        }

        if (resultDepression != null &&
            resultAnxiety != null &&
            resultStress != null
        ) {

            Text(
                "Hasil Tes",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            DassResultCard(
                title = "Depresi",
                score = resultDepression!!,
                category = getDassCategory(
                    resultDepression!!,
                    "depression"
                )
            )

            DassResultCard(
                title = "Kecemasan",
                score = resultAnxiety!!,
                category = getDassCategory(
                    resultAnxiety!!,
                    "anxiety"
                )
            )

            DassResultCard(
                title = "Stres",
                score = resultStress!!,
                category = getDassCategory(
                    resultStress!!,
                    "stress"
                )
            )

            TeduhCard {

                Text(
                    "Catatan",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Hasil ini merupakan skrining awal, " +
                            "bukan diagnosis psikologis atau medis. " +
                            "Jika mengalami kesulitan, pertimbangkan " +
                            "untuk berkonsultasi dengan tenaga profesional."
                )
            }
        }
    }
}

// =====================
// DASS RESULT CARD
// =====================

@Composable
fun DassResultCard(
    title: String,
    score: Int,
    category: String
) {

    TeduhCard {

        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        Text(
            "Skor: $score",
            fontSize = 20.sp
        )

        Text(
            "Kategori: $category",
            fontWeight = FontWeight.Bold
        )
    }
}

// =====================
// RECOMMENDATIONS
// =====================

@Composable
fun RecommendationScreen(
    moods: List<Mood>,
    dassResult: Map<String, Int>?
) {

    val latestMood = moods.firstOrNull()

    val stressScore = dassResult?.get("stress") ?: 0

    val stressLevel =
        if (dassResult != null) {
            getDassCategory(stressScore, "stress")
        } else {
            "Moderate"
        }

    val recommendations =
        if (latestMood != null) {

            RecommendationHelper.getRecommendations(
                latestMood.emotion,
                latestMood.trigger,
                stressLevel
            )

        } else {
            emptyList()
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SectionTitle(
            "Rekomendasi",
            "Aktivitas yang dapat membantu " +
                    "menjaga kesejahteraanmu."
        )

        if (latestMood == null) {

            TeduhCard {

                Text(
                    "Belum tersedia rekomendasi",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Catat mood terlebih dahulu agar " +
                            "TEDUH dapat menyesuaikan rekomendasi " +
                            "aktivitas dengan kondisimu."
                )
            }

        } else {

            TeduhCard {

                Text(
                    "Berdasarkan Mood Terakhir",
                    fontWeight = FontWeight.Bold
                )

                Text("Emosi: ${latestMood.emotion}")
                Text("Pemicu: ${latestMood.trigger}")

                if (dassResult != null) {
                    Text("Kategori stres: $stressLevel")
                } else {
                    Text("Tes stres: belum tersedia")
                }
            }

            recommendations.forEachIndexed { index, activity ->

                TeduhCard {

                    Text(
                        "Rekomendasi ${index + 1}",
                        color = TeduhGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        activity.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(activity.description)

                    Text(
                        "Durasi: ${activity.duration}",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}