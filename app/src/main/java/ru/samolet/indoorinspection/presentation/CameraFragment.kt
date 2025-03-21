package ru.samolet.indoorinspection.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import ru.samolet.indoorinspection.IndoorInspectionApplication
import ru.samolet.indoorinspection.R
import ru.samolet.indoorinspection.data.AvailableModels
import ru.samolet.indoorinspection.data.CacheRepository
import ru.samolet.indoorinspection.data.EnsembleHelper
import ru.samolet.indoorinspection.data.ImageClassifierHelper
import ru.samolet.indoorinspection.data.VotingClassifier
import ru.samolet.indoorinspection.databinding.FragmentCameraBinding


class CameraFragment : Fragment(), ImageClassifierHelper.ClassifierListener {

    private var fragmentCameraBinding: FragmentCameraBinding? = null

    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var bitmapBuffer: Bitmap
    private val classificationResultsAdapter by lazy {
        ClassificationResultsAdapter().apply {
            updateAdapterSize(imageClassifierHelper.maxResults)
        }
    }
    private val viewModel: NavigationViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var ensembleHelper = EnsembleHelper()
    private var votingClassifier = VotingClassifier(3)
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cacheRepository: CacheRepository

    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()

        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(R.id.action_camera_to_permissions)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        cacheRepository =
            (requireActivity().application as IndoorInspectionApplication).cacheRepository
        return fragmentCameraBinding!!.root
    }

    override fun onDestroyView() {
        fragmentCameraBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageClassifierHelper =
            ImageClassifierHelper(context = requireContext(), imageClassifierListener = this)

        with(fragmentCameraBinding?.recyclerviewResults) {
            this?.layoutManager = LinearLayoutManager(requireContext())
            this?.adapter = classificationResultsAdapter
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        fragmentCameraBinding?.viewFinder?.post {
            setUpCamera()
        }

        initBottomSheetControls()

        lifecycleScope.launch {
            if (viewModel.selectedParameters.value is ConcreteInspectionParameters) {
                val inspectionText =
                    (viewModel.selectedParameters.value as ConcreteInspectionParameters).inspectionItem.item
                // findNavController().navigate(R.id.action_camera_to_permissions)
            }
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun initBottomSheetControls() {
        fragmentCameraBinding?.bottomSheetLayout?.thresholdMinus?.setOnClickListener {
            if (imageClassifierHelper.threshold >= 0.1) {
                imageClassifierHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        fragmentCameraBinding?.bottomSheetLayout?.thresholdPlus?.setOnClickListener {
            if (imageClassifierHelper.threshold < 0.9) {
                imageClassifierHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        fragmentCameraBinding?.bottomSheetLayout?.maxResultsMinus?.setOnClickListener {
            if (imageClassifierHelper.maxResults > 1) {
                imageClassifierHelper.maxResults--
                updateControlsUi()
                classificationResultsAdapter.updateAdapterSize(size = imageClassifierHelper.maxResults)
            }
        }

        fragmentCameraBinding?.bottomSheetLayout?.maxResultsPlus?.setOnClickListener {
            if (imageClassifierHelper.maxResults < 3) {
                imageClassifierHelper.maxResults++
                updateControlsUi()
                classificationResultsAdapter.updateAdapterSize(size = imageClassifierHelper.maxResults)
            }
        }

        fragmentCameraBinding?.bottomSheetLayout?.threadsMinus?.setOnClickListener {
            if (imageClassifierHelper.numThreads > 1) {
                imageClassifierHelper.numThreads--
                updateControlsUi()
            }
        }

        fragmentCameraBinding?.bottomSheetLayout?.threadsPlus?.setOnClickListener {
            if (imageClassifierHelper.numThreads < 4) {
                imageClassifierHelper.numThreads++
                updateControlsUi()
            }
        }

        // CPU, GPU, or NNAPI
        fragmentCameraBinding?.bottomSheetLayout?.spinnerDelegate?.setSelection(0, false)
        fragmentCameraBinding?.bottomSheetLayout?.spinnerDelegate?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentDelegate = position
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Any()
                }
            }

        fragmentCameraBinding?.bottomSheetLayout?.spinnerModel?.setSelection(0, false)
        fragmentCameraBinding?.bottomSheetLayout?.spinnerModel?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    imageClassifierHelper.currentModel = AvailableModels.getModelByIndex(position)
                    updateControlsUi()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Any()
                }
            }
    }

    private fun updateControlsUi() {
        fragmentCameraBinding?.bottomSheetLayout?.maxResultsValue?.text =
            imageClassifierHelper.maxResults.toString()
        fragmentCameraBinding?.bottomSheetLayout?.thresholdValue?.text =
            String.format("%.2f", imageClassifierHelper.threshold)
        fragmentCameraBinding?.bottomSheetLayout?.threadsValue?.text =
            imageClassifierHelper.numThreads.toString()
        imageClassifierHelper.clearImageClassifier()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fragmentCameraBinding?.viewFinder?.display?.rotation?.let {
            imageAnalyzer?.targetRotation = it
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(
                    fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0
                )
                .build()

        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(
                    fragmentCameraBinding?.viewFinder?.display?.rotation ?: Surface.ROTATION_0
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        classifyImage(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            // CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(fragmentCameraBinding?.viewFinder?.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun getScreenOrientation(): Int {
        val outMetrics = DisplayMetrics()

        val display: Display?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            display = requireActivity().display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            display = requireActivity().windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }

        return display?.rotation ?: 0
    }

    private fun classifyImage(image: ImageProxy) {
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        imageClassifierHelper.classify(bitmapBuffer, getScreenOrientation())
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            classificationResultsAdapter.updateResults(null)
            classificationResultsAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResults(
        results: List<Classifications>?,
        inferenceTime: Long
    ) {
        val ensembleResults =
            if (imageClassifierHelper.currentModel is AvailableModels.MobileNetV3) {
                ensembleHelper.convertClassificationsToList(results, true)
            } else {
                ensembleHelper.convertClassificationsToList(results)
            }

        lifecycleScope.launch {
            cacheRepository.saveData(ensembleResults.toString())
        }
        with(viewModel.selectedParameters) {
            if (value is ConcreteInspectionParameters &&
                ensembleResults.containsKey((value as ConcreteInspectionParameters).inspectionItem.item)
            ) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        this@CameraFragment.findNavController()
                            .navigate(R.id.action_camera_to_permissions)
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, e.toString())
                    }
                }, 100)
                return
            }
        }
        activity?.runOnUiThread {
            classificationResultsAdapter.updateResults(results)
            classificationResultsAdapter.notifyDataSetChanged()
            fragmentCameraBinding?.bottomSheetLayout?.inferenceTimeVal?.text =
                String.format("%d ms", inferenceTime)
        }
    }

    companion object {
        private const val TAG = "Image Classifier"
    }
}