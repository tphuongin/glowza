package com.sgroupmobile.glowza.ui.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sgroupmobile.glowza.common.enums.CameraRatio
import com.sgroupmobile.glowza.common.enums.CameraTimer
import com.sgroupmobile.glowza.databinding.CameraSettingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class CameraSetting: BottomSheetDialogFragment() {
    private var _binding: CameraSettingBinding? = null
    private val viewModel: CameraViewModel by activityViewModels()
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = CameraSettingBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewStates()
        setupListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun setupViewStates(){
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.grid.collect { isGridOn ->
                    binding.switchGrid.isChecked = isGridOn
                }
            }
            launch {
                viewModel.timer.collect { timer ->
                    val buttonId = when(timer){
                        CameraTimer.TIMER_3S.time ->  binding.btnTimer3.id
                        CameraTimer.TIMER_5S.time -> binding.btnTimer5.id
                        CameraTimer.TIMER_9S.time -> binding.btnTimer9.id
                        else -> binding.btnTimerOff.id
                    }
                    binding.toggleTimer.check(buttonId)
                }
            }
            launch {
                viewModel.ratio.collect { value ->
                    val buttonId = when(value){
                        CameraRatio.RATIO_4_3.value -> binding.ratio34.id
                        CameraRatio.RATIO_16_9.value -> binding.ratio916.id
                        else -> binding.ratio11.id
                    }
                    binding.toggleRatio.check(buttonId)
                }
            }
        }
    }

    fun setupListener(){
        binding.switchGrid.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleGrid(isChecked)
        }

        binding.toggleTimer.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked){
                val time = when(checkedId){
                    binding.btnTimer3.id -> CameraTimer.TIMER_3S.time
                    binding.btnTimer5.id -> CameraTimer.TIMER_5S.time
                    binding.btnTimer9.id -> CameraTimer.TIMER_9S.time
                    else -> CameraTimer.TIMER_OFF.time
                }
                viewModel.setTimer(time)
            }
        }

        binding.toggleRatio.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if(isChecked){
                val value = when(checkedId){
                    binding.ratio34.id -> CameraRatio.RATIO_4_3.value
                    binding.ratio916.id -> CameraRatio.RATIO_16_9.value
                    else -> CameraRatio.RATIO_1_1.value
                }
                viewModel.setRatio(value)
            }
        }
    }
}