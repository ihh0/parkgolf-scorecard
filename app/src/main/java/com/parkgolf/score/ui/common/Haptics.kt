package com.parkgolf.score.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** 홀 내비게이션 햅틱 피드백. 이전/다음/완료를 서로 다른 패턴으로 제공. */
object Haptics {

    private fun vibrator(context: Context): Vibrator? {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        return v.takeIf { it.hasVibrator() }
    }

    /** 다음 홀: 단발 짧은 틱. */
    fun next(context: Context) = oneShot(context, 28L)

    /** 이전 홀: 더블 짧은 틱(다음과 구분). */
    fun prev(context: Context) = waveform(context, longArrayOf(0, 16, 40, 16))

    /** 게임 완료: 길고 리듬감 있는 3연타. */
    fun complete(context: Context) = waveform(context, longArrayOf(0, 40, 60, 40, 60, 90))

    private fun oneShot(context: Context, ms: Long) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }

    private fun waveform(context: Context, pattern: LongArray) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION") v.vibrate(pattern, -1)
        }
    }
}
