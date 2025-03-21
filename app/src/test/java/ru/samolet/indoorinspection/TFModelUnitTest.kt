package ru.samolet.indoorinspection

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import ru.samolet.indoorinspection.presentation.NavigationViewModel
import ru.samolet.indoorinspection.presentation.NoInspection


class TFModelUnitTest {

    private lateinit var viewModel: NavigationViewModel

    @Before
    fun setup() {
        viewModel = NavigationViewModel()
    }

    @Test
    fun setFinishInspection() {
        viewModel.updateInspection(NoInspection)
        assertEquals(NoInspection, viewModel.selectedParameters.value)
    }
}