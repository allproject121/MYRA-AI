package com.myra.assistant.service

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Native Calendar Manager for MYRA Assistant.
 * Interfaces directly with Android CalendarContract for schedule inspection and event creation.
 */
class MyraCalendarManager(private val context: Context) {

    data class CalendarEventItem(
        val title: String,
        val startTime: Long,
        val endTime: Long,
        val location: String?
    )

    fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCalendarWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getUpcomingEvents(daysAhead: Int = 1): List<CalendarEventItem> {
        if (!hasCalendarReadPermission()) {
            return emptyList()
        }

        val events = mutableListOf<CalendarEventItem>()
        val startMillis = System.currentTimeMillis()
        val endMillis = startMillis + (daysAhead * 24 * 60 * 60 * 1000L)

        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(uriBuilder, startMillis)
        android.content.ContentUris.appendId(uriBuilder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val title = if (titleIdx != -1) it.getString(titleIdx) ?: "Untitled Event" else "Event"
                    val begin = if (beginIdx != -1) it.getLong(beginIdx) else startMillis
                    val end = if (endIdx != -1) it.getLong(endIdx) else begin + 30 * 60 * 1000L
                    val loc = if (locIdx != -1) it.getString(locIdx) else null

                    events.add(CalendarEventItem(title, begin, end, loc))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }

    fun scheduleEvent(
        title: String,
        startTimeMillis: Long,
        durationMinutes: Int = 30
    ): Pair<Boolean, String> {
        if (!hasCalendarWritePermission()) {
            // Fall back to launching device calendar app with pre-filled intent
            return try {
                val intent = Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTimeMillis + durationMinutes * 60 * 1000L)
                    .putExtra(CalendarContract.Events.TITLE, title)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Pair(true, "Calendar app opened to confirm '$title'")
            } catch (e: Exception) {
                Pair(false, "Calendar write permission required to schedule events directly.")
            }
        }

        return try {
            val endTimeMillis = startTimeMillis + (durationMinutes * 60 * 1000L)
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTimeMillis)
                put(CalendarContract.Events.DTEND, endTimeMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "Scheduled via MYRA AI Assistant")
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default primary calendar
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            val uri: Uri? = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (uri != null) {
                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeStr = sdf.format(Date(startTimeMillis))
                Pair(true, "Event '$title' scheduled for $timeStr.")
            } else {
                Pair(false, "Could not insert calendar event.")
            }
        } catch (e: Exception) {
            Pair(false, "Failed to schedule event: ${e.message}")
        }
    }
}
