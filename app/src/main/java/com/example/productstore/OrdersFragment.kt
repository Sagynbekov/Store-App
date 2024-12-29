package com.example.productstore

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrdersFragment : Fragment() {

    private lateinit var addCategoryButton: Button
    private lateinit var addProductButton: Button
    private lateinit var categorySpinner: Spinner
    private val categoriesList = mutableListOf<String>()

    // Объявляем кастомный адаптер
    private lateinit var categorySpinnerAdapter: CategorySpinnerAdapter

    private lateinit var productTable: TableLayout
    private val productsList = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        // Инициализация кнопок и спиннера
        addCategoryButton = view.findViewById(R.id.addCategoryButton)
        addProductButton = view.findViewById(R.id.addProductButton)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        productTable = view.findViewById(R.id.productTable)

        // Настройка кастомного адаптера для спиннера
        categorySpinnerAdapter = CategorySpinnerAdapter(requireContext(), categoriesList,
            editCallback = { category ->
                showEditCategoryDialog(category)
            },
            deleteCallback = { category ->
                showDeleteCategoryConfirmationDialog(category)
            }
        )
        categorySpinner.adapter = categorySpinnerAdapter

        // Обработчик нажатия на кнопку "Добавить категорию"
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        // Обработчик нажатия на кнопку "Добавить продукт"
        addProductButton.setOnClickListener {
            showAddProductDialog()
        }

        // Загрузка категорий и продуктов при запуске фрагмента
        loadCategories()

        // Обработчик изменения выбранной категории
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadProducts()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val addCategoryConfirmButton = dialogView.findViewById<Button>(R.id.addCategoryConfirmButton)
        val categoryNameInput = dialogView.findViewById<EditText>(R.id.categoryNameInput)

        // Устанавливаем заголовок диалога
        dialogTitle.text = "Добавить категорию"

        addCategoryConfirmButton.setOnClickListener {
            val categoryName = categoryNameInput.text.toString()
            if (categoryName.isNotEmpty()) {
                addCategory(categoryName) { success ->
                    if (success) {
                        dialog.dismiss()
                        loadCategories()
                        loadProducts()
                    } else {
                        Toast.makeText(requireContext(), "Не удалось добавить категорию", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                categoryNameInput.error = "Пожалуйста, введите название категории"
            }
        }

        dialog.show()
    }

    private fun showEditCategoryDialog(oldCategoryName: String) {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val addCategoryConfirmButton = dialogView.findViewById<Button>(R.id.addCategoryConfirmButton)
        val categoryNameInput = dialogView.findViewById<EditText>(R.id.categoryNameInput)

        // Устанавливаем заголовок диалога и текст кнопки
        dialogTitle.text = "Редактировать категорию"
        addCategoryConfirmButton.text = "Сохранить"

        // Устанавливаем текущее название категории
        categoryNameInput.setText(oldCategoryName)

        addCategoryConfirmButton.setOnClickListener {
            val newCategoryName = categoryNameInput.text.toString()
            if (newCategoryName.isNotEmpty()) {
                updateCategory(oldCategoryName, newCategoryName) { success ->
                    if (success) {
                        dialog.dismiss()
                        loadCategories()
                        loadProducts()
                    } else {
                        Toast.makeText(requireContext(), "Не удалось обновить категорию", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                categoryNameInput.error = "Пожалуйста, введите название категории"
            }
        }

        dialog.show()
    }

    // Метод для показа модального окна подтверждения удаления категории
    private fun showDeleteCategoryConfirmationDialog(categoryName: String) {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_confirm_delete, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val confirmDeleteButton = dialogView.findViewById<Button>(R.id.confirmDeleteButton)
        val cancelDeleteButton = dialogView.findViewById<Button>(R.id.cancelDeleteButton)

        // Устанавливаем текст сообщения с названием категории
        dialogTitle.text = "Удаление категории"
        dialogMessage.text = "Вы уверены, что хотите удалить категорию \"$categoryName\"?"

        confirmDeleteButton.setOnClickListener {
            deleteCategory(categoryName) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Категория \"$categoryName\" удалена", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadCategories()
                    loadProducts()
                } else {
                    dialog.dismiss()
                }
            }
        }

        cancelDeleteButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addCategory(categoryName: String, callback: (Boolean) -> Unit) {
        val call = ApiClient.apiService.addCategory(categoryName)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 409) {
                        Toast.makeText(requireContext(), "Такая категория уже существует", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при добавлении категории", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun updateCategory(oldCategoryName: String, newCategoryName: String, callback: (Boolean) -> Unit) {
        val call = ApiClient.apiService.updateCategory(oldCategoryName, newCategoryName)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 409) {
                        Toast.makeText(requireContext(), "Категория с таким названием уже существует", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при обновлении категории", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun deleteCategory(categoryName: String, callback: (Boolean) -> Unit) {
        val call = ApiClient.apiService.deleteCategory(categoryName)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 404) {
                        Toast.makeText(requireContext(), "Категория не найдена", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при удалении категории", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun showAddProductDialog() {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_add_product, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Инициализация элементов диалога
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val categorySpinnerDialog = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val productNameInput = dialogView.findViewById<EditText>(R.id.productNameInput)
        val purchasePriceInput = dialogView.findViewById<EditText>(R.id.purchasePriceInput)
        val salePriceInput = dialogView.findViewById<EditText>(R.id.salePriceInput)
        val quantityInput = dialogView.findViewById<EditText>(R.id.quantityInput)
        val addProductConfirmButton = dialogView.findViewById<Button>(R.id.addProductConfirmButton)

        // Устанавливаем заголовок диалога
        dialogTitle.text = "Добавить продукт"

        // Настройка адаптера для спиннера категорий
        val spinnerAdapterDialog = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriesList)
        spinnerAdapterDialog.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinnerDialog.adapter = spinnerAdapterDialog

        // Обработчик нажатия на кнопку "Добавить"
        addProductConfirmButton.setOnClickListener {
            val category = categorySpinnerDialog.selectedItem?.toString()
            val name = productNameInput.text.toString()
            val purchasePrice = purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
            val salePrice = salePriceInput.text.toString().toDoubleOrNull() ?: 0.0
            val quantity = quantityInput.text.toString().toIntOrNull() ?: 0

            if (category.isNullOrEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addProduct(category, name, purchasePrice, salePrice, quantity) { success ->
                if (success) {
                    dialog.dismiss()
                    loadProducts()
                } else {
                    Toast.makeText(requireContext(), "Не удалось добавить продукт", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun addProduct(
        category: String,
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        quantity: Int,
        callback: (Boolean) -> Unit
    ) {
        val call = ApiClient.apiService.addProduct(category, name, purchasePrice, salePrice, quantity)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 409) {
                        Toast.makeText(requireContext(), "Такой продукт уже существует в этой категории", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при добавлении продукта", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    private fun loadCategories() {
        val call = ApiClient.apiService.getCategories()
        call.enqueue(object : Callback<CategoriesResponse> {
            override fun onResponse(call: Call<CategoriesResponse>, response: Response<CategoriesResponse>) {
                if (response.isSuccessful) {
                    val categoriesResponse = response.body()
                    categoriesList.clear()
                    if (categoriesResponse != null) {
                        categoriesList.addAll(categoriesResponse.categories)
                    }
                    // Уведомляем адаптер о изменениях
                    categorySpinnerAdapter.notifyDataSetChanged()
                    // Если категория была удалена, устанавливаем первую категорию в качестве выбранной
                    if (categoriesList.isNotEmpty()) {
                        categorySpinner.setSelection(0)
                    } else {
                        productsList.clear()
                        updateProductTable()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке категорий", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CategoriesResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadProducts() {
        val selectedCategory = categorySpinner.selectedItem?.toString()
        if (selectedCategory == null) {
            productsList.clear()
            updateProductTable()
            return
        }
        val call = ApiClient.apiService.getProducts()
        call.enqueue(object : Callback<ProductsResponse> {
            override fun onResponse(call: Call<ProductsResponse>, response: Response<ProductsResponse>) {
                if (response.isSuccessful) {
                    val productsResponse = response.body()
                    productsList.clear()
                    if (productsResponse != null) {
                        // Фильтрация продуктов по выбранной категории
                        productsList.addAll(productsResponse.products.filter { it.category == selectedCategory })
                    }
                    updateProductTable()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке продуктов", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProductsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateProductTable() {
        // Очищаем таблицу, оставляя заголовок
        if (productTable.childCount > 1) {
            productTable.removeViews(1, productTable.childCount - 1)
        }

        for (product in productsList) {
            val tableRow = TableRow(requireContext())

            val nameTextView = TextView(requireContext())
            nameTextView.text = product.name
            nameTextView.setPadding(8, 8, 8, 8)

            val quantityTextView = TextView(requireContext())
            quantityTextView.text = product.quantity.toString()
            quantityTextView.gravity = Gravity.CENTER
            quantityTextView.setPadding(8, 8, 8, 8)

            val purchasePriceTextView = TextView(requireContext())
            purchasePriceTextView.text = product.purchasePrice.toString()
            purchasePriceTextView.gravity = Gravity.CENTER
            purchasePriceTextView.setPadding(8, 8, 8, 8)

            val salePriceTextView = TextView(requireContext())
            salePriceTextView.text = product.salePrice.toString()
            salePriceTextView.gravity = Gravity.CENTER
            salePriceTextView.setPadding(8, 8, 8, 8)

            // Кнопка редактирования
            val editButton = ImageButton(requireContext())
            editButton.setImageResource(R.drawable.ic_edit) // Добавьте иконку редактирования (ручка)
            editButton.setBackgroundColor(Color.TRANSPARENT)
            editButton.setOnClickListener {
                showEditProductDialog(product)
            }
            editButton.setPadding(8, 8, 8, 8)

            // Кнопка удаления
            val deleteButton = ImageButton(requireContext())
            deleteButton.setImageResource(R.drawable.ic_delete)
            deleteButton.setBackgroundColor(Color.TRANSPARENT)
            deleteButton.setOnClickListener {
                showDeleteProductConfirmationDialog(product)
            }
            deleteButton.setPadding(8, 8, 8, 8)

            // Добавляем кнопки в LinearLayout
            val buttonLayout = LinearLayout(requireContext())
            buttonLayout.orientation = LinearLayout.HORIZONTAL
            buttonLayout.addView(editButton)
            buttonLayout.addView(deleteButton)

            // Добавляем элементы в строку таблицы
            tableRow.addView(nameTextView)
            tableRow.addView(quantityTextView)
            tableRow.addView(purchasePriceTextView)
            tableRow.addView(salePriceTextView)
            tableRow.addView(buttonLayout)

            productTable.addView(tableRow)
        }
    }

    // Метод для показа диалога редактирования продукта
    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_add_product, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Инициализация элементов диалога
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val categorySpinnerDialog = dialogView.findViewById<Spinner>(R.id.categorySpinner)
        val productNameInput = dialogView.findViewById<EditText>(R.id.productNameInput)
        val purchasePriceInput = dialogView.findViewById<EditText>(R.id.purchasePriceInput)
        val salePriceInput = dialogView.findViewById<EditText>(R.id.salePriceInput)
        val quantityInput = dialogView.findViewById<EditText>(R.id.quantityInput)
        val addProductConfirmButton = dialogView.findViewById<Button>(R.id.addProductConfirmButton)

        // Изменяем заголовок и текст кнопки
        dialogTitle.text = "Редактировать продукт"
        addProductConfirmButton.text = "Сохранить"

        // Настройка адаптера для спиннера категорий
        val spinnerAdapterDialog = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriesList)
        spinnerAdapterDialog.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinnerDialog.adapter = spinnerAdapterDialog

        // Устанавливаем текущие значения продукта
        productNameInput.setText(product.name)
        purchasePriceInput.setText(product.purchasePrice.toString())
        salePriceInput.setText(product.salePrice.toString())
        quantityInput.setText(product.quantity.toString())

        // Устанавливаем выбранную категорию
        val categoryPosition = categoriesList.indexOf(product.category)
        if (categoryPosition >= 0) {
            categorySpinnerDialog.setSelection(categoryPosition)
        }

        // Обработчик нажатия на кнопку "Сохранить"
        addProductConfirmButton.setOnClickListener {
            val category = categorySpinnerDialog.selectedItem?.toString()
            val name = productNameInput.text.toString()
            val purchasePrice = purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
            val salePrice = salePriceInput.text.toString().toDoubleOrNull() ?: 0.0
            val quantity = quantityInput.text.toString().toIntOrNull() ?: 0

            if (category.isNullOrEmpty() || name.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProduct(
                oldName = product.name,
                oldCategory = product.category,
                category = category,
                name = name,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                quantity = quantity
            ) { success ->
                if (success) {
                    dialog.dismiss()
                    loadProducts()
                } else {
                    Toast.makeText(requireContext(), "Не удалось обновить продукт", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // Метод для обновления продукта
    private fun updateProduct(
        oldName: String,
        oldCategory: String,
        category: String,
        name: String,
        purchasePrice: Double,
        salePrice: Double,
        quantity: Int,
        callback: (Boolean) -> Unit
    ) {
        val call = ApiClient.apiService.updateProduct(
            oldName = oldName,
            oldCategory = oldCategory,
            category = category,
            name = name,
            purchasePrice = purchasePrice,
            salePrice = salePrice,
            quantity = quantity
        )
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 409) {
                        Toast.makeText(requireContext(), "Продукт с таким названием уже существует в этой категории", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при обновлении продукта", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }

    // Метод для показа модального окна подтверждения удаления продукта
    private fun showDeleteProductConfirmationDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())

        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_confirm_delete, null)

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val confirmDeleteButton = dialogView.findViewById<Button>(R.id.confirmDeleteButton)
        val cancelDeleteButton = dialogView.findViewById<Button>(R.id.cancelDeleteButton)

        // Устанавливаем заголовок и сообщение
        dialogTitle.text = "Удаление продукта"
        dialogMessage.text = "Вы уверены, что хотите удалить продукт \"${product.name}\"?"

        confirmDeleteButton.setOnClickListener {
            deleteProduct(product) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Продукт \"${product.name}\" удален", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadProducts()
                } else {
                    dialog.dismiss()
                }
            }
        }

        cancelDeleteButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Метод для удаления продукта
    private fun deleteProduct(product: Product, callback: (Boolean) -> Unit) {
        val call = ApiClient.apiService.deleteProduct(product.name, product.category)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)
                } else {
                    if (response.code() == 404) {
                        Toast.makeText(requireContext(), "Продукт не найден", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при удалении продукта", Toast.LENGTH_SHORT).show()
                    }
                    callback(false)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        })
    }
}