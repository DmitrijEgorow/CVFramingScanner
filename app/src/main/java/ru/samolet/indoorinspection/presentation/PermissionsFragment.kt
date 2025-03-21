package ru.samolet.indoorinspection.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.samolet.indoorinspection.IndoorInspectionApplication
import ru.samolet.indoorinspection.R
import ru.samolet.indoorinspection.databinding.FragmentPermissionsBinding
import java.util.Random
import ru.samolet.indoorinspection.data.FeatureService
import ru.samolet.indoorinspection.data.windowItem

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PermissionsFragment : Fragment() {

    private var fragmentPermissionsBinding: FragmentPermissionsBinding? = null
    private val viewModel: NavigationViewModel by activityViewModels()
    private val launchCameraStateFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    private lateinit var rnd: Random
    private lateinit var featureService: FeatureService

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(
                    context,
                    getString(R.string.permissions_granted_text),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    getString(R.string.permissions_not_granted_text),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentPermissionsBinding = FragmentPermissionsBinding.inflate(inflater, container, false)
        rnd = (requireActivity().application as IndoorInspectionApplication).rnd
        featureService = (requireActivity().application as IndoorInspectionApplication).featureService
        return fragmentPermissionsBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentPermissionsBinding?.proceedButton?.setOnClickListener {
            launchCameraStateFlow.value = (launchCameraStateFlow.value + 1).mod(3)
        }
        viewModel.updateInspection(
            ConcreteInspectionParameters(
                featureService.getDefaultValue(),
                featureService.getDefaultValue(),
                windowItem
            )
        )
        lifecycleScope.launch {
            waitUntilDestination()
        }
        lifecycleScope.launch {
            viewModel.selectedParameters.collect { v ->
                when (v) {
                    is ConcreteInspectionParameters -> {
                        val inspectionText =
                            getString(
                                R.string.inspection_invitation_text_with_params,
                                v.flat.toString(),
                                v.floor.toString(),
                                v.inspectionItem.description
                            )
                        fragmentPermissionsBinding?.textDirections?.text = inspectionText

                    }

                    is NoInspection -> fragmentPermissionsBinding?.textDirections?.text =
                        getString(R.string.finished)
                }

            }
        }
    }

    private suspend fun waitUntilDestination() {
        launchCameraStateFlow.collect { launchCamera ->
            when (launchCamera) {
                2 -> {
                    launchCameraStateFlow.emit(0)
                    navigateToCamera()
                }

                1 -> {
                    fragmentPermissionsBinding?.proceedButton?.text = getString(R.string.i_am_here)
                    fragmentPermissionsBinding?.cancelButton?.isActivated = false
                    context?.getColor(R.color.gray)
                        ?.let { fragmentPermissionsBinding?.cancelButton?.setBackgroundColor(it) }
                }
            }
        }

    }

    private fun navigateToCamera() {
        findNavController().navigate(R.id.action_permissions_to_camera)
    }

    companion object {

        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}