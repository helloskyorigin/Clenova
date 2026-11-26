package com.clenova.app.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clenova.app.core.designsystem.ClenovaMotion
import com.clenova.app.core.designsystem.ClenovaAutoIcon
import com.clenova.app.core.designsystem.ClenovaShapes
import com.clenova.app.core.designsystem.ClenovaSpacing
import com.clenova.app.core.designsystem.ClenovaThemeTokens
import com.example.R

/**
 * Custom iOS-inspired floating 3+1 bottom navigation system.
 * Left pill hosts Home, Clean, and Guard with a spring-animated indicator.
 * Separate Electric Blue circular button opens the dedicated Agent workspace.
 */
@Composable
fun ClenovaFloatingNavBar(
    currentRoute: ClenovaRoute?,
    onNavigate: (ClenovaRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val destinations = PillDestination.entries
    val selectedPillIndex = destinations.indexOfFirst { it.route == currentRoute }
    val isAgentSelected = currentRoute == ClenovaRoute.Agent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = ClenovaSpacing.screenHorizontal, vertical = ClenovaSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ClenovaSpacing.sm),
        ) {
            // Main 3-item floating pill
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = ClenovaShapes.pill,
                        ambientColor = Color.Black.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.12f),
                    ),
                shape = ClenovaShapes.pill,
                color = colors.navPillBackground,
                border = BorderStroke(1.dp, colors.border),
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                ) {
                    val slotWidth = maxWidth / destinations.size
                    val activeIndex = if (selectedPillIndex >= 0) selectedPillIndex else 0
                    val indicatorOffset by animateDpAsState(
                        targetValue = slotWidth * activeIndex,
                        animationSpec = ClenovaMotion.snappySpring(),
                        label = "nav_pill_indicator_offset",
                    )
                    val indicatorAlpha by animateFloatAsState(
                        targetValue = if (selectedPillIndex >= 0) 1f else 0f,
                        animationSpec = ClenovaMotion.quickTween(),
                        label = "nav_pill_indicator_alpha",
                    )

                    // Sliding Soft Blue active capsule
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(slotWidth)
                            .fillMaxHeight()
                            .alpha(indicatorAlpha)
                            .clip(ClenovaShapes.pill)
                            .background(colors.softBlue),
                    )

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        destinations.forEach { destination ->
                            val isSelected = currentRoute == destination.route
                            PillNavItem(
                                destination = destination,
                                selected = isSelected,
                                onClick = { onNavigate(destination.route) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }

            // Separate Electric Blue circular Agent button
            val agentContentDesc = stringResource(R.string.cd_agent_button)
            val agentBorder = if (isAgentSelected) {
                BorderStroke(2.5.dp, colors.softBlue)
            } else {
                null
            }
            val agentScaleAlpha by animateFloatAsState(
                targetValue = if (isAgentSelected) 1f else 0.96f,
                animationSpec = ClenovaMotion.snappySpring(),
                label = "agent_button_alpha",
            )

            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .minimumInteractiveComponentSize()
                    .shadow(
                        elevation = if (isAgentSelected) 14.dp else 10.dp,
                        shape = CircleShape,
                        ambientColor = colors.primary.copy(alpha = 0.28f),
                        spotColor = colors.primary.copy(alpha = 0.36f),
                    )
                    .clip(CircleShape)
                    .testTag("nav_tab_agent")
                    .semantics {
                        contentDescription = agentContentDesc
                        selected = isAgentSelected
                    }
                    .clickable(
                        role = Role.Tab,
                        onClick = { onNavigate(ClenovaRoute.Agent) },
                    ),
                shape = CircleShape,
                color = colors.primary,
                border = agentBorder,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(agentScaleAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ClenovaAutoIcon(
                        tint = colors.onPrimary,
                        size = 24.dp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.nav_agent),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isAgentSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PillNavItem(
    destination: PillDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClenovaThemeTokens.colors
    val label = stringResource(destination.labelRes)
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.textSecondary,
        animationSpec = ClenovaMotion.quickTween(),
        label = "nav_item_color",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(ClenovaShapes.pill)
            .testTag(destination.testTag)
            .semantics { this.selected = selected }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = colors.primary),
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
