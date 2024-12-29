package com.example.productstore

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class CategorySpinnerAdapter(
    context: Context,
    private val categories: MutableList<String>,
    private val editCallback: (String) -> Unit,
    private val deleteCallback: (String) -> Unit
) : ArrayAdapter<String>(context, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, isDropdown = false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent, isDropdown = true)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup, isDropdown: Boolean): View {
        val category = categories[position]

        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.spinner_item_with_delete,
            parent,
            false
        )

        val categoryNameTextView = view.findViewById<TextView>(R.id.categoryNameTextView)
        val editImageView = view.findViewById<ImageView>(R.id.editCategoryImageView)
        val deleteImageView = view.findViewById<ImageView>(R.id.deleteCategoryImageView)

        categoryNameTextView.text = category

        if (!isDropdown) {
            // В главном виде скрываем иконки редактирования и удаления
            editImageView.visibility = View.GONE
            deleteImageView.visibility = View.GONE
        } else {
            // В выпадающем списке показываем иконки и задаем обработчики клика
            editImageView.visibility = View.VISIBLE
            deleteImageView.visibility = View.VISIBLE

            editImageView.setOnClickListener {
                editCallback.invoke(category)
            }

            deleteImageView.setOnClickListener {
                deleteCallback.invoke(category)
            }
        }

        return view
    }
}