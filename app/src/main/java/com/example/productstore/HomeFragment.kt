package com.example.productstore

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var categorySpinner: Spinner
    private lateinit var productNameSpinner: Spinner
    private lateinit var totalTextView: TextView    // Отображает "Итого: 0"
    private lateinit var quantityEditText: EditText
    private lateinit var addButton: Button

    private val categoriesList = mutableListOf<String>()
    private val productsList = mutableListOf<Product>()

    private lateinit var categorySpinnerAdapter: ArrayAdapter<String>
    private lateinit var productSpinnerAdapter: ArrayAdapter<String>

    private var selectedProduct: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        categorySpinner = view.findViewById(R.id.categorySpinner)
        productNameSpinner = view.findViewById(R.id.productNameSpinner)
        totalTextView = view.findViewById(R.id.totalAmountTextView)   // Отображает "Итого: 0"
        quantityEditText = view.findViewById(R.id.quantityEditText)
        addButton = view.findViewById(R.id.addButton)

        // Настройка адаптеров для спиннеров
        categorySpinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriesList)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categorySpinnerAdapter

        productSpinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        productSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        productNameSpinner.adapter = productSpinnerAdapter

        // Загрузка категорий с сервера
        loadCategories()

        // Установка слушателей
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedCategory = categoriesList[position]
                loadProducts(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        productNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedProduct = productsList[position]
                updateTotal()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateTotal()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        addButton.setOnClickListener {
            addEvent()
        }

        return view
    }

    private fun loadCategories() {
        val call = ApiClient.apiService.getCategories()
        call.enqueue(object : Callback<CategoriesResponse> {
            override fun onResponse(
                call: Call<CategoriesResponse>,
                response: Response<CategoriesResponse>
            ) {
                if (response.isSuccessful) {
                    val categoriesResponse = response.body()
                    categoriesList.clear()
                    if (categoriesResponse != null) {
                        categoriesList.addAll(categoriesResponse.categories)
                    }
                    categorySpinnerAdapter.notifyDataSetChanged()
                    // Загрузка продуктов для первой категории, если необходимо
                    if (categoriesList.isNotEmpty()) {
                        val selectedCategory = categoriesList[0]
                        categorySpinner.setSelection(0)
                        loadProducts(selectedCategory)
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

    private fun loadProducts(category: String) {
        val call = ApiClient.apiService.getProducts()   // Изменено: вызываем getProducts() без аргументов

        call.enqueue(object : Callback<ProductsResponse> {
            override fun onResponse(
                call: Call<ProductsResponse>,
                response: Response<ProductsResponse>
            ) {
                if (response.isSuccessful) {
                    val productsResponse = response.body()
                    productsList.clear()
                    if (productsResponse != null) {
                        // Фильтрация продуктов по выбранной категории
                        val filteredProducts = productsResponse.products.filter { it.category == category }
                        productsList.addAll(filteredProducts)
                    }

                    // Обновление адаптера продуктов
                    val productNames = productsList.map { it.name }
                    productSpinnerAdapter.clear()
                    productSpinnerAdapter.addAll(productNames)
                    productSpinnerAdapter.notifyDataSetChanged()

                    if (productsList.isNotEmpty()) {
                        productNameSpinner.setSelection(0)
                        selectedProduct = productsList[0]
                        updateTotal()
                    } else {
                        selectedProduct = null
                        totalTextView.text = "Итого: 0"
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке товаров", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ProductsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateTotal() {
        val quantityText = quantityEditText.text.toString()
        val quantity = quantityText.toIntOrNull() ?: 1

        if (selectedProduct != null) {
            val total = selectedProduct!!.salePrice * quantity
            totalTextView.text = "Итого: $total"
        } else {
            totalTextView.text = "Итого: 0"
        }
    }

    private fun addEvent() {
        val selectedCategory = categorySpinner.selectedItem?.toString()
        val selectedProductName = productNameSpinner.selectedItem?.toString()
        val quantityText = quantityEditText.text.toString()
        val quantity = quantityText.toIntOrNull() ?: 1

        if (selectedCategory == null || selectedProductName == null) {
            Toast.makeText(requireContext(), "Выберите категорию и товар", Toast.LENGTH_SHORT).show()
            return
        }

        val call = ApiClient.apiService.addEvent(
            category = selectedCategory,
            product = selectedProductName,
            quantity = quantity
        )

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Продажа успешно добавлена", Toast.LENGTH_SHORT).show()
                    // Очистка поля количества после добавления
                    quantityEditText.text.clear()
                    updateTotal()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при добавлении продажи", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }
}