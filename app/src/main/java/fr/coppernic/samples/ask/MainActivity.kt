package fr.coppernic.samples.ask

import androidx.appcompat.app.AppCompatActivity
import fr.coppernic.sdk.power.api.PowerListener
import fr.coppernic.sdk.utils.io.InstanceListener
import androidx.appcompat.widget.SwitchCompat
import android.widget.TextView
import android.os.Bundle
import android.view.View
import android.widget.Button
import fr.coppernic.samples.ask.R
import android.widget.CompoundButton
import fr.coppernic.sdk.utils.helpers.OsHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import fr.coppernic.sdk.power.impl.access.AccessPeripheral
import fr.coppernic.sdk.power.impl.cone.ConePeripheral
import fr.coppernic.sdk.utils.sound.Sound
import com.google.android.material.snackbar.Snackbar
import fr.coppernic.sdk.ask.sCARD_SearchExt
import fr.coppernic.sdk.ask.SearchParameters
import fr.coppernic.sdk.ask.ReaderListener
import fr.coppernic.sdk.ask.RfidTag
import fr.coppernic.sdk.utils.core.CpcBytes
import android.widget.RadioGroup
import androidx.appcompat.widget.Toolbar
import fr.coppernic.sdk.ask.Reader
import fr.coppernic.sdk.core.Defines
import fr.coppernic.sdk.hdk.access.GpioPort
import fr.coppernic.sdk.power.PowerManager
import fr.coppernic.sdk.utils.core.CpcResult.RESULT
import fr.coppernic.sdk.power.api.peripheral.Peripheral
import io.reactivex.functions.Consumer
import java.lang.StringBuilder

class MainActivity : AppCompatActivity(), PowerListener, InstanceListener<Reader?> {
    // RFID reader
    private var reader: Reader? = null

    // UI
    private var swOpen: SwitchCompat? = null
    private var btnFwVersion: Button? = null
    private var swCardDetection:SwitchCompat? = null
    private var tvCommunicationModeValue: TextView? = null
    private var tvAtrValue: TextView? = null
    private var btnGetSamAtr: Button? = null
    private var swPower: SwitchCompat? = null

    // GpioPort
    private var gpioPort: GpioPort? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        swOpen = findViewById(R.id.swOpen)
        btnFwVersion = findViewById(R.id.btnFwVersion)
        swCardDetection = findViewById(R.id.swCardDetection)
        tvCommunicationModeValue = findViewById(R.id.tvCommunicationModeValue)
        tvAtrValue = findViewById(R.id.tvAtrValue)
        btnGetSamAtr = findViewById(R.id.btnSamGetAtr)
        swPower = findViewById(R.id.swPower)
        swOpen?.setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            onSwOpenCheckedChanged(compoundButton, b)
        }
        btnFwVersion?.setOnClickListener { view: View? -> onBtnFwVersionClick(view) }
        swCardDetection?.setOnCheckedChangeListener { compoundButton: CompoundButton?, b: Boolean ->
            onSwCardDetectionCheckedChanged(compoundButton, b) }
        btnGetSamAtr?.setOnClickListener { view: View? -> onBtnSamGetAtrClick(view) }
        swPower?.setOnCheckedChangeListener{ compoundButton: CompoundButton?, b: Boolean ->
            onSwPowerCheckedChanged(compoundButton, b) }
        initPowerManagement()
    }

    private fun initPowerManagement() {
        PowerManager.get().registerListener(this)
        if (OsHelper.isAccess()) {
            val d = GpioPort.GpioManager.get()
                .getGpioSingle(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ g: GpioPort? -> gpioPort = g }, onError)
        }
    }

    private val onError = Consumer { _: Throwable? -> Timber.e("Service not found") }
    override fun onDestroy() {
        super.onDestroy()
        // Releases PowerManager
        PowerManager.get().unregisterAll()
        PowerManager.get().releaseResources()
    }

    // InstanceListener implementation
    override fun onCreated(reader: Reader?) {
        this.reader = reader
        enableUiAfterReaderInstantiation(true)
    }

    override fun onDisposed(reader: Reader?) {}
    private fun callback() {
        //do something here
        return Unit
    }

    // End of InstanceListener implementation
    private fun onSwPowerCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            if (OsHelper.isAccess()) {
                AccessPeripheral.RFID_ASK_UCM108_GPIO.on(this@MainActivity)
            } else {
                ConePeripheral.RFID_ASK_UCM108_GPIO.on(this@MainActivity)
            }
        } else {
            if (OsHelper.isAccess()) {
                AccessPeripheral.RFID_ASK_UCM108_GPIO.off(this@MainActivity)
            } else {
                ConePeripheral.RFID_ASK_UCM108_GPIO.off(this@MainActivity)
            }
        }
    }

    private fun beepFunction() {
        val sound = Sound(this)
        sound.playOk(250) { callback() }
    }

    private fun onSwOpenCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            // Opens communication port
            var res = -1
            res = reader!!.cscOpen(Defines.SerialDefines.ASK_READER_PORT, 115200, false)
            res = if (res == fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
                reader!!.cscResetCsc()
            } else {
                Snackbar.make(buttonView!!, "Error opening reader", Snackbar.LENGTH_SHORT).show()
                return
            }
            if (res == fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
                enableUiAfterOpen(true)
            } else {
                Snackbar.make(buttonView!!, "Error resetting reader", Snackbar.LENGTH_SHORT).show()
            }
        } else {
            // CLoses communication port
            reader!!.cscClose()
            enableUiAfterOpen(false)
        }
    }

    private fun onBtnFwVersionClick(v: View?) {
        // Gets firmware version of the reader
        // And initialize it for communication
        val sb = StringBuilder()
        val res = reader!!.cscVersionCsc(sb)
        if (res == fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
            Timber.d("Version : \"%s\"", sb)
            Snackbar.make(v!!, sb.toString(), Snackbar.LENGTH_SHORT).show()
            enableUiAfterFullInit(true)
        }
    }

    private fun launchCardDiscovery(buttonView: CompoundButton?) {
        // Sets the card detection
        val search = sCARD_SearchExt()
        search.OTH = 1
        search.CONT = 0
        search.INNO = 1
        search.ISOA = 1
        search.ISOB = 1
        search.MIFARE = 1
        search.MONO = 1
        search.MV4k = 1
        search.MV5k = 1
        search.TICK = 1
        val mask = fr.coppernic.sdk.ask.Defines.SEARCH_MASK_INNO or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_ISOA or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_ISOB or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_MIFARE or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_MONO or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_MV4K or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_MV5K or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_TICK or fr.coppernic.sdk.ask.Defines.SEARCH_MASK_OTH
        val parameters = SearchParameters(search, mask, 0x01.toByte(), 0x00.toByte())
        // Starts card detection
        reader!!.startDiscovery(parameters, object : ReaderListener {
            override fun onTagDiscovered(rfidTag: RfidTag) {
                // Displays Tag data
                showTag(rfidTag)
            }

            override fun onDiscoveryStopped() {
                Snackbar.make(buttonView!!, R.string.card_detection_stopped, Snackbar.LENGTH_SHORT).show()
                runOnUiThread {
                    if (swCardDetection!!.isChecked) {
                        // TODO : check if a delay should be inserted, also verify that application should
                        //  implement a continuous discovery
                        launchCardDiscovery(buttonView)
                    }
                }
            }
        })
    }

    private fun onSwCardDetectionCheckedChanged(buttonView: CompoundButton?, checked: Boolean) {
        if (checked) {
            // Clears Tag data
            showTag(null)

            // Sets the card detection
            launchCardDiscovery(buttonView)
            Snackbar.make(buttonView!!, R.string.card_detection_started, Snackbar.LENGTH_SHORT).show()
        } else {
            // Stops card detection
            reader!!.stopDiscovery()
        }
    }

    private fun onBtnSamGetAtrClick(v: View?) {
        // Clears ATR
        showSamAtr(null, 0)
        // Gets SAM to be resetted
        val sam = sam
        // Gets select protocol
        val protocol = protocol
        // Selects SAM
        var res = reader!!.cscSelectSam(sam, protocol)
        if (res != fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
            // Shows error
            Snackbar.make(v!!, R.string.error_selecting_sam, Snackbar.LENGTH_SHORT).show()
            return
        }
        // Resets SAM
        val atr = ByteArray(256)
        val atrLength = IntArray(1)
        res = reader!!.cscResetSam(0x00.toByte(), atr, atrLength)
        if (res != fr.coppernic.sdk.ask.Defines.RCSC_Ok) {
            // Shows error
            Snackbar.make(v!!, R.string.error_resetting_sam, Snackbar.LENGTH_SHORT).show()
            return
        }
        // Displays ATR
        showSamAtr(atr, atrLength[0])
    }

    /**
     * Displays tag data
     * @param tag Tag to be displayed
     */
    private fun showTag(tag: RfidTag?) {
        runOnUiThread {
            if (tag != null) {
                beepFunction()
                tvCommunicationModeValue!!.text = tag.communicationMode.toString()
                tvAtrValue!!.text = CpcBytes.byteArrayToString(tag.atr)
            } else {
                tvCommunicationModeValue!!.text = ""
                tvAtrValue!!.text = ""
            }
        }
    }

    /**
     * Enables/disables UI after power state of RFID reader has been changed.
     * @param enable true: enables, false: disables
     */
    private fun enableUiAfterReaderInstantiation(enable: Boolean) {
        runOnUiThread {
            swOpen!!.isEnabled = enable
            if (!enable) {
                swOpen!!.isChecked = false
                btnFwVersion!!.isEnabled = false
            }
        }
    }

    /**
     * Enables/disables UI after the reader has been opened/closed
     * @param enable true: enables, false: disables
     */
    private fun enableUiAfterOpen(enable: Boolean) {
        runOnUiThread {
            btnFwVersion!!.isEnabled = enable
            if (!enable) {
                if (swCardDetection!!.isChecked) {
                    reader!!.stopDiscovery()
                    swCardDetection!!.isChecked = false
                }
                swCardDetection!!.isEnabled = false
                btnGetSamAtr!!.isEnabled = false
            }
        }
    }

    /**
     * Enables/disables UI after the reader has been fully initialized (after firmware version has been retrieved)
     * @param enable true: enables, false: disables
     */
    private fun enableUiAfterFullInit(enable: Boolean) {
        runOnUiThread {
            swCardDetection!!.isEnabled = enable
            btnGetSamAtr!!.isEnabled = enable
        }
    }

    /**
     * Returns SAM selected by user
     * @return SAM number (1 for SAM 1, 2 for SAM 2
     */
    private val sam: Byte
        get() {
            val rgSam = findViewById<RadioGroup>(R.id.rgSam)
            return if (rgSam.checkedRadioButtonId == R.id.rbSam1) {
                0x01
            } else {
                0x02
            }
        }

    /**
     * Returns protocol selected by user
     * @return Protocol
     */
    private val protocol: Byte
        get() {
            val rgProtocol = findViewById<RadioGroup>(R.id.rgProtocol)
            return when (rgProtocol.checkedRadioButtonId) {
                R.id.rbIso7816T0 -> {
                    fr.coppernic.sdk.ask.Defines.SAM_PROT_ISO_7816_T0
                }
                R.id.rbIso7816T1 -> {
                    fr.coppernic.sdk.ask.Defines.SAM_PROT_ISO_7816_T1
                }
                else -> {
                    fr.coppernic.sdk.ask.Defines.SAM_PROT_HSP_INNOVATRON
                }
            }
        }

    /**
     * Displays SAM ATR
     * @param atr ATR
     * @param length ATR length
     */
    private fun showSamAtr(atr: ByteArray?, length: Int) {
        val tvSamAtrValue = findViewById<TextView>(R.id.tvSamAtrValue)
        if (atr == null) {
            tvSamAtrValue.text = ""
        } else {
            tvSamAtrValue.text = CpcBytes.byteArrayToString(atr, length)
        }
    }

    override fun onPowerUp(result: RESULT, peripheral: Peripheral) {
        // reader instantiation
        Reader.getInstance(this, this)
    }

    override fun onPowerDown(result: RESULT, peripheral: Peripheral) {
        enableUiAfterReaderInstantiation(false)
    }
}