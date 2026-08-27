package com.kuaigeglobal.epnova.feature.rating

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.nhsd.basic.ext.getResString
import kotlin.math.ceil

/** 
 * @createtime 2026年08月27日 17:29
 * @author TooWhiteT
 */

class AppRatingViewModel : AppRepositoryViewModel<AppRepository>() {

    val rating = ObservableFloat(0f)
    val title = ObservableField(R.string.app_rating_title_unrated.getResString())
    val subtitle = ObservableField(R.string.app_rating_subtitle_unrated.getResString())
    val actionText = ObservableField(R.string.app_rating_action_continue.getResString())
    val actionEnabled = ObservableBoolean(false)

    fun updateRating(rawRating: Float): Float {
        val normalizedRating = ceil(rawRating.toDouble()).toInt().coerceIn(0, 5)
        val normalizedRatingValue = normalizedRating.toFloat()
        rating.set(normalizedRatingValue)

        val isRated = normalizedRating > 0
        title.set(
            (if (isRated) {
                R.string.app_rating_title_rated
            } else {
                R.string.app_rating_title_unrated
            }).getResString()
        )
        subtitle.set(
            when (normalizedRating) {
                in 1..3 -> R.string.app_rating_subtitle_low
                in 4..5 -> R.string.app_rating_subtitle_high
                else -> R.string.app_rating_subtitle_unrated
            }.getResString()
        )
        actionText.set(
            when (normalizedRating) {
                in 1..3 -> R.string.app_rating_action_done
                in 4..5 -> R.string.app_rating_action_rate_now
                else -> R.string.app_rating_action_continue
            }.getResString()
        )
        actionEnabled.set(isRated)
        return normalizedRatingValue
    }
}
