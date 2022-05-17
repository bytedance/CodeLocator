package com.bytedance.tools.codelocator.tinypng.dialog

import com.bytedance.tools.codelocator.utils.Mob
import com.bytedance.tools.codelocator.utils.ResUtils
import java.awt.event.ActionEvent

class CancelActionListener(dialog: TinyImageDialog) : ActionListenerBase(dialog) {

    override fun actionPerformed(e: ActionEvent) {
        val isInProgress = dialog.compressInProgress
        if (!isInProgress) {
            dialog.dispose()
            if (dialog.mIsAutoPopup) {
                Mob.mob(Mob.Action.CLICK, "tiny_auto_pop_cancel")
            } else {
                Mob.mob(Mob.Action.CLICK, "tiny_cancel")
            }
        } else {
            Mob.mob(Mob.Action.CLICK, "tiny_cancel_process")
        }
        dialog.compressInProgress = false
        dialog.btnCancel.text = ResUtils.getString("cancel")
    }

}