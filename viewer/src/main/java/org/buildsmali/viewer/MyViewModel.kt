package org.buildsmali.viewer

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.buildsmali.viewer.dex.SmaliPackageData
import org.jf.dexlib2.dexbacked.DexBackedClassDef

class MyViewModel : ViewModel() {
    private val _smaliData = mutableStateOf(SmaliPackageData.newRoot())
    val smaliData: MutableState<SmaliPackageData> get() = _smaliData

    private val _infoText = mutableStateOf("")
    val infoText: MutableState<String> get() = _infoText

//    private val _checkedClasses = HashSet<DexBackedClassDef>()
//    val checkedClasses get() = _checkedClasses

    val checkedClasses = HashSet<DexBackedClassDef>()

    private val _checkedClassesMap = mutableStateMapOf<DexBackedClassDef, Boolean>()
    val checkedClassesMap get() = _checkedClassesMap
}