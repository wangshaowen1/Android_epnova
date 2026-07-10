package com.kuaigeglobal.epnova.feature.creation.childs

import android.os.Bundle
import com.kuaigeglobal.epnova.base.viewmodel.EmptyViewModel
import com.kuaigeglobal.epnova.databinding.FragmentCreationInspirationBinding
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.KEY_MODEL
import com.kuaigeglobal.epnova.feature.creation.CreationFragment.Companion.MODEL_INSPIRATION
import com.nhsd.basic.BaseViewModelFragment

/** 
 * @createtime 2026年07月10日 16:11
 * @author TooWhiteT
 */

class CreationInspirationFragment : BaseViewModelFragment<FragmentCreationInspirationBinding, EmptyViewModel>() {
    companion object {
        private const val RANDOM_INSPIRATION_ARROW_ROTATION_DURATION = 200L

        fun newInstance(): CreationInspirationFragment {
            return CreationInspirationFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_MODEL, MODEL_INSPIRATION)
                }
            }
        }
    }
}