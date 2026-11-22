package com.example.teduh

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================
// WARNA (khusus file ini)
// =====================

private val CGreen = Color(0xFF477A68)
private val CLight = Color(0xFFE3EFEA)
private val CBg = Color(0xFFF4F7F5)
private val CDark = Color(0xFF263A33)
private val CGray = Color(0xFF7A8782)
private val CWarm = Color(0xFFFFF3E6)
private val CWarmText = Color(0xFF8A5A1F)

// =====================
// MODEL (data contoh, belum pakai backend)
// =====================

private data class Comment(val nickname: String, val text: String)

private data class Post(
    val id: Int,
    val nickname: String,
    val emoji: String,
    val topic: String,
    val content: String,
    val time: String,
    val reactions: Map<String, Int>,
    val myReactions: Set<String> = emptySet(),
    val comments: List<Comment> = emptyList()
)

private val reactionTypes = listOf(
    Pair("🤗", "Peluk"),
    Pair("🤝", "Aku juga"),
    Pair("💪", "Semangat")
)

private val communityTopics = listOf(
    "Cemas akademik", "Burnout kerja", "Kesepian", "Hubungan", "Syukur"
)

// Kata kunci krisis. Pesan yang mengandung ini TIDAK dipublikasikan
// dan pengguna diarahkan ke bantuan profesional.
// Di versi nyata: gunakan moderasi manusia + penyaring yang lebih baik.
private val crisisKeywords = listOf(
    "bunuh diri", "mengakhiri hidup", "akhiri hidup", "ingin mati",
    "lebih baik mati", "tidak ingin hidup", "menyakiti diri",
    "self harm", "self-harm", "overdosis"
)

private fun containsCrisis(text: String): Boolean {
    val lower = text.lowercase()
    return crisisKeywords.any { lower.contains(it) }
}

private fun samplePosts() = listOf(
    Post(
        1, "Senja_Tenang", "🌅", "Cemas akademik",
        "Minggu ini ada tiga ujian dan aku merasa dadaku sesak tiap malam. " +
                "Ada yang punya cara supaya bisa tidur lebih tenang?",
        "2 jam lalu",
        mapOf("Peluk" to 8, "Aku juga" to 14, "Semangat" to 5),
        comments = listOf(
            Comment("Embun_Pagi", "Aku dulu menulis daftar kekhawatiran sebelum tidur, lumayan membantu."),
            Comment("Laut_Sunyi", "Napas 4-4-6 sebelum tidur juga bisa dicoba. Semangat ya!")
        )
    ),
    Post(
        2, "Daun_Hijau", "🍃", "Burnout kerja",
        "Dulu aku hampir menyerah dengan pekerjaanku. Yang menolong adalah belajar " +
                "bilang 'tidak' dan istirahat tanpa rasa bersalah. Pelan-pelan membaik.",
        "5 jam lalu",
        mapOf("Peluk" to 12, "Aku juga" to 21, "Semangat" to 17),
        comments = listOf(Comment("Bintang_Kecil", "Terima kasih sudah berbagi, ini menguatkan."))
    ),
    Post(
        3, "Hujan_Sore", "🌧️", "Kesepian",
        "Sejak pindah kota aku jarang ngobrol dengan siapa pun. Rasanya sepi, " +
                "tapi aku juga bingung mulai dari mana.",
        "Kemarin",
        mapOf("Peluk" to 19, "Aku juga" to 26, "Semangat" to 9),
        comments = listOf(
            Comment("Senja_Tenang", "Aku pernah di posisi itu. Coba ikut satu komunitas hobi kecil dulu."),
            Comment("Awan_Putih", "Kamu tidak sendirian di sini 🤍")
        )
    ),
    Post(
        4, "Mentari_Pagi", "☀️", "Syukur",
        "Hari ini kecil saja: berhasil mandi, makan, dan jalan kaki 10 menit. " +
                "Bangga sama diri sendiri.",
        "Kemarin",
        mapOf("Peluk" to 6, "Aku juga" to 11, "Semangat" to 23)
    )
)

// =====================
// LAYAR UTAMA KOMUNITAS
// =====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen() {

    val context = LocalContext.current
    val myNickname = "Teduh_Pendengar"

    val posts = remember { mutableStateListOf<Post>().apply { addAll(samplePosts()) } }
    val blocked = remember { mutableStateListOf<String>() }

    var filter by remember { mutableStateOf("Semua") }
    var showRules by remember { mutableStateOf(true) }
    var showCrisis by remember { mutableStateOf(false) }
    var composing by remember { mutableStateOf(false) }
    var draftTopic by remember { mutableStateOf(communityTopics.first()) }
    var draft by remember { mutableStateOf("") }
    var nextId by remember { mutableIntStateOf(100) }

    fun updatePost(id: Int, transform: (Post) -> Post) {
        val index = posts.indexOfFirst { it.id == id }
        if (index >= 0) posts[index] = transform(posts[index])
    }

    val visiblePosts = posts.filter {
        it.nickname !in blocked && (filter == "Semua" || it.topic == filter)
    }

    // ---------- Dialog aturan ----------
    if (showRules) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(24.dp),
            title = { Text("Aturan Ruang Bersama 🤝", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Ini ruang dukungan sebaya, bukan pengganti psikolog atau dokter.", fontSize = 14.sp)
                    Text("• Saling menghargai. Tidak ada perundungan atau penghakiman.", fontSize = 14.sp)
                    Text("• Jangan membagikan data pribadi (nama asli, nomor, alamat).", fontSize = 14.sp)
                    Text("• Jangan membahas cara menyakiti diri atau dosis obat.", fontSize = 14.sp)
                    Text("• Jangan memberi saran medis. Sarankan bantuan profesional.", fontSize = 14.sp)
                    Text("• Gunakan tombol ⋯ untuk melaporkan atau memblokir.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showRules = false }) {
                    Text("Saya mengerti & setuju", color = CGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ---------- Dialog krisis ----------
    if (showCrisis) {
        AlertDialog(
            onDismissRequest = { showCrisis = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Kamu tidak sendirian 💚", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Kami membaca bahwa kamu mungkin sedang sangat berat. " +
                                "Pesanmu tidak dipublikasikan, tapi perasaanmu penting.",
                        fontSize = 14.sp
                    )
                    Text(
                        "Tolong hubungi orang yang kamu percaya atau tenaga profesional sekarang. " +
                                "Jika ada bahaya langsung bagi dirimu, hubungi layanan darurat setempat.",
                        fontSize = 14.sp
                    )
                    // TODO: verifikasi nomor ini sebelum rilis & tambahkan layanan konseling kampus/daerahmu.
                    Text(
                        "Layanan kesehatan jiwa Kemenkes: 119 lalu tekan 8",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CGreen
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:119")))
                }) {
                    Text("📞 Telepon 119", color = CGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCrisis = false }) {
                    Text("Tutup", color = CGray)
                }
            }
        )
    }

    ScreenScaffold(
        title = "Ruang Bersama",
        subtitle = "Berbagi cerita dengan sesama, secara anonim.",
        emoji = "🤝"
    ) {

        // Identitas anonim
        TeduhCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CLight),
                    contentAlignment = Alignment.Center
                ) { Text("🫶", fontSize = 22.sp) }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Kamu tampil sebagai", fontSize = 12.sp, color = CGray)
                    Text(myNickname, fontWeight = FontWeight.Bold, color = CDark)
                }

                TextButton(onClick = { showRules = true }) {
                    Text("Aturan", color = CGreen, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Catatan penting
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CWarm)
                .padding(14.dp)
        ) {
            Text(
                "💡 Ini dukungan sesama pengguna, bukan terapi. Jika kondisimu berat, " +
                        "pertimbangkan berbicara dengan tenaga profesional.",
                fontSize = 12.sp,
                color = CWarmText
            )
        }

        // Filter topik
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectChip("Semua", filter == "Semua") { filter = "Semua" }
            communityTopics.forEach { topic ->
                SelectChip(topic, filter == topic) { filter = topic }
            }
        }

        // Tombol tulis / composer
        if (!composing) {
            PrimaryButton("✍️  Tulis cerita") { composing = true }
        } else {
            TeduhCard {
                CardTitle("Ceritamu", "✍️")

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    communityTopics.forEach { topic ->
                        SelectChip(topic, draftTopic == topic) { draftTopic = topic }
                    }
                }

                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Apa yang ingin kamu bagikan? (tanpa data pribadi)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { composing = false; draft = "" },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Batal", color = CGray) }

                    Button(
                        onClick = {
                            when {
                                draft.trim().length < 10 -> Toast.makeText(
                                    context, "Tulis minimal 10 karakter.", Toast.LENGTH_SHORT
                                ).show()

                                containsCrisis(draft) -> {
                                    showCrisis = true
                                    draft = ""
                                    composing = false
                                }

                                else -> {
                                    posts.add(
                                        0,
                                        Post(
                                            id = nextId++,
                                            nickname = myNickname,
                                            emoji = "🫶",
                                            topic = draftTopic,
                                            content = draft.trim(),
                                            time = "Baru saja",
                                            reactions = emptyMap()
                                        )
                                    )
                                    draft = ""
                                    composing = false
                                    Toast.makeText(context, "Ceritamu dibagikan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CGreen)
                    ) { Text("Bagikan", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Daftar cerita
        if (visiblePosts.isEmpty()) {
            TeduhCard {
                Text("🌱", fontSize = 40.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Text(
                    "Belum ada cerita di topik ini.",
                    color = CGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        visiblePosts.forEach { post ->
            PostCard(
                post = post,
                isMine = post.nickname == myNickname,
                onReact = { label ->
                    updatePost(post.id) { p ->
                        val had = label in p.myReactions
                        val count = p.reactions[label] ?: 0
                        p.copy(
                            reactions = p.reactions + (label to (if (had) count - 1 else count + 1)),
                            myReactions = if (had) p.myReactions - label else p.myReactions + label
                        )
                    }
                },
                onComment = { text ->
                    if (containsCrisis(text)) {
                        showCrisis = true
                        false
                    } else {
                        updatePost(post.id) { p ->
                            p.copy(comments = p.comments + Comment(myNickname, text.trim()))
                        }
                        true
                    }
                },
                onReport = {
                    Toast.makeText(
                        context,
                        "Laporan terkirim. Moderator akan meninjau.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onBlock = {
                    blocked.add(post.nickname)
                    Toast.makeText(context, "${post.nickname} diblokir.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Text(
            "Prototipe: data contoh, belum tersambung ke server.",
            fontSize = 11.sp,
            color = CGray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

// =====================
// KARTU CERITA
// =====================

@Composable
private fun PostCard(
    post: Post,
    isMine: Boolean,
    onReact: (String) -> Unit,
    onComment: (String) -> Boolean,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    TeduhCard {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CLight),
                contentAlignment = Alignment.Center
            ) { Text(post.emoji, fontSize = 22.sp) }

            Column(modifier = Modifier.weight(1f)) {
                Text(post.nickname, fontWeight = FontWeight.Bold, color = CDark)
                Text(post.time, fontSize = 12.sp, color = CGray)
            }

            if (!isMine) {
                Box {
                    Text(
                        "⋯",
                        fontSize = 24.sp,
                        color = CGray,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { menuOpen = true }
                            .padding(horizontal = 10.dp)
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("🚩 Laporkan") },
                            onClick = { menuOpen = false; onReport() }
                        )
                        DropdownMenuItem(
                            text = { Text("🚫 Blokir pengguna") },
                            onClick = { menuOpen = false; onBlock() }
                        )
                    }
                }
            }
        }

        Badge(post.topic, CGreen)

        Text(post.content, fontSize = 15.sp, color = CDark, lineHeight = 22.sp)

        // Reaksi
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reactionTypes.forEach { (emoji, label) ->
                val selected = label in post.myReactions
                val count = post.reactions[label] ?: 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) CGreen else CLight)
                        .clickable { onReact(label) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        "$emoji $count",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else CDark
                    )
                }
            }
        }

        Text(
            if (showComments) "Sembunyikan balasan" else "💬 ${post.comments.size} balasan",
            fontSize = 13.sp,
            color = CGreen,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { showComments = !showComments }
        )

        if (showComments) {

            post.comments.forEach { c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CBg)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(c.nickname, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CGreen)
                    Text(c.text, fontSize = 14.sp, color = CDark)
                }
            }

            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Tulis balasan yang suportif...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    TextButton(onClick = {
                        if (commentText.isNotBlank()) {
                            if (onComment(commentText)) commentText = "" else commentText = ""
                        }
                    }) {
                        Text("Kirim", color = CGreen, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}