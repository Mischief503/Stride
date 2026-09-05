package com.richie.stride.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.richie.stride.StrideApp

/**
 * A tiny generic ViewModelProvider.Factory: pass the constructor that needs the
 * Application's repositories, and this wires it up without needing Hilt/Dagger.
 */
class SimpleViewModelFactory<T : ViewModel>(
    private val create: (StrideApp) -> T
) : ViewModelProvider.Factory {
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StrideApp
        @Suppress("UNCHECKED_CAST")
        return create(app) as VM
    }
}
