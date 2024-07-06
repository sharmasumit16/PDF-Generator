package com.example.pdf_generator.Fragments

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.pdf_generator.R
import com.example.pdf_generator.databinding.FragmentScannerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE
import kotlinx.coroutines.NonCancellable.cancel
import java.nio.ByteBuffer
import java.util.concurrent.Executors


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
class ScannerFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentScannerBinding?=null
    private val binding get()=_binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
            _binding= FragmentScannerBinding.inflate(inflater, container, false)
            return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkallPermission()) startCamera()
        else requestPermission()
    }

    fun checkallPermission() = CameraFragment.REQUIRED_PERMISSIONS.all{
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
                    if (it.key in CameraFragment.REQUIRED_PERMISSIONS && it.value == false)
                        permissionGranted = false
                }
                if (!permissionGranted) Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                else startCamera()
            }
        activityResultLauncher.launch(CameraFragment.REQUIRED_PERMISSIONS)
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider : ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also{ it.setSurfaceProvider(binding.viewFinder.surfaceProvider)}

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val executor = Executors.newSingleThreadExecutor()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        val qrCodeResult = decodeQRCode(imageProxy)
                        processQRCodeResult(qrCodeResult)
                    }
                }

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        }, ContextCompat.getMainExecutor(requireContext()))
//        binding.midButton.visibility=View.VISIBLE
    }

    private var isDialogBoxShowing = false

    private fun processQRCodeResult(result: String?) {
//        binding.midButton.visibility=View.VISIBLE
        result?.let {
            if (!isDialogBoxShowing) {
                isDialogBoxShowing = true
                val handler = Handler(Looper.getMainLooper())
                val runnable = Runnable {
                    buildDialogBox(it)
                }
                handler.post(runnable)
            }
        }
    }

    private fun buildDialogBox(res: String){
//        binding.midButton.visibility=View.VISIBLE
        Log.w("check", "creating dialogBox")
        val builder=MaterialAlertDialogBuilder(requireContext())
            .setTitle("Link Found")
            .setMessage(res)
            .setNeutralButton("Close") { _, which ->
            }
            .setNegativeButton("Scan other Code") { _, which ->
            }
            .setPositiveButton("Open Link") { _, which ->
            }
        val dialog=builder.show()
        dialog.getButton(-1).setOnClickListener {

            dialog.dismiss()
            isDialogBoxShowing=false
            openInWeb(res)
        }
        dialog.getButton(-2).setOnClickListener {

            dialog.dismiss()
            isDialogBoxShowing=false
        }
        dialog.getButton(-3).setOnClickListener {
            dialog.dismiss()
            isDialogBoxShowing=false
            navigateToHome()
        }



    }
    private fun openInWeb(res: String){
        val query: Uri = Uri.parse(res)
        val intent=Intent(Intent.ACTION_VIEW, query)
        startActivity(intent)
    }
    private fun navigateToHome(){
        val action=ScannerFragmentDirections.actionScannerFragmentToHomeFragment()
        findNavController().navigate(action)
    }
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun decodeQRCode(imageProxy: ImageProxy): String? {
        try {
            val mediaImage = imageProxy.image ?: return null

            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21Buffer = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21Buffer, 0, ySize)
            vBuffer.get(nv21Buffer, ySize, vSize)
            uBuffer.get(nv21Buffer, ySize + vSize, uSize)

            val source = PlanarYUVLuminanceSource(
                nv21Buffer,
                imageProxy.width,
                imageProxy.height,
                0,
                0,
                imageProxy.width,
                imageProxy.height,
                true
            )

            val reader = MultiFormatReader()
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            val qrCodeText = result.text

            // Process the QR code text as needed

            return qrCodeText
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }

        return null
    }

}