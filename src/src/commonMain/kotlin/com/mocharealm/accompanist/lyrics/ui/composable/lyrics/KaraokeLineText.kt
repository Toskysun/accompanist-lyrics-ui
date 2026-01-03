package com.mocharealm.accompanist.lyrics.ui.composable.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.ui.utils.LayerPaint
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Bounce
import com.mocharealm.accompanist.lyrics.ui.utils.easing.DipAndRise
import com.mocharealm.accompanist.lyrics.ui.utils.easing.Swell
import com.mocharealm.accompanist.lyrics.ui.utils.isPunctuation
import com.mocharealm.accompanist.lyrics.ui.utils.isRtl
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import kotlin.math.roundToInt

private fun createLineGradientBrush(
    lineLayout: List<SyllableLayout>,
    currentTimeMs: Int,
    isRtl: Boolean
): Brush {
    val activeColor = Color.White
    val inactiveColor = Color.White.copy(alpha = 0.2f)
    val minFadeWidth = 100f

    if (lineLayout.isEmpty()) {
        return Brush.horizontalGradient(colors = listOf(inactiveColor, inactiveColor))
    }

    val totalMinX = lineLayout.minOf { it.position.x }
    val totalMaxX = lineLayout.maxOf { it.position.x + it.width }
    val totalWidth = totalMaxX - totalMinX

    if (totalWidth <= 0f) {
        val isFinished = currentTimeMs >= lineLayout.last().syllable.end
        val color = if (isFinished) activeColor else inactiveColor
        return Brush.horizontalGradient(listOf(color, color))
    }

    val firstSyllableStart = lineLayout.first().syllable.start
    val lastSyllableEnd = lineLayout.last().syllable.end

    val lineProgress = run {
        if (currentTimeMs <= firstSyllableStart) return Brush.horizontalGradient(
            listOf(inactiveColor, inactiveColor)
        )
        if (currentTimeMs >= lastSyllableEnd) return Brush.horizontalGradient(
            listOf(activeColor, activeColor)
        )

        val activeSyllableLayout = lineLayout.find {
            currentTimeMs in it.syllable.start until it.syllable.end
        }

        val currentPixelPosition = when {
            activeSyllableLayout != null -> {
                val syllableProgress = activeSyllableLayout.syllable.progress(currentTimeMs)
                if (isRtl) {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * (1f - syllableProgress)
                } else {
                    activeSyllableLayout.position.x + activeSyllableLayout.width * syllableProgress
                }
            }
            else -> {
                val lastFinished = lineLayout.lastOrNull { currentTimeMs >= it.syllable.end }
                if (isRtl) {
                    lastFinished?.position?.x ?: totalMaxX
                } else {
                    lastFinished?.let { it.position.x + it.width } ?: totalMinX
                }
            }
        }
        ((currentPixelPosition - totalMinX) / totalWidth).coerceIn(0f, 1f)
    }

    val fadeRange = (minFadeWidth / totalWidth).coerceAtMost(1f)
    val fadeCenterStart = -fadeRange / 2f
    val fadeCenterEnd = 1f + fadeRange / 2f
    val fadeCenter = fadeCenterStart + (fadeCenterEnd - fadeCenterStart) * lineProgress
    val fadeStart = fadeCenter - fadeRange / 2f
    val fadeEnd = fadeCenter + fadeRange / 2f

    val colorStops = if (isRtl) {
        arrayOf(
            0.0f to inactiveColor,
            fadeStart.coerceIn(0f, 1f) to inactiveColor,
            fadeEnd.coerceIn(0f, 1f) to activeColor,
            1.0f to activeColor
        )
    } else {
        arrayOf(
            0.0f to activeColor,
            fadeStart.coerceIn(0f, 1f) to activeColor,
            fadeEnd.coerceIn(0f, 1f) to inactiveColor,
            1.0f to inactiveColor
        )
    }

    return Brush.horizontalGradient(
        colorStops = colorStops,
        startX = totalMinX,
        endX = totalMaxX
    )
}

fun DrawScope.drawLine(
    lineLayouts: List<List<SyllableLayout>>,
    currentTimeMs: Int,
    color: Color,
    blendMode: BlendMode,
    isRtl: Boolean,
    showDebugRectangles: Boolean = false
) {
    lineLayouts.forEach { rowLayouts ->
        if (rowLayouts.isEmpty()) return@forEach

        val lastSyllableEnd = rowLayouts.last().syllable.end

        if (currentTimeMs >= lastSyllableEnd) {
            drawRowText(rowLayouts, color, blendMode, showDebugRectangles, currentTimeMs)
            return@forEach
        }

        val minX = rowLayouts.minOf { it.position.x }
        val maxX = rowLayouts.maxOf { it.position.x + it.width }
        val minY = rowLayouts.minOf { it.position.y }
        val totalHeight = rowLayouts.maxOf { it.textLayoutResult.size.height }.toFloat()

        val verticalPadding = (totalHeight * 0.1).dp.toPx()
        val horizontalPadding = ((maxX - minX) * 0.2).dp.toPx()

        drawIntoCanvas { canvas ->
            val layerBounds = Rect(
                left = minX - horizontalPadding,
                top = minY - verticalPadding,
                right = maxX + horizontalPadding,
                bottom = minY + totalHeight + verticalPadding
            )
            canvas.saveLayer(layerBounds, LayerPaint)

            drawRowText(rowLayouts, color, blendMode, showDebugRectangles, currentTimeMs)

            val progressBrush = createLineGradientBrush(rowLayouts, currentTimeMs, isRtl)
            drawRect(
                brush = progressBrush,
                topLeft = layerBounds.topLeft,
                size = layerBounds.size,
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

private fun DrawScope.drawRowText(
    rowLayouts: List<SyllableLayout>,
    drawColor: Color,
    blendMode: BlendMode,
    showDebugRectangles: Boolean,
    currentTimeMs: Int
) {
    rowLayouts.forEachIndexed { index, syllableLayout ->
        val wordAnimInfo = syllableLayout.wordAnimInfo

        if (wordAnimInfo != null) {
            val fastCharAnimationThresholdMs = 200f
            val awesomeDuration = wordAnimInfo.wordDuration * 0.8f

            val charLayouts = syllableLayout.charLayouts ?: emptyList()
            val charBounds = syllableLayout.charOriginalBounds ?: emptyList()

            syllableLayout.syllable.content.forEachIndexed { charIndex, _ ->
                val singleCharLayoutResult = charLayouts.getOrNull(charIndex) ?: return@forEachIndexed
                val charBox = charBounds.getOrNull(charIndex) ?: return@forEachIndexed

                val absoluteCharIndex = syllableLayout.charOffsetInWord + charIndex
                val numCharsInWord = wordAnimInfo.wordContent.length
                val earliestStartTime = wordAnimInfo.wordStartTime
                val latestStartTime = wordAnimInfo.wordEndTime - awesomeDuration

                val charRatio = if (numCharsInWord > 1) absoluteCharIndex.toFloat() / (numCharsInWord - 1) else 0.5f
                val awesomeStartTime = (earliestStartTime + (latestStartTime - earliestStartTime) * charRatio).toLong()
                val awesomeProgress = ((currentTimeMs - awesomeStartTime).toFloat() / awesomeDuration).coerceIn(0f, 1f)

                val floatOffset = 4f * DipAndRise(
                    dip = ((0.5 * (wordAnimInfo.wordDuration - fastCharAnimationThresholdMs * numCharsInWord) / 1000)).coerceIn(0.0, 0.5)
                ).transform(1.0f - awesomeProgress)
                val scale = 1f + Swell(
                    (0.1 * (wordAnimInfo.wordDuration - fastCharAnimationThresholdMs * numCharsInWord) / 1000).coerceIn(0.0, 0.1)
                ).transform(awesomeProgress)

                val centeredOffsetX = (charBox.width - singleCharLayoutResult.size.width) / 2f
                val xPos = syllableLayout.position.x + charBox.left + centeredOffsetX
                val yPos = syllableLayout.position.y + charBox.top + floatOffset

                val blurRadius = 10f * Bounce.transform(awesomeProgress)
                val shadow = Shadow(
                    color = drawColor.copy(0.4f),
                    offset = Offset(0f, 0f),
                    blurRadius = blurRadius
                )

                withTransform({ scale(scale = scale, pivot = syllableLayout.wordPivot) }) {
                    drawText(
                        textLayoutResult = singleCharLayoutResult,
                        color = drawColor,
                        topLeft = Offset(xPos, yPos),
                        shadow = shadow,
                        blendMode = blendMode
                    )
                    if (showDebugRectangles) {
                        drawRect(
                            color = Color.Red, topLeft = Offset(xPos, yPos), size = Size(
                                singleCharLayoutResult.size.width.toFloat(),
                                singleCharLayoutResult.size.height.toFloat()
                            ), style = Stroke(1f)
                        )
                    }
                }
            }
        } else {
            val driverLayout = if (syllableLayout.syllable.content.trim().isPunctuation()) {
                var searchIndex = index - 1
                while (searchIndex >= 0) {
                    val candidate = rowLayouts[searchIndex]
                    if (!candidate.syllable.content.trim().isPunctuation()) {
                        break
                    }
                    searchIndex--
                }
                if (searchIndex < 0) syllableLayout else rowLayouts[searchIndex]
            } else {
                syllableLayout
            }

            val animationFixedDuration = 700f
            val timeSinceStart = currentTimeMs - driverLayout.syllable.start
            val animationProgress = (timeSinceStart / animationFixedDuration).coerceIn(0f, 1f)

            val maxOffsetY = 4f
            val floatCurveValue = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f).transform(1f - animationProgress)
            val floatOffset = maxOffsetY * floatCurveValue

            val finalPosition = syllableLayout.position.copy(
                y = syllableLayout.position.y + floatOffset
            )

            drawText(
                textLayoutResult = syllableLayout.textLayoutResult,
                color = drawColor,
                topLeft = finalPosition,
                blendMode = blendMode
            )
            if (showDebugRectangles) {
                drawRect(
                    color = Color.Green, topLeft = finalPosition, size = Size(
                        syllableLayout.textLayoutResult.size.width.toFloat(),
                        syllableLayout.textLayoutResult.size.height.toFloat()
                    ), style = Stroke(2f)
                )
            }
        }
    }
}

@Stable
@Composable
fun KaraokeLineText(
    line: KaraokeLine,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    currentTimeProvider: () -> Int,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    normalLineTextStyle: TextStyle,
    accompanimentLineTextStyle: TextStyle,
    blendMode: BlendMode = BlendMode.Plus,
    showDebugRectangles: Boolean = false,
    precalculatedLayouts: List<SyllableLayout>? = null
) {

    // 1. RTL 检测
    val isRtl = remember(line.syllables) {
        line.syllables.any { it.content.isRtl() }
    }

    val textMeasurer = rememberTextMeasurer()

    // 2. 核心对齐逻辑计算
    // Unspecified 默认视为 Start (跟随语言自然方向)
    val isRightAligned = remember(line.alignment, isRtl) {
        when (line.alignment) {
            // Start 或 Unspecified:
            // 如果是 RTL，Start 意味着在右边 -> True
            // 如果是 LTR，Start 意味着在左边 -> False
            KaraokeAlignment.Start, KaraokeAlignment.Unspecified -> isRtl

            // End:
            // 如果是 RTL，End 意味着在左边 -> False
            // 如果是 LTR，End 意味着在右边 -> True
            KaraokeAlignment.End -> !isRtl
        }
    }

    // 3. 动画锚点：如果是右对齐，缩放中心点在右侧 (1f)，否则在左侧 (0f)
    val scaleTransformOrigin = remember(isRightAligned) {
        TransformOrigin(
            pivotFractionX = if (isRightAligned) 1f else 0f,
            pivotFractionY = 1f
        )
    }

    // 4. 翻译文本对齐
    val translationTextAlign = remember(isRightAligned) {
        if (isRightAligned) TextAlign.End else TextAlign.Start
    }

    // Column 内部对齐 (用于非 Canvas 元素，如翻译文本)
    val columnHorizontalAlignment = remember(isRightAligned) {
        if (isRightAligned) Alignment.End else Alignment.Start
    }

    val animatedScaleState = animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.98f, animationSpec = if (isFocused) {
            androidx.compose.animation.core.tween(
                durationMillis = 600, easing = LinearOutSlowInEasing
            )
        } else {
            androidx.compose.animation.core.tween(
                durationMillis = 300, easing = EaseInOut
            )
        }, label = "scale"
    )
    val animatedAlphaState = animateFloatAsState(
        targetValue = if (!line.isAccompaniment) if (isFocused) 1f else 0.4f else if (isFocused) 0.6f else 0.2f,
        label = "alpha"
    )

    Box(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(8.dp))
            .combinedClickable(
                onClick = { onLineClicked(line) },
                onLongClick = { onLinePressed(line) }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // 关键：始终占满宽度，不使用 align 移动 Column
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .graphicsLayer {
                    scaleX = animatedScaleState.value
                    scaleY = animatedScaleState.value
                    transformOrigin = scaleTransformOrigin
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = columnHorizontalAlignment // 控制翻译文本的位置
        ) {
            BoxWithConstraints(Modifier.graphicsLayer {
                alpha = animatedAlphaState.value
            }) {
                val density = LocalDensity.current
                val availableWidthPx = with(density) { maxWidth.toPx() }

                val textStyle = remember(line.isAccompaniment) {
                    val baseStyle = if (line.isAccompaniment) accompanimentLineTextStyle else normalLineTextStyle
                    baseStyle.copy(textDirection = TextDirection.Content)
                }

                val spaceWidth = remember(textMeasurer, textStyle) {
                    textMeasurer.measure(" ", textStyle).size.width.toFloat()
                }

                val processedSyllables = remember(line.syllables, line.alignment) {
                    // 仅在 End 且非 RTL 时移除尾部空格，或者根据需要调整逻辑
                    // 简单起见，这里可以保持原逻辑，或者去掉 trim 逻辑以保证数据一致性
                    if (line.alignment == KaraokeAlignment.End) {
                        line.syllables.dropLastWhile { it.content.isBlank() }
                    } else {
                        line.syllables
                    }
                }

                val initialLayouts by remember(precalculatedLayouts) {
                    derivedStateOf {
                        precalculatedLayouts ?: measureSyllablesAndDetermineAnimation(
                            syllables = processedSyllables,
                            textMeasurer = textMeasurer,
                            style = textStyle,
                            isAccompanimentLine = line.isAccompaniment,
                            spaceWidth = spaceWidth
                        )
                    }
                }


                val wrappedLines by remember {
                    derivedStateOf {
                        calculateBalancedLines(
                            syllableLayouts = initialLayouts,
                            availableWidthPx = availableWidthPx,
                            textMeasurer = textMeasurer,
                            style = textStyle
                        )
                    }
                }

                val lineHeight = remember(textStyle) {
                    textMeasurer.measure("M", textStyle).size.height.toFloat()
                }

                // 传入 isRightAligned，完全由 calculateStaticLineLayout 计算 X 坐标
                val finalLineLayouts = remember(wrappedLines, availableWidthPx, lineHeight, isRtl, isRightAligned) {
                    calculateStaticLineLayout(
                        wrappedLines = wrappedLines,
                        isLineRightAligned = isRightAligned,
                        canvasWidth = availableWidthPx,
                        lineHeight = lineHeight,
                        isRtl = isRtl
                    )
                }

                val totalHeight = remember(wrappedLines, lineHeight) {
                    lineHeight * wrappedLines.size
                }

                // Canvas 始终填满 BoxWithConstraints 给的宽度 (maxWidth)
                Canvas(modifier = Modifier.size(maxWidth, (totalHeight.roundToInt() + 8).toDp())) {
                    val time = currentTimeProvider()
                    drawLine(
                        lineLayouts = finalLineLayouts,
                        currentTimeMs = time,
                        color = activeColor,
                        blendMode = blendMode,
                        isRtl = isRtl,
                        showDebugRectangles = showDebugRectangles
                    )
                }
            }

            line.translation?.let { translation ->
                Text(
                    text = translation,
                    color = activeColor.copy(0.4f),
                    modifier = Modifier.graphicsLayer {
                        this.blendMode = blendMode
                    },
                    textAlign = translationTextAlign // 翻译文本使用 Text 的对齐属性
                )
            }
        }
    }
}



@Composable
private fun Int.toDp(): Dp = with(LocalDensity.current) { this@toDp.toDp() }

