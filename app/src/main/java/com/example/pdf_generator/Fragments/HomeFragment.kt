package com.example.pdf_generator.Fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback

import androidx.activity.result.ActivityResultLauncher

import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdf_generator.adapters.PdfAdapter
import com.example.pdf_generator.Activities.CaptureActiv
import com.example.pdf_generator.Activities.MainActivity
import com.example.pdf_generator.Listener.PdfItemClickListener
import com.example.pdf_generator.R
import com.example.pdf_generator.UI.AppViewModel
import com.example.pdf_generator.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import java.io.File



class HomeFragment : Fragment(), PdfItemClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AppViewModel
    private lateinit var imageuriList: ArrayList<Uri>
    private lateinit var imageBitmapList: ArrayList<Bitmap>
    private lateinit var listener: PdfItemClickListener
    private lateinit var adapter: PdfAdapter
    private var isSearchBarVisible: Boolean =false
    private var pdfFiles: MutableList<File>?=null
//    private var arrray:Array<Int>?=null


    var permissionArray = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listener = this
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = (activity as MainActivity).viewModel
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermissions()
            getFiles()
            imageuriList = ArrayList()
            imageBitmapList = ArrayList()
            //Permission checking...
            checkPermissions()
        binding.searchIv.setOnClickListener {
            search()

        }

            binding.scannerBtn
                .setOnClickListener {
                    scanQR()
                }

            binding.createPdfFab.setOnClickListener {
                val action = HomeFragmentDirections.actionHomeFragmentToCameraFragment()
                findNavController().navigate(action)
            }

            binding.createPdfSelectImageFab.setOnClickListener {
                openGallery()
            }
        }

        private fun search(){
            if(isSearchBarVisible) {
                isSearchBarVisible=false
                binding.searchBar.visibility = View.GONE
                setUpRecyclerView(pdfFiles)
                return
            }
            isSearchBarVisible=true
            binding.searchBar.visibility=View.VISIBLE
            binding.searchBarEditText.addTextChangedListener(object: TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val files=pdfFiles
                    setUpRecyclerView(files?.filter{ it.name.startsWith(s.toString(), ignoreCase = true)} as MutableList<File>?)
                }

                override fun afterTextChanged(s: Editable?) {

                }

            })
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity()) {
            if(isSearchBarVisible){
                isSearchBarVisible=false
                binding.searchBar.visibility=View.GONE
                setUpRecyclerView(pdfFiles)
            }
            else{
                requireActivity().finish()
            }
        }
    }


    private fun scanQR() {
            val options: ScanOptions = ScanOptions()
            options.setBeepEnabled(true)
            options.setOrientationLocked(true)
            options.setPrompt("Use Volume Up Button to turn on the Flash Light")
            options.setCaptureActivity(
                CaptureActiv::class.java
            )
            barlauncher.launch(options)
        }

        val barlauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
            ScanContract()
        ) {
            if (it.contents != null) {
                buildDialogBox(it.contents)
            }
        }
        var isDialogBoxShowing = true
        private fun buildDialogBox(res: String) {

            Log.w("check", "creating dialogBox")
            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Link Found")
                .setMessage(res)
                .setNeutralButton("Close") { _, which ->
                }
                .setNegativeButton("Scan other Code") { _, which ->
                }
                .setPositiveButton("Open Link") { _, which ->
                }
            val dialog = builder.show()
            dialog.getButton(-1).setOnClickListener {

                dialog.dismiss()
                isDialogBoxShowing = false
                openInWeb(res)
            }
            dialog.getButton(-2).setOnClickListener {

                dialog.dismiss()
                isDialogBoxShowing = false
            }
            dialog.getButton(-3).setOnClickListener {
                dialog.dismiss()
                isDialogBoxShowing = false

            }


        }

        private fun openInWeb(res: String) {
            val query: Uri = Uri.parse(res)
            val intent = Intent(Intent.ACTION_VIEW, query)
            startActivity(intent)
        }

        private fun getFiles() {
            val documentsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val folderName = "pdfGeneratorDocuments"

            val folder = File(documentsDirectory, folderName)
            if (folder.exists() && folder.isDirectory) {

               pdfFiles = folder.listFiles { file ->
                    file.extension.equals("pdf", ignoreCase = true)
                }?.toMutableList()

                setUpRecyclerView(pdfFiles)
            } else Toast.makeText(requireContext(), "Folder Doesn't Exists", Toast.LENGTH_SHORT)
                .show()

        }

        private fun setUpRecyclerView(files: MutableList<File>?) {
            adapter = PdfAdapter(files, listener)
            binding.savedPdfRcv.adapter = adapter
            binding.savedPdfRcv.layoutManager = LinearLayoutManager(requireContext())
        }

        private fun openPdf(file: File) {
            val authority = "com.example.pdf_generator.fileprovider"

            val pdfUri = FileProvider.getUriForFile(requireContext(), authority, file)

            val pdfIntent = Intent(Intent.ACTION_VIEW)
            pdfIntent.setDataAndType(pdfUri, "application/pdf")
            pdfIntent.flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

            try {
                startActivity(pdfIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No Pdf Viewer Installed", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        private fun openGallery() {
            val intent = Intent()
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Images"), 123)
        }

        private fun checkPermissions() {
            if ((ActivityCompat.checkSelfPermission(
                    requireContext(), permissionArray[0]
                ) != PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(
                    requireContext(), permissionArray[1]
                ) != PackageManager.PERMISSION_GRANTED)
            ) {
                requestPermissions(permissionArray, 123)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 123 && resultCode == RESULT_OK) {
                if (data != null) {
                    if (data.clipData != null) {
                        val x = data.clipData!!.itemCount
                        for (i in 0 until x)
                            imageuriList.add(data.clipData!!.getItemAt(i).uri)

                        for (i in imageuriList) {
                            getBitmapFromUri(i)?.let { viewModel.addElementToList(it) }
                        }
                        val action =
                            HomeFragmentDirections.actionHomeFragmentToImagePreviewFragment()
                        findNavController().navigate(action)
                    } else if (data.data != null) {
                        val imguri = data.data!!.path
                    }
                }
            }
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

        override fun pdfItemClicked(file: File) {
            openPdf(file)
        }

    override fun onPopupMenuBtnClicked(position: Int) {
        val popupMenu=PopupMenu(requireContext(),binding.savedPdfRcv[position].findViewById(R.id.popupMenuBtn))
        popupMenu.inflate(R.menu.popupmenu)
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.share_btn->{
                    sharePdf(position)
                    return@setOnMenuItemClickListener true
                }
                R.id.delete_btn->{
//                   Toast.makeText(requireContext(),"In Process....",Toast.LENGTH_SHORT).show()
                    deletePdf(position)
                    return@setOnMenuItemClickListener true
                }

            }
            return@setOnMenuItemClickListener false
        }
        popupMenu.show()
    }

    private fun deletePdf(position: Int) {
        val tempFile= pdfFiles?.get(position)
        openDialogforDeleteAlert(tempFile,position)
    }

    private fun openDialogforDeleteAlert(tempFile: File?, position: Int) {


            val builder = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete")
                .setMessage("Are you sure want to delete this item?")
                .setNeutralButton("Cancel") { _, which ->
                }
                .setPositiveButton("Delete") { _, which ->
                }
            val dialog = builder.show()
            dialog.getButton(-1).setOnClickListener {

                dialog.dismiss()
                tempFile?.delete()
                pdfFiles?.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
            dialog.getButton(-2).setOnClickListener {

                dialog.dismiss()

            }

    }

    private fun sharePdf(position: Int) {
        val authority = "com.example.pdf_generator.fileprovider"

        val uri =
            pdfFiles?.get(position)?.let {
                FileProvider.getUriForFile(requireContext(), authority,
                    it
                )
            }

        val intent = Intent(Intent.ACTION_SEND)


        intent.putExtra(Intent.EXTRA_STREAM, uri)


//        intent.putExtra(Intent.EXTRA_TEXT, "Sharing Image")


        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")


        intent.type = "application/pdf"

        startActivity(Intent.createChooser(intent, "Share Via"))
    }
}

