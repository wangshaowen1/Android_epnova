package com.kuaigeglobal.epnova.feature.rating

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.RatingPopupDisplayReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.RatingScoreReportBody
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
    val submittedRating = MutableLiveData<Int?>()

    private val popupLocationCode by lazy { mBundle.getInt(Constants.Extra.TYPE) }
    private var displayReportStarted = false
    private var submitting = false

    fun reportDisplay() {
        if (displayReportStarted || popupLocationCode !in 1..2) return
        displayReportStarted = true
        // 展示即消费当前资格，避免两个入口重复弹出；后续资格由用户信息刷新。
        User.get().setRatingPopupDisable()
        requestWithLoading(
            apiBlock = {
                mRepo.api.reportRatingPopupDisplay(RatingPopupDisplayReportBody(popupLocationCode))
            },
            showLoading = false,
            onSuccess = {}
        )
    }

    fun submitRating() {
        val score = rating.get().toInt()
        if (submitting || score !in 1..5 || popupLocationCode !in 1..2) return
        submitting = true
        requestWithLoading(
            apiBlock = {
                mRepo.api.reportRatingScore(
                    RatingScoreReportBody(
                        ratingScore = score.toDouble(),
                        popupLocationCode = popupLocationCode
                    )
                )
            },
            onError = { submitting = false },
            onSuccess = {
                submitting = false
                User.get().setRatingPopupDisable()
                submittedRating.value = score
            }
        )
    }

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
