package fr.coppernic.samples.ask;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.ask.ReaderListener;
import fr.coppernic.sdk.ask.RfidTag;
import fr.coppernic.sdk.ask.SearchParameters;
import fr.coppernic.sdk.ask.sCARD_SearchExt;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.access.AccessPeripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.helpers.OsHelper;
import fr.coppernic.sdk.utils.io.InstanceListener;
import fr.coppernic.sdk.utils.sound.Sound;
import kotlin.Unit;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements PowerListener, InstanceListener<Reader> {
    // RFID reader
    private Reader reader;
    // UI
    SwitchCompat swOpen;
    Button btnFwVersion;
    SwitchCompat swCardDetection;
    TextView tvCommunicationModeValue;
    TextView tvAtrValue;
    Button btnGetSamAtr;
    SwitchCompat swPower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        swOpen = findViewById(R.id.swOpen);
        btnFwVersion= findViewById(R.id.btnFwVersion);
        swCardDetection = findViewById(R.id.swCardDetection);
        tvCommunicationModeValue = findViewById(R.id.tvCommunicationModeValue);
        tvAtrValue = findViewById(R.id.tvAtrValue);
        btnGetSamAtr = findViewById(R.id.btnSamGetAtr);
        swPower = findViewById(R.id.swPower);

        swOpen.setOnCheckedChangeListener(this::onSwOpenCheckedChanged);

        btnFwVersion.setOnClickListener(this::onBtnFwVersionClick);

        swCardDetection.setOnCheckedChangeListener(
            this::onSwCardDetectionCheckedChanged);

        btnGetSamAtr.setOnClickListener(this::onBtnSamGetAtrClick);

        swPower.setOnCheckedChangeListener(this::onSwPowerCheckedChanged);

        initPowerManagement();
    }

    private void initPowerManagement() {

        PowerManager.get().registerListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Releases PowerManager
        PowerManager.get().unregisterAll();
        PowerManager.get().releaseResources();
    }

    // InstanceListener implementation

    @Override
    public void onCreated(Reader reader) {
        this.reader = reader;
        enableUiAfterReaderInstantiation(true);
    }

    @Override
    public void onDisposed(Reader reader) {

    }

    private Unit callback() {
        //do something here

        return Unit.INSTANCE;
    }

    // End of InstanceListener implementation
    public void onSwPowerCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
    }

    private void beepFunction() {
        Sound sound = new Sound(this);
        sound.playOk(250, this::callback);
    }

    public void onSwOpenCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            // Opens communication port
            int res;

            res = reader.cscOpen(fr.coppernic.sdk.core.Defines.SerialDefines.ASK_READER_PORT, 115200, false);

            if (res == Defines.RCSC_Ok) {

                res = reader.cscResetCsc();
            } else {
                Snackbar.make(buttonView, "Error opening reader", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if (res == Defines.RCSC_Ok) {
                enableUiAfterOpen(true);
            } else {
                Snackbar.make(buttonView, "Error resetting reader", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // CLoses communication port
            reader.cscClose();
            enableUiAfterOpen(false);
        }
    }

    public void onBtnFwVersionClick(View v) {
        // Gets firmware version of the reader
        // And initialize it for communication
        StringBuilder sb = new StringBuilder();
        int res = reader.cscVersionCsc(sb);
        if (res == Defines.RCSC_Ok) {
            Timber.d("Version : \"%s\"", sb);
            Snackbar.make(v, sb.toString(), Snackbar.LENGTH_SHORT).show();
            enableUiAfterFullInit(true);
        }
    }

    private void launchCardDiscovery(final CompoundButton buttonView) {
        // Sets the card detection
        sCARD_SearchExt search = new sCARD_SearchExt();
        search.OTH = 1;
        search.CONT = 0;
        search.INNO = 1;
        search.ISOA = 1;
        search.ISOB = 1;
        search.MIFARE = 1;
        search.MONO = 1;
        search.MV4k = 1;
        search.MV5k = 1;
        search.TICK = 1;
        int mask = Defines.SEARCH_MASK_INNO | Defines.SEARCH_MASK_ISOA | Defines.SEARCH_MASK_ISOB | Defines.SEARCH_MASK_MIFARE | Defines.SEARCH_MASK_MONO | Defines.SEARCH_MASK_MV4K | Defines.SEARCH_MASK_MV5K | Defines.SEARCH_MASK_TICK | Defines.SEARCH_MASK_OTH;
        SearchParameters parameters = new SearchParameters(search, mask, (byte) 0x01, (byte) 0x00);
        // Starts card detection
        reader.startDiscovery(parameters, new ReaderListener() {
            @Override
            public void onTagDiscovered(RfidTag rfidTag) {
                // Displays Tag data
                showTag(rfidTag);
            }

            @Override
            public void onDiscoveryStopped() {
                Snackbar.make(buttonView, R.string.card_detection_stopped, Snackbar.LENGTH_SHORT).show();
                runOnUiThread(() -> {
                    if (swCardDetection.isChecked()) {
                        // TODO : check if a delay should be inserted, also verify that application should
                        //  implement a continuous discovery
                        launchCardDiscovery(buttonView);
                    }
                });
            }
        });


    }

    public void onSwCardDetectionCheckedChanged(final CompoundButton buttonView, boolean checked) {
        if (checked) {
            // Clears Tag data
            showTag(null);

            // Sets the card detection
            launchCardDiscovery(buttonView);
            Snackbar.make(buttonView, R.string.card_detection_started, Snackbar.LENGTH_SHORT).show();
        } else {
            // Stops card detection
            reader.stopDiscovery();
        }
    }

    public void onBtnSamGetAtrClick(View v) {
        // Clears ATR
        showSamAtr(null, 0);
        // Gets SAM to be resetted
        byte sam = getSam();
        // Gets select protocol
        byte protocol = getProtocol();
        // Selects SAM
        int res = reader.cscSelectSam(sam, protocol);
        if (res != Defines.RCSC_Ok) {
            // Shows error
            Snackbar.make(v, R.string.error_selecting_sam, Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Resets SAM
        byte[] atr = new byte[256];
        int[] atrLength = new int[1];
        res = reader.cscResetSam((byte) 0x00, atr, atrLength);
        if (res != Defines.RCSC_Ok) {
            // Shows error
            Snackbar.make(v, R.string.error_resetting_sam, Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Displays ATR
        showSamAtr(atr, atrLength[0]);
    }

    /**
     * Displays tag data
     * @param tag Tag to be displayed
     */
    private void showTag(final RfidTag tag) {
        runOnUiThread(() -> {
            if (tag != null) {
                beepFunction();
                tvCommunicationModeValue.setText(tag.getCommunicationMode().toString());
                tvAtrValue.setText(CpcBytes.byteArrayToString(tag.getAtr()));
            } else {
                tvCommunicationModeValue.setText("");
                tvAtrValue.setText("");
            }
        });
    }

    /**
     * Enables/disables UI after power state of RFID reader has been changed.
     * @param enable true: enables, false: disables
     */
    private void enableUiAfterReaderInstantiation(final boolean enable) {
        runOnUiThread(() -> {
            swOpen.setEnabled(enable);

            if (!enable) {
                swOpen.setChecked(false);
                btnFwVersion.setEnabled(false);
            }
        });
    }

    /**
     * Enables/disables UI after the reader has been opened/closed
     * @param enable true: enables, false: disables
     */
    private void enableUiAfterOpen(final boolean enable) {
        runOnUiThread(() -> {
            btnFwVersion.setEnabled(enable);

            if (!enable) {
                if (swCardDetection.isChecked()) {
                    reader.stopDiscovery();
                    swCardDetection.setChecked(false);
                }
                swCardDetection.setEnabled(false);
                btnGetSamAtr.setEnabled(false);
            }
        });
    }

    /**
     * Enables/disables UI after the reader has been fully initialized (after firmware version has been retrieved)
     * @param enable true: enables, false: disables
     */
    private void enableUiAfterFullInit (final boolean enable) {
        runOnUiThread(() -> {
            swCardDetection.setEnabled(enable);
            btnGetSamAtr.setEnabled(enable);
        });
    }

    /**
     * Returns SAM selected by user
     * @return SAM number (1 for SAM 1, 2 for SAM 2
     */
    private byte getSam() {
        RadioGroup rgSam = findViewById(R.id.rgSam);
        if (rgSam.getCheckedRadioButtonId() == R.id.rbSam1) {
            return 0x01;
        } else {
            return 0x02;
        }
    }

    /**
     * Returns protocol selected by user
     * @return Protocol
     */
    private byte getProtocol() {
        RadioGroup rgProtocol = findViewById(R.id.rgProtocol);
        if (rgProtocol.getCheckedRadioButtonId() == R.id.rbIso7816T0) {
            return Defines.SAM_PROT_ISO_7816_T0;
        } else if (rgProtocol.getCheckedRadioButtonId() == R.id.rbIso7816T1) {
            return Defines.SAM_PROT_ISO_7816_T1;
        } else {
            return Defines.SAM_PROT_HSP_INNOVATRON;
        }
    }

    /**
     * Displays SAM ATR
     * @param atr ATR
     * @param length ATR length
     */
    private void showSamAtr(byte[] atr, int length) {
        TextView tvSamAtrValue = findViewById(R.id.tvSamAtrValue);

        if (atr == null) {
            tvSamAtrValue.setText("");
        } else {
            tvSamAtrValue.setText(CpcBytes.byteArrayToString(atr, length));
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        if (swOpen.isChecked()) {
            swOpen.setChecked(false);
        }

        if (swPower.isChecked()) {
            swPower.setChecked(false);
        }

    }

    @Override
    public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
        // reader instantiation
        Reader.getInstance(this, this);
    }

    @Override
    public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
        enableUiAfterReaderInstantiation(false);
    }
}
