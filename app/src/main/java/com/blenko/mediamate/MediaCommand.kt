package com.blenko.mediamate

enum class MediaCommand(val avrcpCode: Byte) {
    PLAY_PAUSE(0x46),
    NEXT(0x4B),
    PREVIOUS(0x4C),
    VOLUME_UP(0x41),
    VOLUME_DOWN(0x42),
    PLAY(0x44),
    PAUSE(0x46),
    STOP(0x45),
    FAST_FORWARD(0x49),
    REWIND(0x48)
}