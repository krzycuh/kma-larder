package pl.kmazurek.larder.domain.model.stock

import java.time.LocalDate
import java.time.YearMonth

/**
 * Value Object: a date as printed on packaging or remembered for home-made food.
 * Always has a year; the month and the day are optional ("2027", "05.2027", "12.05.2027").
 * For comparisons the period is read as its last day: "2027" ends on 2027-12-31, "05.2027" on 2027-05-31.
 */
data class ApproximateDate(
    val year: Int,
    val month: Int? = null,
    val day: Int? = null,
) : Comparable<ApproximateDate> {
    init {
        require(year in MIN_YEAR..MAX_YEAR) { "Year must be between $MIN_YEAR and $MAX_YEAR" }
        month?.let { require(it in 1..12) { "Month must be between 1 and 12" } }
        if (day != null) {
            require(month != null) { "Day requires a month" }
            require(day in 1..YearMonth.of(year, month).lengthOfMonth()) { "Day $day is not valid for $year-$month" }
        }
    }

    /** The last day of the period: the day itself, the end of the month, or the end of the year. */
    val endDate: LocalDate
        get() =
            when {
                month == null -> LocalDate.of(year, 12, 31)
                day == null -> YearMonth.of(year, month).atEndOfMonth()
                else -> LocalDate.of(year, month, day)
            }

    /** The first day of the period. */
    val startDate: LocalDate
        get() = LocalDate.of(year, month ?: 1, day ?: 1)

    /**
     * Shifts the date by a number of months, keeping the precision: "2026" + 12 months = "2027",
     * "06.2026" + 12 months = "06.2027", "15.06.2026" + 12 months = "15.06.2027".
     */
    fun plusMonths(months: Int): ApproximateDate {
        require(months >= 0) { "Months must not be negative" }
        return when {
            month == null -> ApproximateDate(year + months / 12 + if (months % 12 == 0) 0 else 1)
            day == null -> YearMonth.of(year, month).plusMonths(months.toLong()).let { ApproximateDate(it.year, it.monthValue) }
            else ->
                LocalDate.of(
                    year,
                    month,
                    day,
                ).plusMonths(months.toLong()).let { ApproximateDate(it.year, it.monthValue, it.dayOfMonth) }
        }
    }

    fun isBefore(date: LocalDate): Boolean = endDate.isBefore(date)

    override fun compareTo(other: ApproximateDate): Int = endDate.compareTo(other.endDate)

    override fun toString(): String =
        when {
            month == null -> "$year"
            day == null -> "%02d.%d".format(month, year)
            else -> "%02d.%02d.%d".format(day, month, year)
        }

    companion object {
        const val MIN_YEAR = 1990
        const val MAX_YEAR = 2200

        fun of(date: LocalDate) = ApproximateDate(date.year, date.monthValue, date.dayOfMonth)
    }
}
