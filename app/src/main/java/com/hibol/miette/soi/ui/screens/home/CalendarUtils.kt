package com.hibol.miette.soi.ui.screens.home

import java.time.LocalDate
import java.time.YearMonth

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

fun buildCalendarDays(yearMonth: YearMonth): List<CalendarDay> {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()

    // Lundi = 1, Dimanche = 7 — on veut commencer la grille le lundi
    val firstDayOfWeek = firstDay.dayOfWeek.value

    val days = mutableListOf<CalendarDay>()

    // Jours du mois précédent pour remplir la première semaine
    for (i in firstDayOfWeek - 1 downTo 1) {
        days.add(CalendarDay(firstDay.minusDays(i.toLong()), false))
    }

    // Jours du mois courant
    for (day in 1..lastDay.dayOfMonth) {
        days.add(CalendarDay(yearMonth.atDay(day), true))
    }

    // Jours du mois suivant pour compléter la dernière semaine
    val remaining = 42 - days.size  // 6 semaines × 7 jours = 42
    for (i in 1..remaining) {
        days.add(CalendarDay(lastDay.plusDays(i.toLong()), false))
    }

    return days
}