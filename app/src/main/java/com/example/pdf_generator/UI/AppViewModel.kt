package com.example.pdf_generator.UI

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AppViewModel(): ViewModel(){
     var listOfBitmaps: MutableLiveData<ArrayList<Bitmap>?> = MutableLiveData()

    init{
        listOfBitmaps.postValue(arrayListOf())
    }
    fun setList(list: ArrayList<Bitmap>){
        listOfBitmaps.value=list
    }

    fun getListSize(): Int{
        return listOfBitmaps.value!!.size
    }

    fun deleteElementAtPos(index: Int){
        val list=listOfBitmaps.value
        if(!list.isNullOrEmpty() && index>=0 && index<list.size){
            list.removeAt(index)
            listOfBitmaps.postValue(list)
        }
    }

    fun addElementToList(bitmap: Bitmap){
        var list=listOfBitmaps.value
        if(list!=null) list.add(bitmap)
        else{
            list= arrayListOf()
            list.add(bitmap)
        }
        setList(ArrayList(list))

    }

    fun clearList(){
        setList(arrayListOf())
    }


}