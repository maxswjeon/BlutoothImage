package kr.codingbear.blutoothimage;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

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
                try {
                    onButtonSendClick();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    private void onButtonSendClick() throws IOException {
        long start = System.currentTimeMillis();
        DataInputStream inputStream_ = new DataInputStream(bluetoothSocket_.getInputStream());
        BufferedWriter outputStream_ = new BufferedWriter(new OutputStreamWriter(bluetoothSocket_.getOutputStream()));

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

        outputStream_.write(String.format(Locale.US, "{\"command\":\"give\",\"quality\":%d}\n", quality));
        outputStream_.flush();

        int size = inputStream_.readInt();

        byte[] data = new byte[size];
        inputStream_.readFully(data);

        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(data, 0, size);

        view_image_.setImageBitmap(decodedBitmap);
        long dur = System.currentTimeMillis() - start;

        Toast.makeText(this, "Duration : " + dur + "ms", Toast.LENGTH_SHORT).show();

        view_image_.setVisibility(View.VISIBLE);
    }

    private void onButtonConnectClick() throws IOException {
        if (bluetoothDevice_ != null) {
            bluetoothSocket_.close();
            button_connect_.setText(R.string.connect);
            bluetoothDevice_ = null;
            button_send_.setEnabled(false);
            view_image_.setVisibility(View.INVISIBLE);
            text_status_.setText(R.string.not_connected);
            text_status_.setTextColor(Color.RED);
        } else {
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

            UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
            bluetoothSocket_ = bluetoothDevice_.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket_.connect();

            button_send_.setEnabled(true);
            button_connect_.setText(R.string.disconnect);
            text_status_.setText(R.string.connected);
            text_status_.setTextColor(Color.parseColor("#037e32"));
        }
    }
}