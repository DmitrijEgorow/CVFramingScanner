package ru.samolet.indoorinspection.presentation

sealed class InspectionParameters

data object NoInspection : InspectionParameters()

data class ConcreteInspectionParameters(
    val floor: Int,
    val flat: Int,
    val inspectionItem: InspectionItem
): InspectionParameters() {
    data class InspectionItem(
        val item: String,
        val description: String
    )
}