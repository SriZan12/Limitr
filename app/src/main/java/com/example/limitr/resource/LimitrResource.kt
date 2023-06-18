package com.example.limitr.resource

sealed class LimitrResource {
    data class Loading<T>(val progress: T) : LimitrResource()
    data class Success<T>(val result: T) : LimitrResource()
    data class Error(val error: Exception) : LimitrResource()
}