package com.example.privaro.ui.components.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.ui.theme.PacificoFontFamily
import com.example.privaro.ui.theme.PrivaroTeal

@Composable
fun AuthHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(PrivaroTeal),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Privaro",
            fontFamily = PacificoFontFamily,
            fontSize = 52.sp,
            fontStyle = FontStyle.Italic,
            color = Color.White,
            modifier = Modifier.padding(bottom = 40.dp)
        )
    }
}
