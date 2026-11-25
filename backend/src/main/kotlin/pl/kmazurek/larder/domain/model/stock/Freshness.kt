package pl.kmazurek.larder.domain.model.stock

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FreshnessStatus {
    OK,
    EXPIRING_SOON,
    EXPIRED,
    UNKNOWN,
}

/**
 * Domain policy: how a best-before date reads today. "Soon" is a number of days chosen by the caller
 * (30 by default), so the same batch can be "soon" on the dashboard and "fine" in a yearly overview.
 */
object Freshness {
    const val DEFAULT_SOON_DAYS = 30

    fun status(
        expiresOn: LocalDate?,
        today: LocalDate,
        soonDays: Int = DEFAULT_SOON_DAYS,
    ): FreshnessStatus =
        when {
            expiresOn == null -> FreshnessStatus.UNKNOWN
            expiresOn.isBefore(today) -> FreshnessStatus.EXPIRED
            !expiresOn.isAfter(today.plusDays(soonDays.toLong())) -> FreshnessStatus.EXPIRING_SOON
            else -> FreshnessStatus.OK
        }

    /** Days from today to the date (negative when past); null without a date. */
    fun daysLeft(
        expiresOn: LocalDate?,
        today: LocalDate,
    ): Long? = expiresOn?.let { ChronoUnit.DAYS.between(today, it) }

    /** The worst of several statuses: EXPIRED beats EXPIRING_SOON beats OK; UNKNOWN only when nothing else is known. */
    fun worst(statuses: Collection<FreshnessStatus>): FreshnessStatus =
        when {
            FreshnessStatus.EXPIRED in statuses -> FreshnessStatus.EXPIRED
            FreshnessStatus.EXPIRING_SOON in statuses -> FreshnessStatus.EXPIRING_SOON
            FreshnessStatus.OK in statuses -> FreshnessStatus.OK
            else -> FreshnessStatus.UNKNOWN
        }
}
