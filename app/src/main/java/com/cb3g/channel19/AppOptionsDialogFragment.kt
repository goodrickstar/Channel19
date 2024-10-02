package com.cb3g.channel19

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.android.multidex.myapplication.databinding.AppOptionsDialogFragmentBinding

class AppOptionsDialogFragment : DialogFragment() {
    lateinit var binding: AppOptionsDialogFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AppOptionsDialogFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val options = RadioService.appOptions
        val admin = RadioService.operator.admin
        binding.switchBlocking.isChecked = options.blocking
        binding.switchFlagging.isChecked = options.flagging
        binding.switchSilencing.isChecked = options.silencing
        binding.switchGhostMode.isChecked = options.ghost_mode
        binding.switchRadioShop.isChecked = options.radio_shop
        binding.switchChannelLock.isChecked = options.private_channels
        binding.switchBlocking.isEnabled = admin
        binding.switchFlagging.isEnabled = admin
        binding.switchSilencing.isEnabled = admin
        binding.switchGhostMode.isEnabled = admin
        binding.switchRadioShop.isEnabled = admin
        binding.switchChannelLock.isEnabled = admin
        binding.close.setOnClickListener {
            Utils.vibrate(it)
            if (admin){
                Utils.getDatabase().reference.child("appOptions").setValue(AppOptionsObject(
                    binding.switchBlocking.isChecked,
                    binding.switchGhostMode.isChecked,
                    binding.switchFlagging.isChecked,
                    binding.switchRadioShop.isChecked,
                    binding.switchSilencing.isChecked,
                    binding.switchChannelLock.isChecked))
            }
            dismiss()
        }

    }

}