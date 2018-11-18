package com.rmkrishna.ibeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;

import java.util.List;

/**
 * <p>
 * Packet Structure Byte Map
 * Byte 0-2: Standard BLE Flags
 * <p>
 * Byte 0: Length :  0x02
 * Byte 1: Type: 0x01 (Flags)
 * Byte 2: Value: 0x06 (Typical Flags)
 * Byte 3-29: Apple Defined iBeacon Data
 * <p>
 * Byte 3: Length: 0x1a
 * Byte 4: Type: 0xff (Custom Manufacturer Packet)
 * Byte 5-6: Manufacturer ID : 0x4c00 (Apple)
 * Byte 7: SubType: 0x02 (iBeacon)
 * Byte 8: SubType Length: 0x15
 * Byte 9-24: Proximity UUID
 * Byte 25-26: Major
 * Byte 27-28: Minor
 * Byte 29: Signal Power
 * Ref : https://en.wikipedia.org/wiki/IBeacon#Technical_details
 * </p>
 */


public class IBeaconManager {

    private BluetoothManager mBTManager;
    private BluetoothAdapter mBTAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mIsScanning = false;

    private Handler mScanHandler = new Handler();
    private final static int SCAN_INTERVAL = 5000;

    private IBeaconListener mIBeaconListener = null;

    public interface IBeaconListener {
        public void receivedIBeacon(String uuid, int major, int minor);
    }

    public IBeaconManager(Context context) {
        mBTManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = mBTManager.getAdapter();
        mBluetoothLeScanner = mBTAdapter.getBluetoothLeScanner();


    }

    /**
     * To set the Listener for IBeacon receiver
     *
     * @param iBeaconListener
     */
    public void setListener(IBeaconListener iBeaconListener) {
        mIBeaconListener = iBeaconListener;
    }

    /**
     * To stop the beacon search
     */
    public void stopListening() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(scanCallback);
        }

        mScanHandler.removeCallbacks(scanRunnable);

        mIsScanning = false;
    }

    /**
     * To start the Beacon Search
     */
    public void startListening() {
        mScanHandler.post(scanRunnable);
    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {

            if (mIsScanning) {
                if (mBluetoothLeScanner != null) {
                    mBluetoothLeScanner.stopScan(scanCallback);
                }
            } else {
                if (mBluetoothLeScanner != null) {
                    mBluetoothLeScanner.startScan(scanCallback);
                }
            }

            mIsScanning = !mIsScanning;

            mScanHandler.postDelayed(this, SCAN_INTERVAL);
        }
    };


    private static final int IBEACON_IDEDNTIFIER_INDEX = 7;
    private static final int IBEACON_IDEDNTIFIER_SUBTYPE_INDEX = 8;
    private static final int IBEACON_UUID_START_INDEX = 9;
    private static final int IBEACON_MAJOR_START_INDEX = 25;
    private static final int IBEACON_MINOR_START_INDEX = 27;
    private static final int IBEACON_SIGNAL_INDEX = 29;

    /**
     * To check whether the scanned device is iBeacon
     *
     * @param scanRecord - scanRecord Record bytes
     * @return true for iBeacon, false otherwise
     */
    private boolean isiBeacon(final byte[] scanRecord) {

        if (scanRecord.length > 9) {
            int iBeaconIdentifier = ((int) scanRecord[IBEACON_IDEDNTIFIER_INDEX] & 0xff);
            int iBeaconSubType = ((int) scanRecord[IBEACON_IDEDNTIFIER_SUBTYPE_INDEX] & 0xff);

            if (iBeaconIdentifier == 0x02 && iBeaconSubType == 0x15) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * To get the iBeacon UUID from scanRecord bytes
     *
     * @param scanRecord
     * @return iBeacon UUID formatted string
     */
    private String getUUID(final byte[] scanRecord) {
        byte[] bytes = new byte[16];

        System.arraycopy(scanRecord, IBEACON_UUID_START_INDEX, bytes, 0, 16);

        String uuid = bytesToHex(bytes);

        return String.format("%s-%s-%s-%s-%s", uuid.substring(0, 8),
                uuid.substring(8, 12),
                uuid.substring(12, 16),
                uuid.substring(16, 20),
                uuid.substring(20, 32));
    }

    /**
     * To get the major from scanRecord bytes
     *
     * @param scanRecord
     * @return major value for iBeacon
     */
    private int getMajor(final byte[] scanRecord) {
        return (scanRecord[IBEACON_MAJOR_START_INDEX] & 0xff) * 0x100 + (scanRecord[IBEACON_MAJOR_START_INDEX + 1] & 0xff);
    }

    /**
     * To get the minor from scanRecord bytes
     *
     * @param scanRecord
     * @return minor value for iBeacon
     */
    private int getMinor(final byte[] scanRecord) {
        return (scanRecord[IBEACON_MINOR_START_INDEX] & 0xff) * 0x100 + (scanRecord[IBEACON_MINOR_START_INDEX + 1] & 0xff);
    }

    /**
     * To get the minor from scanRecord bytes
     *
     * @param scanRecord
     * @return minor value for iBeacon
     */
    private int getSignalPower(final byte[] scanRecord) {
        return (scanRecord[IBEACON_SIGNAL_INDEX] & 0xff);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            byte[] scanRecord = result.getScanRecord().getBytes();

            if (!isiBeacon(scanRecord)) {
                return;
            }

            String uuid = getUUID(scanRecord);

            final int major = getMajor(scanRecord);

            final int minor = getMinor(scanRecord);

            if (mIBeaconListener != null) {
                mIBeaconListener.receivedIBeacon(uuid, major, minor);
            }
        }

    };

    /**
     * bytesToHex method
     */
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
