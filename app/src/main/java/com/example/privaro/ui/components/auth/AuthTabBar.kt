package com.example.privaro.ui.components.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.privaro.R
import com.example.privaro.ui.theme.PrivaroTeal

enum class AuthTab {
    SIGN_IN,
    SIGN_UP
}

@Composable
fun AuthTabBar(
    selectedTab: AuthTab,
    onTabSelected: (AuthTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE8E8E8))
    ) {
        TabItem(
            text = stringResource(R.string.auth_sign_in),
            isSelected = selectedTab == AuthTab.SIGN_IN,
            onClick = { onTabSelected(AuthTab.SIGN_IN) },
            modifier = Modifier.weight(1f)
        )
        TabItem(
            text = stringResource(R.string.auth_sign_up),
            isSelected = selectedTab == AuthTab.SIGN_UP,
            onClick = { onTabSelected(AuthTab.SIGN_UP) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isSelected) Color.White else Color.DarkGray,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) PrivaroTeal else Color.Transparent)
            .padding(vertical = 12.dp)
    )
}
