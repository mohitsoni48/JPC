package com.mohitsoni.jpc

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeightPicker() {
    val value = remember { mutableIntStateOf(50) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = value.intValue.toString(), fontSize = 28.sp)
        Box(modifier = Modifier.height(20.dp))

        Image(imageVector = ImageVector.vectorResource(id = R.drawable.arrow), "", modifier = Modifier.rotate(180f))
        WheelPicker(
            listOfValues = (0..9999).toList().map { it.toString() },
            startValue = "50",
            activeColor = White,
            onSnappedIndexChanged = {
                value.intValue = it
            }
        ) {
            value.intValue = it
            it
        }
        Image(imageVector = ImageVector.vectorResource(id = R.drawable.arrow), "")
    }

}

