package com.cloudpos.bluetoothprinterdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView textView;
    private Button conBtn, printBtn, disBtn, printBitmap, printText;
    private String TAG = "bluetoothprinterdemo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        conBtn = findViewById(R.id.button1);
        printBtn = findViewById(R.id.button2);
        printBitmap = findViewById(R.id.button4);
        disBtn = findViewById(R.id.button3);
        printText = findViewById(R.id.button5);
        conBtn.setOnClickListener(this);
        printBtn.setOnClickListener(this);
        disBtn.setOnClickListener(this);
        printBitmap.setOnClickListener(this);
        printText.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                getDevices();
                break;
            case R.id.button2:
                String sendData = "Welcome to use WizarPOS_Print device!\nHere is the demo content :12345678901234567890123456789012345678901234567890\n";
                try {
                    byte[] data = sendData.getBytes("gbk");
                    send(data);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "print fail!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button4:
                textView.append("\nPrinting, please wait ！");
                printBitmap();
                break;
            case R.id.button5:
                String textData = "欢迎使用wizarpos打印服务\n";
                try {
                    byte[] data = textData.getBytes("gbk");
                    send(data);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "print fail!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button3:
                disconnect();
                break;
        }
    }

    private void printBitmap() {
        try {
            Bitmap bitmapwrf = BitmapFactory.decodeStream(getResources().getAssets()
                    .open("timg.bmp"));
            printBitmapESCStar(bitmapwrf, 0, 0, 200);
        } catch (IOException e) {
            Toast.makeText(this, "print fail!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    public void printBitmapESCStar(Bitmap bm, int bitMarginLeft, int bitMarginTop, int brightness) {
        byte[] result = generateCmdBitmapArrayESCStar(bm, bitMarginLeft, bitMarginTop, brightness);
        int lines = (result.length - GSV_HEAD) / WIDTH;
        byte[] src = new byte[]{0x1D, 0x76, 0x30, 0x00, 0x30, 0x00,
                (byte) (lines & 0xff), (byte) ((lines >> 8) & 0xff)};
        System.arraycopy(src, 0, result, 0, GSV_HEAD);
        write(result, result.length);
        result[0] = (byte) '\n';
        write(result, 1);
        flush();
    }

    public final int BIT_WIDTH = 384;

    private final int WIDTH = 48;
    private final int GSV_HEAD = 8;

    /**
     *
     * @param bm
     * @param bitMarginLeft
     * @param bitMarginTop
     * @param mBrightness ：The parameter define the brightness threhold of the color during image conversion to black and white.
     *                   The larger the value, the lighter the color that can be printed.
     * @return
     */
    private byte[] generateCmdBitmapArrayESCStar(Bitmap bm, int bitMarginLeft, int bitMarginTop, int mBrightness) {
        byte[] result = null;
        int n = bm.getHeight() + bitMarginTop;
        int offset = GSV_HEAD;
        result = new byte[n * WIDTH + offset];
        for (int y = 0; y < bm.getHeight(); y++) {
            for (int x = 0; x < bm.getWidth(); x++) {
                if (x + bitMarginLeft < BIT_WIDTH) {
                    int color = bm.getPixel(x, y);
                    int red = Color.red(color);
                    int blue = Color.blue(color);
                    int green = Color.green(color);
                    int alpha = Color.alpha(color);
                    int brightness = (red + green + blue) / 3;
                    if (alpha > 128 && (red < 128 || blue < 128 || green < 128 || brightness < mBrightness)) {
                        int bitX = bitMarginLeft + x;
                        int byteX = bitX / 8;
                        int byteY = y + bitMarginTop;
                        result[offset + byteY * WIDTH + byteX] |= (0x80 >> (bitX - byteX * 8));
                    }
                } else {
                    break;
                }
            }
        }
        return result;
    }


    BluetoothDevice dev;
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

    public void getDevices() {

        if (adapter != null) {
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            if (devices.size() > 0) {
                for (BluetoothDevice device : devices) {
                    String addr = device.getAddress();
                    Log.d(TAG, "device.getAddress() = " + addr);
                    if (addr.equalsIgnoreCase("99:87:65:43:21:00")) {
                        dev = adapter.getRemoteDevice(addr);
                        connect();
                    }
                }
            } else {
                Log.d(TAG, "No paired bluetooth device!");
            }
        } else {
            Log.d(TAG, "This device has no bluetooth device!");
        }
    }

    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static BluetoothSocket bluetoothSocket = null;
    private static boolean isConnected = false;
    private static OutputStream outputStream = null;

    public boolean connect() {
        if (!isConnected) {
            try {
                bluetoothSocket = dev.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;
                if (adapter.isDiscovering()) {
                    Toast.makeText(this, "close BluetoothAdapter!", Toast.LENGTH_LONG).show();
                    adapter.isDiscovering();
                }
                textView.setText(dev.getName());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Bluetooth connect fail!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        Toast.makeText(this, dev.getName() + "Bluetooth connect success!", Toast.LENGTH_SHORT).show();
        return true;
    }


    public void send(byte[] data) {
        if (isConnected) {
            write(data, data.length);
            flush();
        } else {
            Toast.makeText(this, "device not connect...", Toast.LENGTH_SHORT).show();
        }
    }

    public void write(byte[] buff, int length) {
        try {
            outputStream.write(buff, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
            textView.setText("Connection disconnected!");
            isConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
