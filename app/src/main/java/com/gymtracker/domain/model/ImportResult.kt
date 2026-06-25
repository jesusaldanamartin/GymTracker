package com.gymtracker.domain.model

sealed class ImportResult {
    data class Success(
        val mergedSessions: List<Session>,
        val newSessions: Int,
        val updatedSessions: Int,
        val newSets: Int,
        val skippedRows: Int,
        val newCustomExercises: List<Exercise> = emptyList()
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}