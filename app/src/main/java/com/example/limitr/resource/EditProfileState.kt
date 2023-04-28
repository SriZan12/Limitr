package com.example.limitr.resource

sealed class EditProfileState {
    class Loading(val progress: Long): EditProfileState()
    object Success: EditProfileState()
    class Error(val errorMessage: String): EditProfileState()
}