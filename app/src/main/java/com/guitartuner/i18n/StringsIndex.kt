package com.guitartuner.i18n

import com.guitartuner.model.AppLanguage

/**
 * Merges all per-family language bundles into a single lookup table.
 *
 * Each bundle file (StringsRomance, StringsGermanic, …) provides
 * `Map<AppLanguage, Map<StringKey, String>>`.  We flatten them here
 * into `Map<AppLanguage, Map<StringKey, String>>` so Strings.get()
 * does a two-key O(1) lookup.
 */
internal val ALL_TRANSLATIONS: Map<AppLanguage, Map<StringKey, String>> = buildMap {
    putAll(ROMANCE_TRANSLATIONS)
    putAll(GERMANIC_TRANSLATIONS)
    putAll(ASIAN_TRANSLATIONS)
    putAll(CYRILLIC_TRANSLATIONS)
    putAll(OTHER_TRANSLATIONS)
}
