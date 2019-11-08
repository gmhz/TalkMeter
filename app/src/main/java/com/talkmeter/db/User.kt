package com.talkmeter.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class User(
    @PrimaryKey val uid: Int? = null,
    @ColumnInfo(name = "full_name") val fullName: String?,
    @ColumnInfo(name = "spoke_time") var spokeTime: Int = 0,
    @ColumnInfo(name = "u_category") val uCategory: Int = 1,
    @ColumnInfo(name = "u_selected") var uSelected: Boolean = false
) {
    fun nameTime(): String {
        return "$fullName: ${formatSeconds(spokeTime)}"
    }


    private fun formatSeconds(timeInSeconds: Int): String {
        val hours = timeInSeconds / 3600
        val secondsLeft = timeInSeconds - hours * 3600
        val minutes = secondsLeft / 60
        val seconds = secondsLeft - minutes * 60

        var formattedTime = ""
        if (hours > 0) {
            if (hours < 10)
                formattedTime += "0"
            formattedTime += "$hours:"
        }
        if (minutes > 0) {
            if (minutes < 10)
                formattedTime += "0"
            formattedTime += "$minutes:"
        }

        if (seconds in 0..9 && minutes > 0)
            formattedTime += "0"
        formattedTime += seconds

        return formattedTime
    }
}