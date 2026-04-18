package com.guitartuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guitartuner.i18n.StringKey
import com.guitartuner.i18n.Strings
import com.guitartuner.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: TunerState,
    onBack: () -> Unit,
    onTunerModeChanged: (TunerMode) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onCalibrationChanged: (Double) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onInstrumentTypeChanged: (InstrumentType) -> Unit,
    onStringCountChanged: (Int) -> Unit,
    onShowAllLanguagesChanged: (Boolean) -> Unit,
    onToggleFavoriteLanguage: (AppLanguage) -> Unit,
) {
    val lang = state.language

    fun s(key: StringKey) = Strings.get(key, lang)

    val layoutDir = if (state.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = s(StringKey.SETTINGS),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s(StringKey.BACK)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // TUNER MODE
                SettingsSection(title = s(StringKey.TUNER_MODE)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillChip(
                            label = s(StringKey.STROBOSCOPIC),
                            selected = state.tunerMode == TunerMode.STROBOSCOPIC,
                            onClick = { onTunerModeChanged(TunerMode.STROBOSCOPIC) },
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            label = s(StringKey.NEEDLE),
                            selected = state.tunerMode == TunerMode.NEEDLE,
                            onClick = { onTunerModeChanged(TunerMode.NEEDLE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // INSTRUMENT
                SettingsSection(title = s(StringKey.INSTRUMENT)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InstrumentType.entries.forEach { type ->
                            PillChip(
                                label = s(instrumentLabelKey(type)),
                                selected = state.instrumentType == type,
                                onClick = { onInstrumentTypeChanged(type) }
                            )
                        }
                    }
                    if (state.isHighPrecision) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s(StringKey.HIGH_PRECISION),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // STRING COUNT
                SettingsSection(title = s(StringKey.STRING_COUNT)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.instrumentType.stringCountOptions.forEach { count ->
                            PillChip(
                                label = "$count ${s(StringKey.STRINGS)}",
                                selected = state.stringCount == count,
                                onClick = { onStringCountChanged(count) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // THEME
                SettingsSection(title = s(StringKey.THEME)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillChip(
                            label = s(StringKey.AUTO),
                            selected = state.themeMode == ThemeMode.AUTO,
                            onClick = { onThemeModeChanged(ThemeMode.AUTO) },
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            label = s(StringKey.DARK),
                            selected = state.themeMode == ThemeMode.DARK,
                            onClick = { onThemeModeChanged(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            label = s(StringKey.LIGHT),
                            selected = state.themeMode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChanged(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // LANGUAGE
                SettingsSection(title = s(StringKey.LANGUAGE)) {
                    // Show all / Favorites toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.showAllLanguages) s(StringKey.SHOW_ALL_LANGUAGES)
                            else s(StringKey.SHOW_FAVORITES),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Switch(
                            checked = state.showAllLanguages,
                            onCheckedChange = onShowAllLanguagesChanged
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LanguageGrid(
                        languages = state.visibleLanguages,
                        selectedLanguage = state.language,
                        favoriteLanguages = state.favoriteLanguages,
                        showAllLanguages = state.showAllLanguages,
                        onLanguageSelected = onLanguageChanged,
                        onToggleFavorite = onToggleFavoriteLanguage
                    )
                }

                // CALIBRATION
                SettingsSection(title = s(StringKey.CALIBRATION)) {
                    CalibrationControl(
                        calibration = state.a4Calibration,
                        onCalibrationChanged = onCalibrationChanged,
                        lang = lang
                    )
                }

                // VIBRATION
                SettingsSection(title = s(StringKey.VIBRATION)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s(StringKey.VIBRATION_DESC),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Switch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = onVibrationChanged
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun instrumentLabelKey(type: InstrumentType): StringKey = when (type) {
    InstrumentType.GUITARRA -> StringKey.GUITAR
    InstrumentType.BAIXO -> StringKey.BASS
    InstrumentType.VIOLINO -> StringKey.VIOLIN
    InstrumentType.VIOLA -> StringKey.VIOLA
    InstrumentType.VIOLONCELO -> StringKey.CELLO
    InstrumentType.CONTRABAIXO -> StringKey.DOUBLE_BASS
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
    @Suppress("DEPRECATION")
    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
}

@Composable
private fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surface

    val textColor = if (selected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .then(
                if (!selected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    RoundedCornerShape(999.dp)
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun LanguageGrid(
    languages: List<AppLanguage>,
    selectedLanguage: AppLanguage,
    favoriteLanguages: Set<AppLanguage>,
    showAllLanguages: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onToggleFavorite: (AppLanguage) -> Unit
) {
    val rows = languages.chunked(3)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { lang ->
                    val isSelected = lang == selectedLanguage
                    val isFavorite = lang in favoriteLanguages

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .then(
                                if (!isSelected) Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable { onLanguageSelected(lang) }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.displayName}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (showAllLanguages) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isFavorite) "\u2605" else "\u2606",
                                    fontSize = 14.sp,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = if (isFavorite) 1f else 0.3f
                                        ),
                                    modifier = Modifier.clickable { onToggleFavorite(lang) }
                                )
                            }
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalibrationControl(
    calibration: Double,
    onCalibrationChanged: (Double) -> Unit,
    lang: AppLanguage
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { onCalibrationChanged((calibration - 1).coerceIn(420.0, 460.0)) },
                modifier = Modifier.size(40.dp)
            ) {
                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "${calibration.toInt()} Hz",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(16.dp))

            FilledIconButton(
                onClick = { onCalibrationChanged((calibration + 1).coerceIn(420.0, 460.0)) },
                modifier = Modifier.size(40.dp)
            ) {
                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = Strings.get(StringKey.CALIBRATION_RANGE, lang),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
