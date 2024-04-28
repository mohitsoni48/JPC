package com.mohitsoni.jpc

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.LazyListSnapperLayoutInfo
import dev.chrisbanes.snapper.SnapperLayoutInfo
import dev.chrisbanes.snapper.rememberLazyListSnapperLayoutInfo
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(128.dp, 30.dp),
    rowCount: Int = 3,
    listOfValues: List<String>,
    startValue: String,
    activeColor: Color,
    frictionMultiplier: Float? = null,
    startIndex: Int = listOfValues.indexOf(startValue).let {
        if (it < 0) 0 else it
    },
    lazyListState: LazyListState = rememberLazyListState(startIndex),
    onSnappedIndexChanged: (snappedIndex: Int) -> Unit,
    onScrollFinished: (snappedIndex: Int) -> Int?,
) {

    val snapperLayoutInfo = rememberLazyListSnapperLayoutInfo(lazyListState = lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress
    val firstCompose = remember {
        mutableStateOf(true)
    }
    var snappedIndex by remember {
        mutableIntStateOf(startIndex)
    }
//    val view = LocalView.current

    LaunchedEffect(isScrollInProgress, listOfValues.size) {
        val calculatedIndex = calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex
        if (!isScrollInProgress && !firstCompose.value && calculatedIndex!= snappedIndex) {
            snappedIndex = calculatedIndex
            onScrollFinished(calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex)
        }
        firstCompose.value = false
    }

    LaunchedEffect(key1 = calculateSnappedItemIndex(snapperLayoutInfo)) {
        if (!firstCompose.value) {
            onSnappedIndexChanged.invoke(calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            modifier = Modifier
                .height(size.height)
                .width(size.width),
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = size.width / rowCount * ((rowCount - 1) / 2)),
            flingBehavior = rememberSnapperFlingBehavior(
                lazyListState = lazyListState,
                decayAnimationSpec = if (frictionMultiplier == null)
                    rememberSplineBasedDecay()
                else
                    splineBasedDecay(
                        Density(
                            LocalDensity.current.density.times(frictionMultiplier),
                            1f
                        )
                    )
            )
        ) {
            items(listOfValues.size) { index ->
                val rotationY = calculateAnimatedRotationY(
                    lazyListState = lazyListState,
                    snapperLayoutInfo = snapperLayoutInfo,
                    index = index,
                    rowCount = rowCount
                )
                Box(
                    modifier = Modifier
                        .height(size.height )
                        .width(size.width/ rowCount)
                        .alpha(
                            calculateAnimatedAlpha(
                                lazyListState = lazyListState,
                                snapperLayoutInfo = snapperLayoutInfo,
                                index = index,
                                rowCount = rowCount
                            )
                        )
                        .graphicsLayer {
                            if (rotationY
                                    .isNaN()
                                    .not()
                            )
                                this.rotationY = rotationY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = listOfValues[index],
                        style = getTextStyle(snapperLayoutInfo, index).copy(
                            //color = if (calculateSnappedItemIndex(snapperLayoutInfo) == index) activeColor else inactiveColor,
                            brush = Brush.verticalGradient(
                                if (calculateSnappedItemIndex(snapperLayoutInfo) == index)
                                    listOf(activeColor, activeColor)
                                else if (index > (calculateSnappedItemIndex(snapperLayoutInfo) ?: startIndex))
                                    listOf(Secondary1, Background2.copy(alpha = 0.63f))
                                else
                                    listOf(Background2, Secondary1.copy(alpha = 0.63f))
                            )
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
private fun getTextStyle(snapperLayoutInfo: LazyListSnapperLayoutInfo, index: Int): TextStyle {
    return if (calculateSnappedItemIndex(snapperLayoutInfo) == index)
        TextStyle(
            fontSize = 16.sp,
            color = White
        )
    else
        TextStyle(
            fontSize = 14.sp,
            color = Secondary1
        )
}


fun LazyListState.disableScrolling(scope: CoroutineScope) {
    scope.launch {
        scroll(scrollPriority = MutatePriority.PreventUserInput) {
            // Await indefinitely, blocking scrolls
            awaitCancellation()
        }
    }
}

fun LazyListState.reenableScrolling(scope: CoroutineScope) {
    scope.launch {
        scroll(scrollPriority = MutatePriority.PreventUserInput) {
            // Do nothing, just cancel the previous indefinite "scroll"
        }
    }
}

@OptIn(ExperimentalSnapperApi::class)
private fun calculateSnappedItemIndex(snapperLayoutInfo: SnapperLayoutInfo): Int? {
    var currentItemIndex = snapperLayoutInfo.currentItem?.index
    if(snapperLayoutInfo.currentItem?.offset != 0 && currentItemIndex != null && currentItemIndex < snapperLayoutInfo.totalItemsCount - 1) {
        currentItemIndex ++
    }
    return currentItemIndex
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
private fun calculateAnimatedAlpha(
    lazyListState: LazyListState,
    snapperLayoutInfo: SnapperLayoutInfo,
    index: Int,
    rowCount: Int
): Float {

    val distanceToIndexSnap = snapperLayoutInfo.distanceToIndexSnap(index).absoluteValue
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortWidth = layoutInfo.viewportSize.width.toFloat()
    val singleViewPortHeight = viewPortWidth / rowCount

    return if (distanceToIndexSnap in 0..singleViewPortHeight.toInt()) {
        1.8f - (distanceToIndexSnap / singleViewPortHeight)
    } else {
        0.8f
    }
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
private fun calculateAnimatedRotationY(
    lazyListState: LazyListState,
    snapperLayoutInfo: SnapperLayoutInfo,
    index: Int,
    rowCount: Int
): Float {

    val distanceToIndexSnap = snapperLayoutInfo.distanceToIndexSnap(index)
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortWidth = layoutInfo.viewportSize.width.toFloat()
    val singleViewPortHeight = viewPortWidth / rowCount

    return -20f * (distanceToIndexSnap / singleViewPortHeight)
}

val Secondary1 = Color(0xFFB0B4CD)
val White = Color(0xFFFFFFFF)
val Background2 = Color(0x440A0A0A)