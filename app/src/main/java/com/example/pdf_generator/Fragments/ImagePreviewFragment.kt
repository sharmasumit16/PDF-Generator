package com.example.pdf_generator.Fragments

import android.Manifest
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pdf_generator.Activities.MainActivity
import com.example.pdf_generator.Listener.ItemClickListner
import com.example.pdf_generator.R
import com.example.pdf_generator.UI.AppViewModel
import com.example.pdf_generator.adapters.ClickedImagePreviewAdapter
import com.example.pdf_generator.databinding.FragmentImagePreviewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class ImagePreviewFragment : Fragment(), ItemClickListner {


    private var _binding: FragmentImagePreviewBinding? = null
    private val binding get() = _binding!!
    private val REQUEST_CODE = 20
    private lateinit var adapter: ClickedImagePreviewAdapter
    private lateinit var viewModel: AppViewModel
    private lateinit var listener: ItemClickListner
    private lateinit var temp:String
    private lateinit var mProgressDialog:ProgressDialog


    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        val REQUIRED_PERMISSIONS =
            mutableListOf(
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
        listener=this
        viewModel=(activity as MainActivity).viewModel
        _binding = FragmentImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRecyclerView()
        updateRecyclerView()
        binding.openDialogBoxForName.setOnClickListener {
            buildDialogBox()
        }

    }
    private fun updateRecyclerView(){
        adapter.differ.submitList(viewModel.listOfBitmaps.value)
    }

    private fun setUpRecyclerView() {
        adapter= ClickedImagePreviewAdapter(listener)
        binding.RVImageGrid.adapter=adapter
        binding.RVImageGrid.layoutManager=GridLayoutManager(requireContext(), 3)
    }


    var isDialogBoxShowing = true
    private fun buildDialogBox() {
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialoglayout, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.et_pdfname)
        Log.w("check", "creating dialogBox")
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogLayout)
            .setNeutralButton("Cancel") { _, which ->
            }
            .setPositiveButton("Create") { _, which ->
            }
        val dialog = builder.show()
        dialog.getButton(-1).setOnClickListener {
           temp = editText.text.toString()
            if (temp.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Pdf Name!", Toast.LENGTH_LONG).show()

            } else {
                dialog.dismiss()
                progressBarDialog()
                isDialogBoxShowing = false
            }
        }
        dialog.getButton(-2).setOnClickListener {

            dialog.dismiss()
            isDialogBoxShowing = false
        }



    }
    var isprogressBarDialogShowing=false
    private fun progressBarDialog() {
      mProgressDialog = ProgressDialog(requireContext(),R.style.AppCompatDialogBox)
        mProgressDialog.setTitle("Please wait...")
        mProgressDialog.setMessage("Preparing")
        isprogressBarDialogShowing=true
        mProgressDialog.show()
        createPdf(viewModel.listOfBitmaps.value!!, temp)
        mProgressDialog.dismiss()

    }

    private fun openDoc(pdfFile: File) {

        val authority = "com.example.pdf_generator.fileprovider"

        val pdfUri = FileProvider.getUriForFile(requireContext(), authority, pdfFile)

        val pdfIntent = Intent(Intent.ACTION_VIEW)
        pdfIntent.setDataAndType(pdfUri, "application/pdf")
        pdfIntent.flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

        try {
            pdfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(pdfIntent)
            val action = ImagePreviewFragmentDirections.actionImagePreviewFragmentToHomeFragment()
            findNavController().navigate(action)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No Pdf Viewer Installed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun scaleBitmapToFitScreenWidth(bitmap: Bitmap, screenWidth: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val scaledHeight = (screenWidth.toFloat() / bitmapWidth * bitmapHeight).toInt()
        val processedBitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, scaledHeight, true)
        val stream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.WEBP, 50 ,stream)
        val  byteArray=stream.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun createPdf(bitmaps: ArrayList<Bitmap>, pdfFileName: String) {
        if(bitmaps.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Click some images to proceed", Toast.LENGTH_SHORT).show()
            return
        }
        val pdfDocument = PdfDocument()

        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        for (bitmap in bitmaps) {
            val scaledBitmap = scaleBitmapToFitScreenWidth(bitmap, screenWidth)
            val pageInfo = PdfDocument.PageInfo.Builder(scaledBitmap.width, scaledBitmap.height, pdfDocument.pages.size + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
        }

        val documentsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val directoryName = "pdfGeneratorDocuments"
        val directory=File(documentsDirectory, directoryName)
        if (!directory.exists()) {directory.mkdirs()}

        val pdfFilePath = "${directory.path}/$pdfFileName.pdf"
        val pdfFile = File(pdfFilePath)

        try {
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()
            Toast.makeText(requireContext(), "${pdfFileName}.pdf saved successfully", Toast.LENGTH_SHORT).show()
            openDoc(pdfFile)
            if(isprogressBarDialogShowing){
                mProgressDialog.dismiss()
                isprogressBarDialogShowing=false
            }
            viewModel.clearList()

        } catch (e: IOException) {
            Log.w(ContentValues.TAG, "Error while creating Pdf: ${e}")
            Toast.makeText(requireContext(), "Failed to create PDF", Toast.LENGTH_SHORT).show()
        }


    }
    override fun onItemClick(view: View, imgUri: Uri, position: Int) {

    }

    override fun onDeleteBtnClick(view: View, position: Int) {
        viewModel.deleteElementAtPos(position)
       adapter.notifyItemRemoved(position)
        updateRecyclerView()
    }

    override fun openPDF() {
        TODO("Not yet implemented")
    }

}