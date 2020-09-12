package kr.codingbear.blutoothimage;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter_;
    BluetoothSocket bluetoothSocket_ = null;
    BluetoothDevice bluetoothDevice_ = null;

    private TextView text_status_;
    private Button button_connect_;
    private Button button_send_;
    private ImageView view_image_;
    private EditText input_quality_;

    private StringBuilder logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_status_ = findViewById(R.id.text_status);
        button_connect_ = findViewById(R.id.button_connect);
        button_send_ = findViewById(R.id.button_send);
        view_image_ = findViewById(R.id.view_image);
        input_quality_ = findViewById(R.id.input_quality);

        button_send_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonSendClick();
            }
        });

        button_connect_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    onButtonConnectClick();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        bluetoothAdapter_ = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter_ == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("BluetoothAdapter Not Found")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.finish();
                        }
                    })
                    .create()
                    .show();
        }

        if (!bluetoothAdapter_.isEnabled()) {
            Toast.makeText(this, "Bluetooth Must be Enabled", Toast.LENGTH_LONG).show();

            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetooth);
        }

    }

    private void onButtonSendClick() {
        int quality = 10;

        String quality_raw = input_quality_.getText().toString();
        if (!quality_raw.equals("")) {
            quality = Integer.parseInt(quality_raw);
        }

        if (quality <= 0) {
            quality = 1;
        } else if (quality > 100) {
            quality = 100;
        }

        new BluetoothTask.ImageDownload(new BluetoothTask.OnImageDownloadFinishListener() {
            @Override
            public void onFinish(final Bitmap bitmap) {
                if (bitmap == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,
                                    "Error while downloading Image",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view_image_.setImageBitmap(bitmap);
                        view_image_.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).execute(bluetoothSocket_, quality);

    }

    private void onButtonConnectClick() throws IOException {
        if (bluetoothDevice_ != null) {
            bluetoothSocket_.close();
            button_connect_.setText(R.string.connect);
            bluetoothDevice_ = null;
            view_image_.setVisibility(View.INVISIBLE);
            text_status_.setText(R.string.not_connected);
            text_status_.setTextColor(Color.RED);
            button_send_.setEnabled(false);
        } else {
            button_connect_.setEnabled(false);
            Set<BluetoothDevice> devices = bluetoothAdapter_.getBondedDevices();
            if (devices.size() > 0) {
                for (BluetoothDevice device : devices) {
                    if (device.getName().equals("올바른걸음")) {
                        bluetoothDevice_ = device;
                        break;
                    }
                }
            }

            if (bluetoothDevice_ == null) {
                return;
            }

            new BluetoothTask.Connect(new BluetoothTask.OnConnectFinishListener() {
                @Override
                public void onFinish(BluetoothSocket socket) {
                    if (socket != null) {
                        bluetoothSocket_ = socket;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                button_send_.setEnabled(true);
                                button_connect_.setText(R.string.disconnect);
                                text_status_.setText(R.string.connected);
                                text_status_.setTextColor(Color.parseColor("#037e32"));
                            }
                        });
                    } else {
                        bluetoothDevice_ = null;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            button_connect_.setEnabled(true);
                        }
                    });
                }
            }).execute(bluetoothDevice_);
        }
    }
}