
package com.example.teduh.utils

data class ActivityRecommendation(
    val name: String,
    val description: String,
    val emotion: String,
    val trigger: String,
    val duration: String
)

object RecommendationHelper {

    private val activities = listOf(

        ActivityRecommendation(
            "Latihan Pernapasan",
            "Atur napas secara perlahan untuk membantu tubuh lebih rileks.",
            "Cemas, Stres, Marah",
            "Pekerjaan, Akademik, Hubungan",
            "5 menit"
        ),

        ActivityRecommendation(
            "Mendengarkan Musik",
            "Dengarkan musik yang menenangkan dan nikmati suasananya.",
            "Sedih, Stres, Lelah",
            "Pekerjaan, Akademik, Lingkungan",
            "10 menit"
        ),

        ActivityRecommendation(
            "Menulis Jurnal",
            "Tuliskan perasaan dan hal-hal yang sedang kamu pikirkan.",
            "Sedih, Cemas, Marah",
            "Hubungan, Keluarga, Akademik",
            "10 menit"
        ),

        ActivityRecommendation(
            "Berjalan Santai",
            "Lakukan jalan santai untuk menyegarkan pikiran.",
            "Stres, Lelah, Bosan",
            "Pekerjaan, Akademik, Lingkungan",
            "15 menit"
        ),

        ActivityRecommendation(
            "Meditasi",
            "Luangkan waktu untuk menenangkan pikiran dan fokus pada diri sendiri.",
            "Cemas, Stres, Marah",
            "Pekerjaan, Hubungan, Akademik",
            "10 menit"
        ),

        ActivityRecommendation(
            "Berbicara dengan Teman",
            "Ceritakan perasaanmu kepada orang yang kamu percaya.",
            "Sedih, Cemas, Kesepian",
            "Hubungan, Keluarga, Lingkungan",
            "15 menit"
        ),

        ActivityRecommendation(
            "Istirahat Sejenak",
            "Berikan waktu bagi tubuh dan pikiran untuk beristirahat.",
            "Lelah, Stres, Bosan",
            "Pekerjaan, Akademik, Lingkungan",
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
                if (activity.emotion.contains(
                        emotion,
                        ignoreCase = true
                    )
                ) 1 else 0

            val triggerMatch =
                if (activity.trigger.contains(
                        trigger,
                        ignoreCase = true
                    )
                ) 1 else 0

            val durationMatch =
                if (
                    stressLevel == "Normal" ||
                    stressLevel == "Mild"
                ) {

                    if (activity.duration == "15 menit") 1 else 0

                } else {

                    if (
                        activity.duration == "5 menit" ||
                        activity.duration == "10 menit"
                    ) 1 else 0
                }

            val score =
                (emotionMatch * 3) +
                        (triggerMatch * 2) +
                        (durationMatch * 1)

            Pair(activity, score)

        }.sortedByDescending {
            it.second
        }.take(3).map {
            it.first
        }
    }
}
