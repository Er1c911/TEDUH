
package com.example.teduh.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.teduh.model.Mood

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
        db.execSQL("DROP TABLE IF EXISTS moods")
        db.execSQL("DROP TABLE IF EXISTS dass_results")
        onCreate(db)
    }

    fun insertMood(mood: Mood): Boolean {

        val values = ContentValues().apply {
            put("date", mood.date)
            put("score", mood.score)
            put("emotion", mood.emotion)
            put("trigger_name", mood.trigger)
            put("journal", mood.journal)
        }

        return writableDatabase.insert(
            "moods",
            null,
            values
        ) != -1L
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
                        id = it.getInt(
                            it.getColumnIndexOrThrow("id")
                        ),
                        date = it.getString(
                            it.getColumnIndexOrThrow("date")
                        ),
                        score = it.getInt(
                            it.getColumnIndexOrThrow("score")
                        ),
                        emotion = it.getString(
                            it.getColumnIndexOrThrow("emotion")
                        ),
                        trigger = it.getString(
                            it.getColumnIndexOrThrow("trigger_name")
                        ),
                        journal = it.getString(
                            it.getColumnIndexOrThrow("journal")
                        )
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
