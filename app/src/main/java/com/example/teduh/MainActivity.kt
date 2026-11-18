
package com.example.teduh

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teduh.database.DatabaseHelper
import com.example.teduh.model.Mood
import com.example.teduh.utils.RecommendationHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val TeduhGreen = Color(0xFF477A68)
private val TeduhBackground = Color(0xFFF4F7F5)

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

@Composable
fun TeduhApp(database: DatabaseHelper) {

    var selectedTab by remember { mutableIntStateOf(0) }
    var refresh by remember { mutableIntStateOf(0) }

    val moods = remember(refresh) {
        database.getAllMoods()
    }

    val dassResult = remember(refresh) {
        database.getLatestDassResult()
    }

    Scaffold(
        containerColor = TeduhBackground,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                val menus = listOf(
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
                        icon = {},
                        label = {
                            Text(
                                text = menu,
                                fontSize = 11.sp
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

                0 -> MoodScreen(
                    moods = moods,
                    onSave = { mood ->
                        val success = database.insertMood(mood)

                        if (success) {
                            refresh++
                        }

                        success
                    }
                )

                1 -> AnalysisScreen(moods)

                2 -> DassScreen(
                    onSave = { depression, anxiety, stress ->
                        val success = database.insertDassResult(
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

                3 -> RecommendationScreen(
                    moods = moods,
                    dassResult = dassResult
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TeduhCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
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
            content = content
        )
    }
}

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
            .padding(20.dp)
    ) {

        SectionTitle(
            "Catatan Mood",
            "Kenali dan catat perasaanmu hari ini."
        )

        TeduhCard {

            Text(
                "Bagaimana perasaanmu hari ini?",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                "${score.toInt()} / 5",
                fontSize = 30.sp,
                color = TeduhGreen,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = score,
                onValueChange = {
                    score = it
                },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = TeduhGreen,
                    activeTrackColor = TeduhGreen
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sangat buruk", fontSize = 11.sp)
                Text("Sangat baik", fontSize = 11.sp)
            }
        }

        TeduhCard {

            Text(
                "Pilih emosi yang kamu rasakan",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            emotions.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    FilterChip(
                        selected = emotion == item,
                        onClick = {
                            emotion = item
                        },
                        label = {
                            Text(item)
                        }
                    )
                }
            }
        }

        TeduhCard {

            Text(
                "Apa yang memengaruhi perasaanmu?",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            triggers.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = trigger == item,
                        onClick = {
                            trigger = item
                        },
                        label = {
                            Text(item)
                        }
                    )
                }
            }
        }

        TeduhCard {

            Text(
                "Ceritakan perasaanmu",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = journal,
                onValueChange = {
                    journal = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Tuliskan apa yang sedang kamu rasakan...")
                },
                minLines = 4
            )
        }

        Button(
            onClick = {
                val mood = Mood(
                    date = LocalDate.now().toString(),
                    score = score.toInt(),
                    emotion = emotion,
                    trigger = trigger,
                    journal = journal
                )

                val success = onSave(mood)

                Toast.makeText(
                    context,
                    if (success) "Mood berhasil disimpan"
                    else "Gagal menyimpan mood",
                    Toast.LENGTH_SHORT
                ).show()

                if (success) {
                    journal = ""
                    score = 3f
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeduhGreen
            )
        ) {
            Text("Simpan Catatan Mood")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Riwayat Mood",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TeduhGreen
        )

        if (moods.isEmpty()) {
            Text(
                "Belum ada catatan mood.",
                modifier = Modifier.padding(vertical = 15.dp),
                color = Color.Gray
            )
        }

        moods.take(10).forEach { mood ->
            TeduhCard {
                Text(
                    mood.date,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Text(
                    "${mood.emotion} • Skor ${mood.score}/5",
                    fontWeight = FontWeight.Bold
                )

                Text("Pemicu: ${mood.trigger}")

                if (mood.journal.isNotBlank()) {
                    Text(
                        mood.journal,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisScreen(moods: List<Mood>) {

    val today = LocalDate.now()

    val currentWeek = moods.filter {
        val date = LocalDate.parse(it.date)
        !date.isBefore(today.minusDays(6)) &&
                !date.isAfter(today)
    }

    val previousWeek = moods.filter {
        val date = LocalDate.parse(it.date)
        date.isBefore(today.minusDays(6)) &&
                !date.isBefore(today.minusDays(13))
    }

    val average = if (currentWeek.isNotEmpty()) {
        currentWeek.map { it.score }.average()
    } else {
        0.0
    }

    val previousAverage = if (previousWeek.isNotEmpty()) {
        previousWeek.map { it.score }.average()
    } else {
        0.0
    }

    val lowestMood = currentWeek.minByOrNull {
        it.score
    }

    val dominantTrigger = currentWeek
        .groupingBy { it.trigger }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    val difference = average - previousAverage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        SectionTitle(
            "Analisis Mood",
            "Pantau perkembangan suasana hatimu."
        )

        TeduhCard {
            Text(
                "Rata-rata Mood (7 Hari)",
                fontWeight = FontWeight.Bold
            )

            Text(
                if (currentWeek.isEmpty()) "Belum ada data"
                else String.format("%.1f / 5", average),
                fontSize = 30.sp,
                color = TeduhGreen,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Berdasarkan ${currentWeek.size} catatan mood."
            )
        }

        TeduhCard {
            Text(
                "Hari dengan Mood Terendah",
                fontWeight = FontWeight.Bold
            )

            Text(
                lowestMood?.let {
                    "${it.date} • ${it.emotion} (${it.score}/5)"
                } ?: "Belum ada data",
                fontSize = 16.sp
            )
        }

        TeduhCard {
            Text(
                "Pemicu yang Paling Sering Muncul",
                fontWeight = FontWeight.Bold
            )

            Text(
                dominantTrigger ?: "Belum ada data",
                fontSize = 18.sp,
                color = TeduhGreen
            )
        }

        TeduhCard {
            Text(
                "Perbandingan dengan 7 Hari Sebelumnya",
                fontWeight = FontWeight.Bold
            )

            if (currentWeek.isEmpty() || previousWeek.isEmpty()) {
                Text(
                    "Data belum cukup untuk dibandingkan."
                )
            } else {
                Text(
                    "Minggu ini: %.1f".format(average)
                )

                Text(
                    "Minggu sebelumnya: %.1f".format(previousAverage)
                )

                Text(
                    when {
                        difference > 0 -> "Mood meningkat sebesar %.1f poin.".format(difference)
                        difference < 0 -> "Mood menurun sebesar %.1f poin.".format(-difference)
                        else -> "Rata-rata mood tidak berubah."
                    },
                    color = TeduhGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TeduhCard {
            Text(
                "Riwayat Mood 7 Hari",
                fontWeight = FontWeight.Bold
            )

            currentWeek.sortedBy { it.date }.forEach {
                Text(
                    "${it.date} — ${it.score}/5 (${it.emotion})",
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }

            if (currentWeek.isEmpty()) {
                Text("Belum ada data mood.")
            }
        }
    }
}

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

@Composable
fun DassScreen(
    onSave: (Int, Int, Int) -> Boolean
) {

    val context = LocalContext.current

    val answers = remember {
        mutableStateMapOf<Int, Int>()
    }

    var showResult by remember {
        mutableStateOf(false)
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        SectionTitle(
            "Tes DASS-21",
            "Evaluasi kondisi emosionalmu dalam 7 hari terakhir."
        )

        TeduhCard {
            Text(
                "Petunjuk Pengisian",
                fontWeight = FontWeight.Bold
            )

            Text(
                "Pilih jawaban yang paling menggambarkan kondisi " +
                        "yang kamu alami selama seminggu terakhir."
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("0 = Tidak pernah")
            Text("1 = Kadang-kadang")
            Text("2 = Sering")
            Text("3 = Hampir selalu")
        }

        dassQuestions.forEachIndexed { index, question ->

            TeduhCard {

                Text(
                    "Pertanyaan ${index + 1}",
                    color = TeduhGreen,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(question)

                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    listOf(
                        "0 - Tidak pernah",
                        "1 - Kadang-kadang",
                        "2 - Sering",
                        "3 - Hampir selalu"
                    ).forEachIndexed { value, label ->

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = answers[index] == value,
                                onClick = {
                                    answers[index] = value
                                }
                            )

                            Text(
                                label,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (answers.size == 21) {
                    val success = onSave(
                        depression,
                        anxiety,
                        stress
                    )

                    if (success) {
                        showResult = true
                        Toast.makeText(
                            context,
                            "Hasil tes berhasil disimpan",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Gagal menyimpan hasil tes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Jawab seluruh 21 pertanyaan terlebih dahulu",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeduhGreen
            )
        ) {
            Text("Lihat Hasil Tes")
        }

        if (showResult) {

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Hasil Pemeriksaan",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhGreen
            )

            DassResultCard(
                "Depresi",
                depression,
                getDassCategory(depression, "depression")
            )

            DassResultCard(
                "Kecemasan",
                anxiety,
                getDassCategory(anxiety, "anxiety")
            )

            DassResultCard(
                "Stres",
                stress,
                getDassCategory(stress, "stress")
            )

            Text(
                "Catatan: DASS-21 merupakan alat skrining, bukan " +
                        "alat diagnosis. Hasilnya tidak menggantikan " +
                        "penilaian tenaga profesional. Gunakan versi " +
                        "terjemahan resmi dan tervalidasi untuk penggunaan penelitian.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
fun DassResultCard(
    title: String,
    score: Int,
    category: String
) {
    TeduhCard {
        Text(
            title,
            fontWeight = FontWeight.Bold
        )

        Text(
            "$score — $category",
            fontSize = 20.sp,
            color = TeduhGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecommendationScreen(
    moods: List<Mood>,
    dassResult: Map<String, Int>?
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        SectionTitle(
            "Rekomendasi Aktivitas",
            "Temukan aktivitas yang sesuai dengan kondisimu."
        )

        val latestMood = moods.firstOrNull()

        if (latestMood == null) {

            TeduhCard {
                Text(
                    "Belum Ada Data Mood",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Catat suasana hatimu terlebih dahulu " +
                            "agar TEDUH dapat memberikan rekomendasi."
                )
            }

        } else {

            val stressScore = dassResult?.get("stress") ?: 0

            val stressLevel = if (dassResult == null) {
                "Normal"
            } else {
                getDassCategory(stressScore, "stress")
            }

            TeduhCard {
                Text(
                    "Kondisi Terakhir",
                    fontWeight = FontWeight.Bold
                )

                Text("Emosi: ${latestMood.emotion}")
                Text("Pemicu: ${latestMood.trigger}")
                Text("Skor mood: ${latestMood.score}/5")

                Text(
                    "Kategori stres: $stressLevel"
                )
            }

            val recommendations =
                RecommendationHelper.getRecommendations(
                    emotion = latestMood.emotion,
                    trigger = latestMood.trigger,
                    stressLevel = stressLevel
                )

            Text(
                "3 Rekomendasi untukmu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TeduhGreen
            )

            recommendations.forEachIndexed { index, activity ->

                TeduhCard {

                    Text(
                        "Rekomendasi ${index + 1}",
                        color = TeduhGreen,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        activity.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        activity.description,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        "Durasi: ${activity.duration}",
                        color = TeduhGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
