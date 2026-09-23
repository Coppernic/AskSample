package fr.coppernic.samples.ask

import android.content.Context
import fr.coppernic.sdk.power.OutletPowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

fun OutletPowerManager.powerFuture(
    context: Context,
    on: Boolean
): CompletableFuture<Boolean> {
    return CoroutineScope(Dispatchers.IO).future {
        if (on) {
            if (!isPowerOn(context)) {
                powerOn(context)
            } else true
        } else {
            powerOff(context)
        }
    }
}