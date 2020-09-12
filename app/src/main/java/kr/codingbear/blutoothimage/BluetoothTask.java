package kr.codingbear.blutoothimage;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.UUID;

public class BluetoothTask {

    public interface OnConnectFinishListener {
        void onFinish(final BluetoothSocket socket);
    }

    public interface OnImageDownloadFinishListener {
        void onFinish(final Bitmap bitmap);
    }

    public static class Connect extends AsyncTask<BluetoothDevice, Integer, BluetoothSocket> {
        private OnConnectFinishListener listener_;

        public Connect(OnConnectFinishListener listener) {
            listener_ = listener;
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... devices) {
            BluetoothDevice device = devices[0];

            try {
                UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();

                return socket;
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            listener_.onFinish(bluetoothSocket);
        }
    }


    public static class ImageDownload extends AsyncTask<Object, Integer, Bitmap> {
        private OnImageDownloadFinishListener listener_;

        public ImageDownload(OnImageDownloadFinishListener listener) {
            listener_ = listener;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            BluetoothSocket socket = (BluetoothSocket) params[0];
            int quality = (int) params[1];

            try {
                DataInputStream inputStream_ = new DataInputStream(socket.getInputStream());
                BufferedWriter outputStream_ = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                outputStream_.write(String.format(Locale.US, "{\"command\":\"give\",\"quality\":%d}\n", quality));
                outputStream_.flush();

                int size = inputStream_.readInt();

                byte[] data = new byte[size];
                inputStream_.readFully(data);

                return BitmapFactory.decodeByteArray(data, 0, size);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            listener_.onFinish(bitmap);
        }
    }
}
