package fr.coppernic.samples.ask;

import android.content.Context;

import fr.coppernic.sdk.powermgmt.PowerMgmt;
import fr.coppernic.sdk.powermgmt.PowerMgmtFactory;
import fr.coppernic.sdk.powermgmt.PowerUtilsNotifier;
import fr.coppernic.sdk.powermgmt.cone.identifiers.InterfacesCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ManufacturersCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.ModelsCone;
import fr.coppernic.sdk.powermgmt.cone.identifiers.PeripheralTypesCone;

/**
 * Created by benoist on 23/06/17.
 */

public class Power {
    private PowerMgmt powerMgmt;

    public Power (Context context, PowerUtilsNotifier notifier) {
        powerMgmt = PowerMgmtFactory.get().setContext(context)
                .setNotifier(notifier)
                .setPeripheralTypes(PeripheralTypesCone.RfidSc)
                .setManufacturers(ManufacturersCone.Ask)
                .setModels(ModelsCone.Ucm108)
                .setInterfaces(InterfacesCone.ExpansionPort)
                .build();
    }

    public void rfid (boolean on) {
        if (on) {
            powerMgmt.powerOn();
        } else {
            powerMgmt.powerOff();
        }
    }
}
