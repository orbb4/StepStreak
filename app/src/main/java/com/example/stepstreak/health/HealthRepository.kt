package com.example.stepstreak.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter.Companion.between
import com.example.stepstreak.PERMISSIONS
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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