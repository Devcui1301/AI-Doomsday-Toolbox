package com.example.llamadroid.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State for Stable Diffusion / A1111 installation progress
 */
sealed class SDInstallState {
    object NotInstalled : SDInstallState()
    object CheckingStatus : SDInstallState()
    data class Downloading(val progress: Float, val step: String = "Downloading Ubuntu rootfs...") : SDInstallState()
    data class Extracting(val progress: Float, val step: String = "Extracting filesystem...") : SDInstallState()
    data class InstallingDeps(val step: String, val progress: Float = -1f) : SDInstallState()
    data class CloningRepo(val progress: Float, val step: String = "Cloning A1111 repository...") : SDInstallState()
    data class SettingUpVenv(val progress: Float, val step: String = "Setting up Python environment...") : SDInstallState()
    object Installed : SDInstallState()
    data class Error(val message: String) : SDInstallState()
}

/**
 * State for SD server (A1111 WebUI) runtime
 */
sealed class SDServerState {
    object Stopped : SDServerState()
    object Starting : SDServerState()
    data class Running(val port: Int) : SDServerState()
    data class Error(val message: String) : SDServerState()
}

/**
 * Singleton holder for SD installation and server state
 */
object SDStateHolder {
    private val _installState = MutableStateFlow<SDInstallState>(SDInstallState.NotInstalled)
    val installState = _installState.asStateFlow()
    
    private val _serverState = MutableStateFlow<SDServerState>(SDServerState.Stopped)
    val serverState = _serverState.asStateFlow()
    
    fun updateInstallState(state: SDInstallState) {
        _installState.value = state
    }
    
    fun updateServerState(state: SDServerState) {
        _serverState.value = state
    }
}
