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
        maven { url "https://nexus.coppernic.fr/repository/libs-release" }
    }
}
```
Documentation
-------------

The javadoc for CpcAsk can be found [here](https://nexus.coppernic.fr/repository/libs-release-coppernic/fr/coppernic/sdk/ask/CpcAsk/4.0.2/CpcAsk-4.0.2-javadoc.jar).

The basics
----------
### Power management

#### Libraries
CpcCore is the library responsible for power management.

In your build.gradle file, at module level, add the following lines:

```groovy
implementation 'fr.coppernic.sdk.core:CpcCore:2.1.12'
```
#### Power on/off RFID reader

First, implement PowerNotifier:

``` groovy
public class MainActivity extends AppCompatActivity implements PowerListener

...

@Override
public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
	// reader instantiation
	Reader.getInstance(this, this);
}

@Override
public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
	enableUiAfterReaderInstantiation(false);
}

```
Then register it in Power API, in onCreate for example:

```groovy

PowerManager.get().registerListener(this);

```
Use the on/off methods:

```groovy
if (isChecked) {
    if (OsHelper.isAccess()) {
        AccessPeripheral.RFID_ASK_UCM108_GPIO.on(MainActivity.this);
    } else {
        ConePeripheral.RFID_ASK_UCM108_GPIO.on(MainActivity.this);
    }

} else {
    if (OsHelper.isAccess()) {
         AccessPeripheral.RFID_ASK_UCM108_GPIO.off(MainActivity.this);
    } else {
         ConePeripheral.RFID_ASK_UCM108_GPIO.off(MainActivity.this);
    }
}
```

Finally, release it:

```groovy
PowerManager.get().unregisterAll();
PowerManager.get().releaseResources();
```

### Reader initialization
#### Libraries
CpcAsk manages the ASK UCM108 RFID reader:

```groovy
implementation 'fr.coppernic.sdk.ask:CpcAsk:4.0.2'
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
On a C-One and Access-ER ASK, by default baudrate is 115200 bauds:

```groovy
reader.cscOpen(ASK_READER_PORT, 115200, false);
```

### Get firmware version to initialize reader for communication

```groovy
StringBuilder sb = new StringBuilder();
reader.cscVersionCsc(sb);
```
Reader is fully initialized.
