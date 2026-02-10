package com.blenko.mediamate

enum class MediaCommand(val avrcpCode: Byte) {
    PLAY_PAUSE(0x46),  // Using PAUSE code; toggle behavior handled by receiver
    PLAY(0x44),
    PAUSE(0x46),
    STOP(0x45),
    NEXT(0x4B),
    PREVIOUS(0x4C),
    FAST_FORWARD(0x49),
    REWIND(0x48),
    VOLUME_UP(0x41),
    VOLUME_DOWN(0x42)
}