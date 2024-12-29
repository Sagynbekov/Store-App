package com.example.productstore

import android.app.Activity
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private lateinit var categorySpinner: Spinner
    private lateinit var dateRangeButton: Button
    private lateinit var reportTable: TableLayout

    private val categoriesList = mutableListOf<String>()
    private lateinit var categorySpinnerAdapter: ArrayAdapter<String>

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null

    private lateinit var totalQuantityTextView: TextView
    private lateinit var totalPurchaseTextView: TextView
    private lateinit var totalSaleTextView: TextView
    private lateinit var totalRevenueTextView: TextView

    private val CREATE_FILE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        categorySpinner = view.findViewById(R.id.categorySpinner)
        dateRangeButton = view.findViewById(R.id.dateRangeButton)
        reportTable = view.findViewById(R.id.reportTable)

        // Инициализируем текстовые поля для итоговых значений
        totalQuantityTextView = view.findViewById(R.id.totalQuantity)
        totalPurchaseTextView = view.findViewById(R.id.totalPurchase)
        totalSaleTextView = view.findViewById(R.id.totalSale)
        totalRevenueTextView = view.findViewById(R.id.totalRevenue)

        // Настройка адаптера для спиннера категорий
        categorySpinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriesList)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categorySpinnerAdapter

        // Загрузка категорий с сервера
        loadCategories()

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                loadEvents()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        dateRangeButton.setOnClickListener {
            openDateRangePicker()
        }

        val exportButton: Button = view.findViewById(R.id.exportButton)

        exportButton.setOnClickListener {
            exportTableToPDF()
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
                    categoriesList.add("Все категории") // Добавляем опцию для всех категорий
                    if (categoriesResponse != null) {
                        categoriesList.addAll(categoriesResponse.categories)
                    }
                    categorySpinnerAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке категорий", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CategoriesResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun openDateRangePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()
        picker.show(parentFragmentManager, picker.toString())
        picker.addOnPositiveButtonClickListener { dateSelected ->
            val startDateMillis = dateSelected.first ?: return@addOnPositiveButtonClickListener
            val endDateMillis = dateSelected.second ?: return@addOnPositiveButtonClickListener

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedStartDate = dateFormat.format(Date(startDateMillis))
            selectedEndDate = dateFormat.format(Date(endDateMillis))

            // Обновляем текст кнопки
            dateRangeButton.text = "$selectedStartDate - $selectedEndDate"

            // Загружаем события с новыми датами
            loadEvents()
        }
    }

    private fun loadEvents() {
        val selectedCategory = if (categorySpinner.selectedItemPosition == 0) null else categorySpinner.selectedItem.toString()

        // Вызываем getEvents() с параметрами
        val call = ApiClient.apiService.getEvents(
            startDate = selectedStartDate,
            endDate = selectedEndDate,
            category = selectedCategory
        )

        call.enqueue(object : Callback<EventsResponse> {
            override fun onResponse(call: Call<EventsResponse>, response: Response<EventsResponse>) {
                if (response.isSuccessful) {
                    val eventsResponse = response.body()
                    val eventsList = eventsResponse?.events ?: emptyList()

                    updateReportTable(eventsList)
                } else {
                    Toast.makeText(requireContext(), "Ошибка при загрузке отчетов", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<EventsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Ошибка подключения к серверу", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateReportTable(events: List<Event>) {
        // Очистить таблицу, кроме заголовка (позиция 0)
        if (reportTable.childCount > 2) {
            reportTable.removeViews(2, reportTable.childCount - 2)
        }

        var totalQuantity = 0
        var totalPurchase = 0.0
        var totalSale = 0.0
        var totalRevenue = 0.0

        for (event in events) {
            val tableRow = TableRow(requireContext())

            val productNameTextView = TextView(requireContext())
            productNameTextView.text = event.product
            productNameTextView.setPadding(8, 8, 8, 8)
            tableRow.addView(productNameTextView)

            val quantityTextView = TextView(requireContext())
            quantityTextView.text = event.quantity.toString()
            quantityTextView.gravity = Gravity.CENTER
            quantityTextView.setPadding(8, 8, 8, 8)
            tableRow.addView(quantityTextView)

            val purchasePriceTextView = TextView(requireContext())
            purchasePriceTextView.text = String.format("%.2f", event.purchase_price)
            purchasePriceTextView.gravity = Gravity.CENTER
            purchasePriceTextView.setPadding(8, 8, 8, 8)
            tableRow.addView(purchasePriceTextView)

            val salePriceTextView = TextView(requireContext())
            salePriceTextView.text = String.format("%.2f", event.sale_price)
            salePriceTextView.gravity = Gravity.CENTER
            salePriceTextView.setPadding(8, 8, 8, 8)
            tableRow.addView(salePriceTextView)

            val revenueTextView = TextView(requireContext())
            revenueTextView.text = String.format("%.2f", event.revenue)
            revenueTextView.gravity = Gravity.CENTER
            revenueTextView.setPadding(8, 8, 8, 8)
            tableRow.addView(revenueTextView)

            reportTable.addView(tableRow)

            // Обновляем суммы
            totalQuantity += event.quantity
            totalPurchase += event.purchase_price * event.quantity
            totalSale += event.sale_price * event.quantity
            totalRevenue += event.revenue
        }

        // Обновляем итоговую строку
        totalQuantityTextView.text = totalQuantity.toString()
        totalPurchaseTextView.text = String.format("%.2f", totalPurchase)
        totalSaleTextView.text = String.format("%.2f", totalSale)
        totalRevenueTextView.text = String.format("%.2f", totalRevenue)
    }

    private fun exportTableToPDF() {
        // Создаем Intent для сохранения файла
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "Report_${System.currentTimeMillis()}.pdf")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                // Создаем PDF-документ
                val pdfDocument = createPdfDocument()

                // Записываем PDF в выбранный пользователем путь
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                        Toast.makeText(requireContext(), "Отчет сохранен", Toast.LENGTH_LONG).show()
                    } ?: throw IOException("Не удалось открыть поток для записи.")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    pdfDocument.close()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createPdfDocument(): PdfDocument {
        val pdfDocument = PdfDocument()

        // Размер страницы A4 в пунктах (1 пункт = 1/72 дюйма)
        val pageWidth = 595
        val pageHeight = 842

        // Подготавливаем макет для отрисовки
        reportTable.measure(
            View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        reportTable.layout(0, 0, pageWidth, reportTable.measuredHeight)

        val totalHeight = reportTable.measuredHeight
        var currentPage = 1
        var yOffset = 0

        while (yOffset < totalHeight) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas

            // Сохраняем состояние канвы
            canvas.save()

            // Смещаем канву по вертикали, чтобы отрисовать нужную часть
            canvas.translate(0f, (-yOffset).toFloat())

            // Рисуем содержимое
            reportTable.draw(canvas)

            // Восстанавливаем состояние канвы
            canvas.restore()

            // Завершаем страницу
            pdfDocument.finishPage(page)

            yOffset += pageHeight
            currentPage++
        }

        return pdfDocument
    }
}