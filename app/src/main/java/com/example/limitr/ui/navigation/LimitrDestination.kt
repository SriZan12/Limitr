package com.example.limitr.ui.navigation

sealed interface LimitrDestination {
    val route: String

    data object Signup : LimitrDestination {
        override val route: String = "signup"
    }

    data object Home : LimitrDestination {
        override val route: String = "home"
    }

    data object EditProfile : LimitrDestination {
        override val route: String = "edit_profile"
    }
}
