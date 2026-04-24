package com.skinsshowcase.trades.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Фильтр по «специальному» типу предмета: по наличию в названии StatTrak™, Souvenir или обычные (без них).
 */
@Schema(description = "Special: STATTRACK — в названии есть StatTrak™, SOUVENIR — сувенирные, NORMAL — обычные (без StatTrak и Souvenir)")
public enum SpecialFilter {

    /** В названии есть StatTrak™ (или StatTrak). */
    STATTRACK,

    /** В названии есть Souvenir. */
    SOUVENIR,

    /** Обычные предметы — без StatTrak и без Souvenir. */
    NORMAL
}
