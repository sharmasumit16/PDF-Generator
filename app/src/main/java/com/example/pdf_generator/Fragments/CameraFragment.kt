package com.example.pdf_generator.Fragments
import android.Manifest
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pdf_generator.Activities.MainActivity
import com.example.pdf_generator.UI.AppViewModel
import com.example.pdf_generator.databinding.FragmentCameraBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraFragment : Fragment()
{
    private var _binding:FragmentCameraBinding?=null
    private val binding get()=_binding!!
    private lateinit var viewModel: AppViewModel
    private var cameraManager:CameraManager?=null
    private var imgCapture:ImageCapture?=null
    private lateinit var getcameraID:String
    private val REQUEST_CODE = 20

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,

            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel=(activity as MainActivity).viewModel
        _binding= FragmentCameraBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)
        if (checkallPermission()) startCamera()
        else requestPermission()
        var captureBtnClicked =false
        binding.captureBtn.setOnClickListener{
            if(!captureBtnClicked){ binding.pbImgPt.visibility = View.VISIBLE }
            captureBtnClicked=true
            captureImage()
        }

        binding.homeBtn.setOnClickListener {
            val action=CameraFragmentDirections.actionCameraFragmentToHomeFragment()
            findNavController().navigate(action)
        }
        binding.saveBtn.setOnClickListener {
            if(viewModel.getListSize()==0) Toast.makeText(requireContext(), "Cannot create empty Pdf", Toast.LENGTH_SHORT).show()
            else {
                val action=CameraFragmentDirections.actionCameraFragmentToImagePreviewFragment()
                findNavController().navigate(action)
            }

        }


    }

    private
    fun checkallPermission() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode
        : Int,
        permissions
        : Array<String>,
        grantResults
        : IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (checkallPermission()) { startCamera() }
            else { Toast.makeText(requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun requestPermission() {
        val activityResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                var permissionGranted = true

                permissions.entries.forEach {
                        if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                            permissionGranted = false
                    }
                if (!permissionGranted) Toast.makeText(requireContext(), "Permission denied",Toast.LENGTH_SHORT).show()
                else startCamera()
            }
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                    .build()
                    .also{ it.setSurfaceProvider(binding.viewFinder.surfaceProvider)}
           binding.flashAutoBtn.setOnClickListener {
               ImageCapture.Builder().setFlashMode(FLASH_MODE_AUTO)
           }

            imgCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try{
                    cameraProvider.unbindAll()
                 val cam= cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imgCapture)
                        val camControl=cam.cameraControl
                binding.flashOnBtn
                    .setOnClickListener {
                    camControl.enableTorch(true)
                }
                binding.flashOffBtn.setOnClickListener {
                    camControl.enableTorch(false)
                }
                }
            catch (exc: Exception){
                    Log.e(TAG, "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(requireContext()))
    }





    private fun captureImage() {
        var imgCapture = imgCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(requireContext().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imgCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(output : ImageCapture.OutputFileResults) {

                    val savedUri = output.savedUri

                    val capturedBitmap = savedUri?.let { getBitmapFromUri(it) }
                    capturedBitmap?.let { it->
                        binding.clickedImageIv.visibility=View.VISIBLE
                        binding.clickedImageIv.setImageBitmap(it)
                        binding.pbImgPt.visibility=View.INVISIBLE
                        binding.saveBtn.isVisible=true
                        viewModel.addElementToList(it)
                    }
                }
                override fun onError(e: ImageCaptureException){
                    Log.e(TAG, "Photo capture failed: ${e.message}", e)
                }
            }
        )


    }

    private fun getBitmapFromUri(it: Uri): Bitmap? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to load image: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }
}