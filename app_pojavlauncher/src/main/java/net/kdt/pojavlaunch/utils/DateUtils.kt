package net.kdt.pojavlaunch.utils

import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Tools
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

// Utils for date-based activation for certain launcher workarounds.
object DateUtils {
    /**
     * Parse the release date of a game version from the JMinecraftVersionList.Version time or releaseTime fields
     * @param releaseTime the time or releaseTime string from JMinecraftVersionList.Version
     * @return the date object
     * @throws ParseException if date parsing fails
     */
    @Throws(ParseException::class)
    fun parseReleaseDate(releaseTime: String?): Date? {
        var releaseTime = releaseTime
        if (releaseTime == null) return null
        val tIndexOf = releaseTime.indexOf('T')
        if (tIndexOf != -1) releaseTime = releaseTime.substring(0, tIndexOf)
        return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(releaseTime)
    }

    /**
     * Checks if the Date object is before the date denoted by
     * year, month, dayOfMonth parameters
     * @param date the Date object that we compare against
     * @param year the year
     * @param month the month (zero-based)
     * @param dayOfMonth the day of the month
     * @return true if the Date is before year, month, dayOfMonth, false otherwise
     */
    fun dateBefore(date: Date, year: Int, month: Int, dayOfMonth: Int): Boolean {
        return date.before(Date(GregorianCalendar(year, month, dayOfMonth).getTimeInMillis()))
    }

    /**
     * Extracts the original release date of a game version, ignoring any mods (if present)
     * @param gameVersion the JMinecraftVersionList.Version object
     * @return the game's original release date
     */
    @Throws(ParseException::class)
    fun getOriginalReleaseDate(gameVersion: JMinecraftVersionList.Version): Date? {
        var gameVersion = gameVersion
        if (Tools.isValidString(gameVersion.inheritsFrom)) {
            gameVersion = Tools.getVersionInfo(gameVersion.inheritsFrom, true)
        } else {
            // The launcher's inheritor mutilates the version object, causing it to have the original
            // version's ID but modded version's dates. Work around it by re-reading the version without
            // inheriting.
            gameVersion = Tools.getVersionInfo(gameVersion.id, true)
        }
        return parseReleaseDate(gameVersion.releaseTime)
    }
}
