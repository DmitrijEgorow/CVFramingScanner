package ru.samolet.indoorinspection.data

import ru.samolet.indoorinspection.presentation.ConcreteInspectionParameters


val windowItem = ConcreteInspectionParameters.InspectionItem(
    "window shade",
    "установлено окно"
)
val doorItem = ConcreteInspectionParameters.InspectionItem(
    "sliding door",
    "установлена входная дверь"
)
val radiatorItem = ConcreteInspectionParameters.InspectionItem(
    "radiator",
    "установлены батареи"
)

val sampleItems = arrayListOf(windowItem, doorItem, radiatorItem)
