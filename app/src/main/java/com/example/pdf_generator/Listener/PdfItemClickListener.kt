package com.example.pdf_generator.Listener

import java.io.File

interface PdfItemClickListener {
    fun pdfItemClicked(file: File)
    fun onPopupMenuBtnClicked(position:Int)
}