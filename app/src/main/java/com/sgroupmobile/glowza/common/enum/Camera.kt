package com.sgroupmobile.glowza.common.enum

enum class CameraRatio(val value: String) {
    RATIO_4_3("3:4"),
    RATIO_16_9("9:16"),
    RATIO_1_1("1:1")
}

enum class CameraTimer(val time: Int) {
    TIMER_3S(3),
    TIMER_5S(5),
    TIMER_9S(9),
    TIMER_OFF(0)
}