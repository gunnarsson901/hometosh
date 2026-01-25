package com.example.dpicontroller.model

import com.google.gson.annotations.SerializedName

data class DpiConfig(
    @SerializedName("hactive") var hActive: Int = 512,
    @SerializedName("hfp") var hFp: Int = 16,
    @SerializedName("hsync") var hSync: Int = 32,
    @SerializedName("hbp") var hBp: Int = 48,
    
    @SerializedName("vactive") var vActive: Int = 342,
    @SerializedName("vfp") var vFp: Int = 10,
    @SerializedName("vsync") var vSync: Int = 2,
    @SerializedName("vbp") var vBp: Int = 30,
    
    @SerializedName("clock-frequency") var clockFreq: Int = 15667200,

    @SerializedName("color-format") var colorFormat: String = "rgb565",
    @SerializedName("color-mode") var colorMode: String = "default",
    @SerializedName("temperature") var temperature: Int = 6500
)