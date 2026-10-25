package app.shoutsend.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.StatusPill
import kotlinx.coroutines.launch

/**
 * 4-page animated onboarding flow introducing acoustic communication and managing initial microphone permission.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setPage(page)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChirpTheme.colors.background)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Navigation Row: Indicator & Skip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = ChirpTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Morphing Page Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isSelected = pagerState.currentPage == i
                    val width = if (isSelected) ChirpTheme.spacing.lg else ChirpTheme.spacing.sm
                    val color = if (isSelected) ChirpTheme.colors.primary else ChirpTheme.colors.outlineVariant

                    Box(
                        modifier = Modifier
                            .height(ChirpTheme.spacing.xs)
                            .width(width)
                            .clip(ChirpTheme.shapes.extraSmall)
                            .background(color)
                    )
                }
            }

            // Skip Button (pages 0, 1, 2)
            AnimatedVisibility(
                visible = pagerState.currentPage < 3,
                enter = fadeIn(tween(ChirpTheme.motion.durationShort2)),
                exit = fadeOut(tween(ChirpTheme.motion.durationShort2))
            ) {
                CdTextButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(3)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_btn_skip),
                        style = ChirpTheme.typography.labelLarge,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                }
            }
        }

        // --- Pager Content ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                }
        ) { page ->
            when (page) {
                0 -> PageTalkInSound()
                1 -> PageHowItWorks()
                2 -> PageGoodToKnow()
                3 -> PageAllowMicrophone(
                    micState = uiState.micPermissionState,
                    onRequestPermission = {
                        viewModel.onPermissionRequested()
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onFinish = {
                        viewModel.completeOnboarding(onFinished)
                    }
                )
            }
        }

        // --- Bottom Controls ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ChirpTheme.spacing.sm)
        ) {
            if (pagerState.currentPage < 3) {
                CdButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_btn_next))
                }
            }
        }
    }
}

@Composable
private fun PageTalkInSound() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = ChirpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TalkInSoundIllustration(modifier = Modifier.size(ChirpTheme.sizes.iconXl * 4))
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_page1_title),
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )
                Text(
                    text = stringResource(R.string.onboarding_page1_body),
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageHowItWorks() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = ChirpTheme.spacing.sm)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(ChirpTheme.sizes.iconXl * 2)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HowItWorksIllustration(modifier = Modifier.fillMaxSize())
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_page2_title),
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )

                StepItem(
                    title = stringResource(R.string.onboarding_step1_title),
                    body = stringResource(R.string.onboarding_step1_body)
                )
                StepItem(
                    title = stringResource(R.string.onboarding_step2_title),
                    body = stringResource(R.string.onboarding_step2_body)
                )
                StepItem(
                    title = stringResource(R.string.onboarding_step3_title),
                    body = stringResource(R.string.onboarding_step3_body)
                )
            }
        }
    }
}

@Composable
private fun StepItem(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = ChirpTheme.typography.labelLarge,
            color = ChirpTheme.colors.secondary
        )
        Text(
            text = body,
            style = ChirpTheme.typography.bodySmall,
            color = ChirpTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun PageGoodToKnow() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = ChirpTheme.spacing.sm)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(ChirpTheme.sizes.iconXl * 2)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            GoodToKnowIllustration(modifier = Modifier.fillMaxSize())
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_page3_title),
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )

                BulletItem(text = stringResource(R.string.onboarding_bullet1))
                BulletItem(text = stringResource(R.string.onboarding_bullet2))
                BulletItem(text = stringResource(R.string.onboarding_bullet3))
                BulletItem(text = stringResource(R.string.onboarding_bullet4))
            }
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = ChirpTheme.typography.bodyMedium,
            color = ChirpTheme.colors.primary
        )
        Text(
            text = text,
            style = ChirpTheme.typography.bodyMedium,
            color = ChirpTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun PageAllowMicrophone(
    micState: MicPermissionState,
    onRequestPermission: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = ChirpTheme.spacing.sm)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(ChirpTheme.sizes.iconXl * 2)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AllowMicrophoneIllustration(modifier = Modifier.fillMaxSize())
        }

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_page4_title),
                        style = ChirpTheme.typography.titleLarge,
                        color = ChirpTheme.colors.primary
                    )
                    if (micState == MicPermissionState.Granted) {
                        StatusPill(status = app.shoutsend.core.designsystem.components.AcousticStatus.SUCCESS, customLabel = "GRANTED")
                    }
                }

                Text(
                    text = stringResource(R.string.onboarding_page4_body),
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1.0f, fill = false))

        if (micState == MicPermissionState.Granted) {
            CdButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.onboarding_btn_get_started))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                CdButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_btn_allow_mic))
                }
                CdTextButton(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.onboarding_btn_not_now))
                }
            }
        }
    }
}
