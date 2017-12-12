package com.carmocasoft.carmoca.spmobile;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BleWrapper mBleWrapper = null;
    private RequestQueue queue = null;
    private EditText ipAddy = null;
    private String datetime;
    private String url;

    ImageView graph;
    ProgressDialog mProgressDialog;

    private static final UUID
        UUID_ARDUINO_SERV   = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
        UUID_ARDUINO_TX     = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"),
        UUID_ARDUINO_RX     = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"),

        UUID_NORDIC_SERV    = UUID.fromString("00001530-1212-efde-1523-785feabcd123"),
        UUID_NORDIC_CHAR1   = UUID.fromString("00001531-1212-efde-1523-785feabcd123"),
        UUID_NORDIC_CHAR2   = UUID.fromString("00001532-1212-efde-1523-785feabcd123"),
        UUID_NORDIC_CHAR4   = UUID.fromString("00001534-1212-efde-1523-785feabcd123"),

        UUID_GENERIC_SERV   = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"),
        UUID_GENERIC_CHAR0   = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"), /*returns name*/
        UUID_GENERIC_CHAR1   = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"),
        UUID_GENERIC_CHAR4   = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb"),

        UUID_DEVI_SERV    = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb"),
        UUID_DEVI_CHAR0   = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb"), /* Adafruit Industries*/
        UUID_DEVI_CHAR1   = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"), /* BLESPIFRIEND */
        UUID_DEVI_CHAR2   = UUID.fromString("00002a28-0000-1000-8000-00805f9b34fb"), /* 0.5.5 - Dec 13 2016 */
        UUID_DEVI_CHAR3   = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb"), /* S110 8.0.0, 0.2 */
        UUID_DEVI_CHAR4   = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb"), /* QFACA10 */

        UUID_GENATT_SERV    = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb"),
        UUID_GENATT_CHAR0   = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");

    private static final int
            CHARACTERISTIC_DISABLE = 0x00,
            CHARACTERISTIC_ENABLE  = 0x01;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        queue = Volley.newRequestQueue(this);
        ipAddy = (EditText) findViewById(R.id.edit_text_ip_address);

        final Button btnGetLevelFromBLE = (Button) findViewById(R.id.read_from_ble);
        final Button btnSendJson = (Button) findViewById(R.id.btn_send_json);
        final Button btnUpdateHtml = (Button) findViewById(R.id.btn_update_html);
        graph = (ImageView) findViewById(R.id.imageViewGraph);

        btnUpdateHtml.setVerticalScrollBarEnabled(true);
        btnUpdateHtml.setMovementMethod(new ScrollingMovementMethod());

        btnUpdateHtml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String user_uri_entry = ipAddy.getText().toString();
                if(user_uri_entry.isEmpty())
                    return;
                String url = "http://" + user_uri_entry + ":7100/getGraph";
                new DownloadImage().execute(url);

                //Fetch HTML...
                //FIXME! This code works (in that it fetches HTML. The problem exists
                //FIXME!  in the rendering of that HTML. Text elements render well;
                //FIXME!  everything else not so much.

                //FIXME! I'm not going to remove this at this time but for future
                //FIXME!  future use it is definately a good example on how to
                //FIXME!  download HTML.
                url = "http://" + user_uri_entry + ":7100/report";
                new fetchHTML(new OnTaskFinished()
                {
                    @Override
                    public void onHtmlRetrieved(String feeds)
                    {
                        TextView content = (TextView) findViewById(R.id.textViewHtml);
                        content.setVerticalScrollBarEnabled(true);
                        content.setMaxLines(10);
                        content.setMovementMethod(new ScrollingMovementMethod());
                        content.setText(Html.fromHtml(feeds));
                    }
                }).execute(url);

                // Execute DownloadImage AsyncTask
            }
        });

        btnGetLevelFromBLE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("DEBUG","ServiceRequested");
                BluetoothGatt gatt;
                BluetoothGattCharacteristic c;

                gatt = mBleWrapper.getGatt();

                //c = gatt.getService(UUID_ARDUINO_SERV).getCharacteristic(UUID_ARDUINO_TX);
                c = gatt.getService(UUID_NORDIC_SERV).getCharacteristic(UUID_NORDIC_CHAR4);
                //mBleWrapper.requestCharacteristicValue(c);
                mBleWrapper.setNotificationForCharacteristic(c, true);
            }
        });

        btnSendJson.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("DEBUG","Making HTTP POST Request");

                String user_uri_entry = ipAddy.getText().toString();
                if(user_uri_entry.isEmpty())
                    return;
                String url = "http://" + user_uri_entry + ":7100/submit";

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.forLanguageTag(Locale.getDefault().getDisplayLanguage()));
                long msTime = System.currentTimeMillis();
                Date curDateTime = new Date(msTime);
                datetime = sdf.format(curDateTime);

                Log.d("DEBUG", "Current time is:" + datetime);

                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                // response
                                Log.d("Response:", response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error
                                Log.d("Error.Response", error.toString());
                            }
                        } ) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String>  params = new HashMap<String, String>();
                        Random rn = new Random();
                        int level = rn.nextInt(100) + 1;
                        params.put("sensor", "GEN");
                        params.put("level", Integer.toString(level)); //FIXME! Value from BLE
                        params.put("date", datetime);

                        return params;
                    }
                };

                queue.add(postRequest);

                String url2 = "http://" + user_uri_entry + ":7100/getGraph";
                new DownloadImage().execute(url2);
            }
        });

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

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
                mBleWrapper.disconnect();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mBleWrapper.isBtEnabled()) {
            Intent enableBtIntent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            startActivity(enableBtIntent);
            finish();
        }

        mBleWrapper.initialize();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBleWrapper.disconnect();
        mBleWrapper.close();
    }

    public void getTankLevelFromBLE(View v) {
        BluetoothGatt gatt;
        BluetoothGattCharacteristic c;

        gatt = mBleWrapper.getGatt();

        c = gatt.getService(UUID_ARDUINO_SERV).getCharacteristic(UUID_ARDUINO_RX);
        mBleWrapper.requestCharacteristicValue(c);

    }

    // DownloadImage AsyncTask
    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Create a progressdialog
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle("Downloading Image");
            mProgressDialog.setMessage("Please wait...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

        @Override
        protected Bitmap doInBackground(String... URL) {

            String imageURL = URL[0];

            Bitmap bitmap = null;
            try {
                // Download Image from URL
                InputStream input = new java.net.URL(imageURL).openStream();
                // Decode Bitmap
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Set the bitmap into ImageView
            graph.setImageBitmap(result);
            // Close progressdialog
            mProgressDialog.dismiss();
        }
    }
}
