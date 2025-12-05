package com.example.stepstreak.health

import android.util.Log
import androidx.core.i18n.DateTimeFormatter
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter.Companion.between
import com.example.stepstreak.PERMISSIONS
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.max

class HealthRepository(private val healthConnectClient: HealthConnectClient) {
    suspend fun readTodaySteps(): Long {
        val records = healthConnectClient.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = between(
                    LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant(), Instant.now()
                )
            )
        )
        val steps = records.records.sumOf { it.count }
        val db = Firebase.database   // <-- esto crea el objeto db
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return steps
        val today = android.text.format.DateFormat.format("yyyy-MM-dd", Date()).toString()
        val stepsToday = steps

        db.getReference("userDailySteps")
            .child(uid)
            .child(today)
            .setValue(stepsToday)
            .addOnSuccessListener { Log.d("DB", "Guardado OK") }
            .addOnFailureListener { e -> Log.e("DB", "Error guardando", e) }
        return steps
    }
    suspend fun readTotalSteps(fromDate: Instant): Long {
        val records = healthConnectClient.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = between(
                    fromDate, Instant.now()
                )
            )
        )
        val steps = records.records.sumOf { it.count }
        return steps
    }

    suspend fun hasPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(PERMISSIONS)
    }
}

fun calculateStreak(dailySteps: Map<String, Long>, goal: Long): Pair<Int, Int> {
    if (dailySteps.isEmpty()) return 0 to 0

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val days = dailySteps.entries
        .map {
            val date = formatter.parse(it.key)!!
            val localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            localDate to it.value
        }
        .sortedBy { it.first }

    var currentStreak = 0
    var bestStreak = 0
    var previousDay: LocalDate? = null

    for ((date, steps) in days) {
        val metGoal = steps >= goal

        if (previousDay == null) {
            currentStreak = if (metGoal) 1 else 0
        } else {
            if (date == previousDay!!.plusDays(1)) {
                currentStreak = if (metGoal) currentStreak + 1 else 0
            } else {
                currentStreak = if (metGoal) 1 else 0
            }
        }

        bestStreak = max(bestStreak, currentStreak)
        previousDay = date
    }

    return currentStreak to bestStreak
}
