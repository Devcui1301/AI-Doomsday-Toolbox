package com.example.llamadroid.util

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.RandomAccessFile

data class SystemStats(
    val cpuUsagePercent: Int,
    val ramUsagePercent: Int,
    val freeRamGb: Float,
    val totalRamGb: Float
)

class SystemMonitor(private val context: Context) {
    
    fun observeStats(): Flow<SystemStats> = flow {
        while(true) {
            emit(getStats())
            delay(2000)
        }
    }
    
    private fun getStats(): SystemStats {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        
        return SystemStats(
            cpuUsagePercent = readCpuUsage(),
            ramUsagePercent = readRamUsage(),
            freeRamGb = memInfo.availMem / (1024f * 1024f * 1024f),
            totalRamGb = memInfo.totalMem / (1024f * 1024f * 1024f)
        )
    }

    private var lastTotal = 0L
    private var lastIdle = 0L

    private fun readCpuUsage(): Int {
        try {
            // Try reading overall CPU from /proc/stat
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" +".toRegex())
            // toks[0] = "cpu", toks[1..7] = user, nice, system, idle, iowait, irq, softirq
            if (toks.size < 5) return 0
            
            val idle = toks[4].toLongOrNull() ?: 0L
            val total = toks.drop(1).take(7).mapNotNull { it.toLongOrNull() }.sum()

            val diffIdle = idle - lastIdle
            val diffTotal = total - lastTotal
            
            lastIdle = idle
            lastTotal = total
            
            if (diffTotal == 0L) return 0
            return ((diffTotal - diffIdle) * 100 / diffTotal).toInt()
        } catch (e: SecurityException) {
            // /proc/stat may be restricted on Android 8+
            // Fallback: estimate based on Runtime CPU load (less accurate)
            return estimateCpuLoad()
        } catch (e: Exception) {
            return 0
        }
    }
    
    private fun estimateCpuLoad(): Int {
        // This is a placeholder - true CPU load requires native code or repeated sampling
        // Return a simulated value based on available processors and memory pressure
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        // Very rough estimation based on memory pressure
        return ((usedMemory.toDouble() / maxMemory.toDouble()) * 50).toInt().coerceIn(0, 100)
    }

    private fun readRamUsage(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val total = memInfo.totalMem
        val avail = memInfo.availMem
        return (((total - avail) * 100) / total).toInt()
    }
    
    private fun getFreeRam(): Float {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024f * 1024f * 1024f) 
    }
}
