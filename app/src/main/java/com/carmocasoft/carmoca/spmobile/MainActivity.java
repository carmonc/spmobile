package com.carmocasoft.carmoca.spmobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BleWrapper mBleWrapper = null;

    private static final UUID
        UUID_ARDUINO_SERV   = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
        UUID_ARDUINO_TX     = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"),
        UUID_ARDUINO_RX     = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int
            CHARACTERISTIC_DISABLE = 0x00,
            CHARACTERISTIC_ENABLE  = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mBleWrapper = new BleWrapper(this,new BleWrapperUiCallbacks.Null() {
            @Override
            public void uiDeviceFound(final BluetoothDevice device,
                                      final int rssi,
                                      final byte[] record) {
                if (device.getName() != null) {
                    String msg = "uiDeviceFound: " + device.getName() + "," + rssi + "," + Integer.toString(rssi);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    Log.d("DEBUG", "uiDeviceFound: " + msg);

                         boolean status = false;
                         if(device.getName().equals("Adafruit Bluefruit LE")) {
                            status = mBleWrapper.connect(device.getAddress());
                            if (!status) {
                                Log.d("DEBUG", "Failed to connect to " + device.getName());
                            }else {
                                Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }

            @Override
            public void uiAvailableServices(BluetoothGatt gatt,
                                            BluetoothDevice device,
                                            List<BluetoothGattService> services) {
                for (BluetoothGattService service : services) {
                    String serviceName = BleNamesResolver.resolveUuid((service.getUuid().toString()));
                    if (serviceName.equals("Unknown UUID")) {
                        serviceName = "Unknown UUID: (" + service.getUuid().toString() + ")";
                    } else {
                        serviceName = serviceName + "(" + service.getUuid().toString() + ")";
                    }
                    List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
                    Log.d("DEBUG", "\n" + serviceName);
                    for (BluetoothGattCharacteristic gattC : gattCharacteristics) {
                        String s = gattC.getUuid().toString();
                        Log.d("DEBUG", " -- GattUuid( " + s + ")\n -- Gatt Char: " + gattC.toString());
                    }
                }
            }

            @Override
            public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
                                                    final BluetoothDevice device,
                                                    final BluetoothGattService service,
                                                    final BluetoothGattCharacteristic ch,
                                                    final String strValue,
                                                    final int intValue,
                                                    final byte[] rawValue,
                                                    final String timestamp) {
                super.uiNewValueForCharacteristic(gatt, device, service, ch, strValue,
                        intValue, rawValue, timestamp);
                Log.d("DEBUG", "uiNewValueForCharacteristic");
                for (byte b : rawValue) {
                    Log.d("DEBUG", "Val: " + b);
                }
            }

        } ) ;

        if(!mBleWrapper.checkBleHardwareAvailable()) {
                Toast.makeText(this, "No BLE-compatible hardware detected.", Toast.LENGTH_LONG).show();
                finish();
                }
    } //end oncreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                break;
            case R.id.action_scan:
                mBleWrapper.startScanning();
                break;
            case R.id.action_stop:
                mBleWrapper.stopScanning();
                break;
            case R.id.action_Disconnect:
                mBleWrapper.stopScanning();
                mBleWrapper.diconnect();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mBleWrapper.isBtEnabled() == false) {
            Intent enableBtIntent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            startActivity(enableBtIntent);
            finish();
        }

        mBleWrapper.initialize();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleWrapper.diconnect();
        mBleWrapper.close();
    }

    public void getTankLevelFromBLE(View v) {
        BluetoothGatt gatt;
        BluetoothGattCharacteristic c;

        gatt = mBleWrapper.getGatt();

        c = gatt.getService(UUID_ARDUINO_SERV).getCharacteristic(UUID_ARDUINO_RX);
        mBleWrapper.requestCharacteristicValue(c);

    }
}
