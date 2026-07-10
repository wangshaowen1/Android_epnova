package com.kuaigeglobal.epnova.feature.creation.childs

import android.os.Bundle
import com.kuaigeglobal.epnova.base.viewmodel.EmptyViewModel
import com.kuaigeglobal.epnova.databinding.FragmentCreationNovelBinding
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.MODEL_CUSTOM
import com.nhsd.basic.BaseViewModelFragment

/** 
 * @createtime 2026年07月10日 16:11
 * @author TooWhiteT
 */

class CreationNovelFragment : BaseViewModelFragment<FragmentCreationNovelBinding, EmptyViewModel>() {
    companion object {
        private const val GENERATING_PANEL_COLLAPSED_HEIGHT_DP = 84
        private const val GENERATING_PANEL_GAP_DP = 8

        fun newInstance(): CreationNovelFragment {
            return CreationNovelFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_MODEL, MODEL_CUSTOM)
                }
            }
        }
    }
}