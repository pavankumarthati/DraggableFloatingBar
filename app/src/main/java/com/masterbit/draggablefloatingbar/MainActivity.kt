package com.masterbit.draggablefloatingbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VectorizedFloatAnimationSpec
import androidx.compose.animation.core.VectorizedSpringSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.masterbit.draggablefloatingbar.ui.theme.DraggableFloatingBarTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DraggableFloatingBarTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DraggableSample(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DraggableSample(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
        val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        var extended by  remember { mutableStateOf(false) }

        ExtendableFloatingActionButton(
            Modifier
                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
                .pointerInput(Unit) {
                    val decay = SplineBasedFloatDecayAnimationSpec(this)
                    coroutineScope {
                        val tracker = VelocityTracker()
                        detectDragGestures(onDragEnd = {
                            val velocity =
                                Offset(tracker.calculateVelocity().x, tracker.calculateVelocity().y)
                            val targetX = decay.getTargetValue(offset.value.x, velocity.x)
                            val targetY = decay.getTargetValue(offset.value.y, velocity.y)
                            val calculatedX =
                                if (targetX.absoluteValue < (width - targetX.absoluteValue).absoluteValue) {
                                    0f
                                } else {
                                    width
                                }

                            val calculatedY =
                                if (targetY.absoluteValue < (height - targetY.absoluteValue).absoluteValue) {
                                    0f
                                } else {
                                    height
                                }
                            offset.updateBounds(
                                Offset(0f, 0f),
                                Offset(width - size.width, height - size.height.toFloat())
                            )

                            launch {
                                offset.animateTo(
                                    Offset(calculatedX, calculatedY),
                                    TweenSpec<Offset>(),
                                    velocity
                                )
                            }
                        }, onDrag = { change, dragAmount ->
                            change.consumeAllChanges()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            launch {
                                offset.snapTo(
                                    Offset(
                                        offset.value.x + dragAmount.x,
                                        offset.value.y + dragAmount.y
                                    )
                                )
                            }
                        })
                    }
                }
        , extended, onClick = { extended = !extended})
    }
}


@Composable
fun ExtendableFloatingActionButton(
    modifier: Modifier = Modifier,
    extended: Boolean,
    text: @Composable () -> Unit = {Text("Hello", style = TextStyle(color = Color.Black))},
    icon: @Composable () -> Unit = { Icon(
        imageVector = Icons.Outlined.AccountBox,
        contentDescription = null
    )},
    onClick: () -> Unit = {}
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = extended) {
            Column {
                Spacer(Modifier.size(12.dp))
                Box(
                    modifier = Modifier.size(64.dp).clip(shape = CircleShape).background(
                        color = MaterialTheme.colors.secondary
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    text()
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.size(64.dp).clip(shape = CircleShape).background(
                        color = MaterialTheme.colors.secondary
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    text()
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier.size(64.dp).clip(shape = CircleShape).background(
                        color = MaterialTheme.colors.secondary
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    text()
                }
                Spacer(Modifier.size(12.dp))
            }
        }
        FloatingActionButton(
            modifier = Modifier.size(64.dp),
            onClick = onClick,
        ) {
            Box(modifier = Modifier.padding(horizontal = PaddingSize, vertical = PaddingSize)) {
                icon()
            }
        }
    }

}

private val PaddingSize = 16.dp



// @OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun DraggableSample(modifier: Modifier = Modifier) {
//    BoxWithConstraints(Modifier.fillMaxSize()) {
//        val offset = remember { Animatable(Offset(0f, 0f), Offset.VectorConverter) }
//        val width = constraints.maxWidth.toFloat()
//        val height = constraints.maxHeight.toFloat()
//
//        Box(
//            Modifier
//                .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
//                .background(Color.Blue)
//                .size(50.dp)
//                .pointerInput(Unit) {
//                    val decay = SplineBasedFloatDecayAnimationSpec(this)
//                    coroutineScope {
//                        val tracker = VelocityTracker()
//                        detectDragGestures(onDragEnd = {
//                            val velocity = Offset(tracker.calculateVelocity().x, tracker.calculateVelocity().y)
//                            val targetX = decay.getTargetValue(offset.value.x, velocity.x)
//                            val targetY = decay.getTargetValue(offset.value.y, velocity.y)
//
//                            val calculatedX = if (targetX.absoluteValue  < (width - targetX.absoluteValue).absoluteValue) {
//                                0f
//                            } else {
//                                width
//                            }
//
//                            val calculatedY = if (targetY.absoluteValue  < (height - targetY.absoluteValue).absoluteValue) {
//                                0f
//                            } else {
//                                height
//                            }
//                            offset.updateBounds(
//                                Offset(0f, 0f),
//                                Offset(width - size.width, height - size.height.toFloat())
//                            )
//
//                            launch {
//                                offset.animateTo(Offset(calculatedX, calculatedY), SpringSpec<Offset>(), velocity)
//                            }
//                        }, onDrag = { change, dragAmount ->
//                            change.consumeAllChanges()
//                            tracker.addPosition(change.uptimeMillis, change.position)
//                            launch {
//                                offset.snapTo(
//                                    Offset(
//                                        offset.value.x + dragAmount.x,
//                                        offset.value.y + dragAmount.y
//                                    )
//                                )
//                            }
//                        })
//                    }
//                }
//        )
//    }
//}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DraggableFloatingBarTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            DraggableSample(Modifier.fillMaxSize())
        }
    }
}