package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    /*
        Object Declarations

        Variable Blocks:
            1. Bluetooth Variables
            2. Program Variables
            3. Temperature History/Print Variables

     */
    private BluetoothAdapter BTAdapt;
    private BluetoothDevice hc;
    private BluetoothSocket BTsock;
    private InputStream sockIn;

    private boolean RUNNING;
    private final int ObjTotal = 5;

    private TextView tx;
    private Queue<String> temperatureHist;
    private String[] tempHistArr;
    private TextView[] histPrint;


    /*
        onCreate Method

            Declares necessary global variables.
            Gets the default bluetooth adapter.
                WARNING: Bluetooth Functionality Required.
            Initializes HC-05 device.
                WARNING: Must be paired to HC-05.
            Starts HC-05 connection socket.
            Starts data thread.

     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initializing Objects
        tempHistArr = new String[ObjTotal];
        temperatureHist = new LinkedList<>();
        for(int i = 0; i < ObjTotal; i++) {
            temperatureHist.add("0.00");
            tempHistArr[i] = "0.00";
        }

        RUNNING = true; //Thread Variable

        //TextView Initializations
        tx = findViewById(R.id.currTemp);
        histPrint = new TextView[5];
        histPrint[0] = findViewById(R.id.hist1);
        histPrint[1] = findViewById(R.id.hist2);
        histPrint[2] = findViewById(R.id.hist3);
        histPrint[3] = findViewById(R.id.hist4);
        histPrint[4] = findViewById(R.id.hist5);


        BTAdapt = BluetoothAdapter.getDefaultAdapter();
        if (!(BTAdapt == null)) {
            if (!BTAdapt.isEnabled()) {
                Intent BTPerm = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(BTPerm, 0);
            }
            hc = findBTDevice();
            BTsock = connectBT();
            startBT();

            if(BTsock != null) {
                displayData();
            }

        }

    }


    /*
        Find Bluetooth Device Function

            Searches for HC-05 and returns it
                WARNING: Must be paired to HC-05
            Returns null BluetoothDevice otherwise

     */
    public BluetoothDevice findBTDevice() {
        Set<BluetoothDevice> pairedDevices = BTAdapt.getBondedDevices();
        String macAdd;

        if(pairedDevices.size() > 0) {
            for(BluetoothDevice dev : pairedDevices) {
                macAdd = dev.getAddress();

                if (macAdd.equalsIgnoreCase("98:D3:11:FC:94:94")) {
                    return dev;
                }
            }
        }

        return null;
    }

    /*
        Bluetooth Connect Function

            Establishes and returns a Socket with HC-05
                WARNING: no socket to the HC-05 can already exist
            returns null socket otherwise

     */
    public BluetoothSocket connectBT() {

        if (hc != null) {
            try {
                return hc.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;

    }


    /*
        Start Bluetooth Function

            Begins the socket connection to the HC-05
            Initializes the socket input stream

     */
    public void startBT() {
        if(BTsock != null) {
            try {
                BTsock.connect();
                sockIn = BTsock.getInputStream();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /*
        Display Data Function

            Starts a Thread that handles the input stream
            Determines when to send data based on new line byte '\n'
            Updates both the current temperature and the temperature history
            Continually runs until program is closed

     */
    public void displayData() {
        final Handler handle = new Handler();
        final byte delim = 10;

        final int[] position = {0};
        byte[] buffer = new byte[256];

        Thread inputThread = new Thread(() -> {
             while(!Thread.currentThread().isInterrupted() && RUNNING) {

                 try {

                     int availableBytes = sockIn.available();
                     if(availableBytes > 0) {

                         byte[] packet = new byte[availableBytes];
                         final int read = sockIn.read(packet);

                         for(int i = 0; i < availableBytes; i++) {
                            byte b = packet[i];

                            if(b == delim) {
                                byte[] encoded = new byte[position[0]];
                                System.arraycopy(buffer, 0, encoded, 0, encoded.length);
                                final String data = new String(encoded, StandardCharsets.US_ASCII);
                                final String toPrint = "Current Temperature: " + data;
                                position[0] = 0;
                                handle.post(() -> tx.setText(toPrint));
                                temperatureHist.remove();
                                temperatureHist.add(data);
                                updateHistory();
                            }
                            else {

                                buffer[position[0]] = b;
                                position[0]++;
                            }

                         }
                     }
                 }
                 catch (Exception e) {
                     RUNNING = false;
                     e.printStackTrace();
                 }

             }
        });
        inputThread.start();


    }

    /*
        Update History Function

            Updates the UI of the history section
            Garbage data is taken care of in the
                Display Data Function

     */
    public void updateHistory() {

        int i = 0;
        for(String s : temperatureHist) {
            tempHistArr[4 - i] = s;
            i++;
        }

        for(int j = 0; j < ObjTotal; j++) {

            String toPrint = (j + 1) + ": " + tempHistArr[j];
            histPrint[j].setText(toPrint);
        }
    }

    /*
        System Close function

            Dynamically closes the socket resources used

     */
    public void SysClose() {
        if(BTsock != null) {
            try {
                sockIn.close();
                BTsock.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /*
        On Destroy Method

        Closes socket and thread resources
        on Application close

     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        SysClose();
        RUNNING = false;

    }

}