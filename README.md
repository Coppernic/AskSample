# AskSample
Sample Application for ASK UCM108 RFID reader

Introduction
------------
This sample application illustrates how to use the ASK UCM108 RFID reader on Coppernic devices.

The libraries
-------------

Coppernic uses a Maven repository to provide libraries.

In the build.gradle, at project level, add the following lines:

```groovy
allprojects {
    repositories {                
        maven { url 'https://artifactory.coppernic.fr/artifactory/libs-release'}
    }
}
```
Documentation
-------------

The javadoc for CpcAsk can be found [here](https://github.com/Coppernic/coppernic.github.io/raw/master/assets/CpcAsk-3.0.0-javadoc.jar).

The basics
----------
### Power management

#### Libraries
CpcCore is the library responsible for power management.

In your build.gradle file, at module level, add the following lines:

```groovy
compile 'fr.coppernic.sdk.core:CpcCore:1.0.0'
```
#### Power on/off RFID reader

First, create a Power management object:

``` groovy
private PowerMgmt powerMgmt;
```
Then instantiate it:

```groovy
powerMgmt = PowerMgmtFactory.get().setContext(context)
                .setNotifier(notifier)
                .setPeripheralTypes(PeripheralTypesCone.RfidSc)
                .setManufacturers(ManufacturersCone.Ask)
                .setModels(ModelsCone.Ucm108)
                .setInterfaces(InterfacesCone.ExpansionPort)
                .build();
```
Finally, use the powerOn/powerOff methods:

```groovy
public void rfid (boolean on) {
    if (on) {
        powerMgmt.powerOn();
    } else {
        powerMgmt.powerOff();
    }
}
```

### Reader initialization
#### Libraries
CpcAsk manages the ASK UCM108 RFID reader:

```groovy
compile 'fr.coppernic.sdk.ask:CpcAsk:3.0.0'
```

#### Create reader object
First declare a Reader object:

```groovy
private Reader reader;
```
Then instantiate it:

```groovy
Reader.getInstance(this, this);
```

Where your activity implements InstanceListener<Reader>:

```groovy
@Override
public void onCreated(Reader reader) {
    this.reader = reader;    
}

@Override
public void onDisposed(Reader reader) {

}
```

### Open reader
On a C-One, by default baudrate is 115200 bauds:

```groovy
reader.cscOpen(CpcDefinitions.ASK_READER_PORT, 115200, false);
```
CpcDefinitions is part of Coppernic Utility Library, you can add it to your build.gradle:

```groovy
compile 'fr.coppernic.sdk.cpcutils:CpcUtilsLib:6.7.0'
```

### Initialize reader

Once the open has been performed, a reset is needed to insure that the reader has been completeley initialized:

```groovy
reader.cscResetCsc();
```

### Get firmware version to initialize reader for communication

```groovy
StringBuilder sb = new StringBuilder();
reader.cscVersionCsc(sb);
```
Reader is fully initialized.
