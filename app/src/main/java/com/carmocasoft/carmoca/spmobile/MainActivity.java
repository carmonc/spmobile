package com.carmocasoft.carmoca.spmobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

public class MainActivity extends AppCompatActivity {

    private BleWrapper mBleWrapper = null;

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

        mBleWrapper = new BleWrapper(this,new BleWrapperUiCallbacks.Null()
        {
            public void uiDeviceFound(final BluetoothDevice device,
                                      final int rssi,
                                      final byte[] record)
            {
                String msg = "uiDeviceFound: " + device.getName() + "," + rssi + "," + Integer.toString(rssi);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                Log.d("DEBUG", "uiDeviceFound: " + msg);
            }

        });

        if(mBleWrapper.checkBleHardwareAvailable() == false) {
            Toast.makeText(this, "No BLE-compatible hardware detected.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

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
}
