package com.codeSmithLabs.organizeemail.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleNormal: String,
    val titleBold: String,
    val description: String,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            titleNormal = "Welcome to your",
            titleBold = "clutter-free inbox",
            description = "Experience a new way to manage emails. We help you categorize and clean up your digital space efficiently.",
            gradientColors = listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF), Color.White) // Pink/Peach soft gradient
        ),
        OnboardingPage(
            titleNormal = "Smart cleanup for",
            titleBold = "peace of mind",
            description = "Our Cleanup Assistant intelligently identifies promotional emails and heavy attachments, so you can focus on what matters.",
            gradientColors = listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB), Color.White) // Purple/Lavender soft gradient
        ),
        OnboardingPage(
            titleNormal = "Your privacy is",
            titleBold = "our priority",
            description = "We do NOT see, save, or store your emails. Everything happens securely right on your device.",
            gradientColors = listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4), Color.White) // Teal/Blue soft gradient
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    // Smoothly animate the gradient colors based on the current page
    val currentColors = pages[pagerState.currentPage].gradientColors
    val color1 by animateColorAsState(targetValue = currentColors[0], animationSpec = tween(durationMillis = 500), label = "color1")
    val color2 by animateColorAsState(targetValue = currentColors[1], animationSpec = tween(durationMillis = 500), label = "color2")
    val color3 by animateColorAsState(targetValue = currentColors[2], animationSpec = tween(durationMillis = 500), label = "color3")

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }
    ) { _ -> // Ignore scaffold padding for full screen background
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Static Background Layer with Animated Colors
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(x = size.width * 0.8f, y = size.height * 0.2f)
                val radius = size.width * 1.2f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color1, color2, color3),
                        center = centerOffset,
                        radius = radius
                    ),
                    center = centerOffset,
                    radius = radius
                )
            }

            // Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding() // Add system bars padding here to content only
            ) {
                // Top Right Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onFinish) {
                        Text(
                            "Skip",
                            color = Color.Black.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        OnboardingPageContent(page = pages[pageIndex])
                    }
                }

                // Bottom Indicators
                Row(
                    Modifier
                        .padding(start = 32.dp, bottom = 48.dp)
                        .height(20.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    repeat(pages.size) { iteration ->
                        val width = if (pagerState.currentPage == iteration) 24.dp else 8.dp
                        val color = if (pagerState.currentPage == iteration) Color.Black else Color.Black.copy(alpha = 0.2f)
                        
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .size(width = width, height = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Removed Canvas from here to avoid sliding animation
        
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Light)) {
                    append(page.titleNormal + "\n")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(page.titleBold)
                }
            },
            fontSize = 40.sp,
            lineHeight = 48.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black.copy(alpha = 0.6f),
            lineHeight = 28.sp,
            fontSize = 18.sp
        )
    }
}
