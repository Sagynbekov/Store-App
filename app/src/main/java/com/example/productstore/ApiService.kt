package com.example.productstore

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("add_category")
    fun addCategory(@Field("name") categoryName: String): Call<Void>

    @GET("get_categories")
    fun getCategories(): Call<CategoriesResponse>

    @FormUrlEncoded
    @POST("/add_product")
    fun addProduct(
        @Field("category") category: String,
        @Field("name") name: String,
        @Field("purchase_price") purchasePrice: Double,
        @Field("sale_price") salePrice: Double,
        @Field("quantity") quantity: Int
    ): Call<Void>

    @GET("get_products")
    fun getProducts(): Call<ProductsResponse>

    @DELETE("delete_category")
    fun deleteCategory(@Query("name") categoryName: String): Call<Void>

    @DELETE("/delete_product")
    fun deleteProduct(
        @Query("name") productName: String,
        @Query("category") categoryName: String
    ): Call<Void>


    @FormUrlEncoded
    @PUT("update_product")
    fun updateProduct(
        @Field("old_name") oldName: String,
        @Field("old_category") oldCategory: String,
        @Field("category") category: String,
        @Field("name") name: String,
        @Field("purchase_price") purchasePrice: Double,
        @Field("sale_price") salePrice: Double,
        @Field("quantity") quantity: Int
    ): Call<Void>

    @FormUrlEncoded
    @PUT("update_category")
    fun updateCategory(
        @Field("old_name") oldName: String,
        @Field("new_name") newName: String
    ): Call<Void>


    @FormUrlEncoded
    @POST("add_event")
    fun addEvent(
        @Field("category") category: String,
        @Field("product") product: String,
        @Field("quantity") quantity: Int
    ): Call<Void>

    @GET("get_events")
    fun getEvents(
        @Query("category") category: String?,
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): Call<EventsResponse>
}