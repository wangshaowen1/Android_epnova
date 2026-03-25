package com.kuaigeglobal.epnova.core.ui

import androidx.fragment.app.FragmentManager

interface FragmentHost {

    fun getHostContainerId(): Int
    fun getHostFragmentManager(): FragmentManager?

}