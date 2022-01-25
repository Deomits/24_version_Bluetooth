package com.example.a24_version_bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVERABLE_BT = 0;

    private static final String APP_NAME = "BIChat";
    private static final UUID MY_UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    Button listen, listDevices;
    ListView listView;
    TextView status;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice[] btArray;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView out=(TextView)findViewById(R.id.out);
        final Button button1 = (Button)findViewById(R.id.button1);
        final Button button2 = (Button)findViewById(R.id.button2);
        final Button button3 = (Button)findViewById(R.id.button3);
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            out.append("device not supported");
        }
        button1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (!mBluetoothAdapter.isEnabled()){
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                implementListeners();
            }

            private void implementListeners() {
                listDevices.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        Set<BluetoothDevice> bt=mBluetoothAdapter.getBondedDevices();
                        String[] strings=new String[bt.size()];
                        int index=0;

                        if(bt.size()>0){
                            for (BluetoothDevice device : bt){
                                btArray[index]=device;
                                strings[index]=device.getName();
                                index++;
                            }
                            ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1,strings);
                            listView.setAdapter(arrayAdapter);
                        }
                    }
                });

                listen.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        ServerClass serverClass=new ServerClass();
                        serverClass.start();
                    }
                });

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        ClientClass clientClass=new ClientClass(btArray[i]);
                        clientClass.start();

                        status.setText("Connecting");
                    }
                });
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!mBluetoothAdapter.isDiscovering()){
                    out.append("MAKING YOUR DEVICE DISCOVERABLE");
                    Toast.makeText(getApplicationContext(),"MAKING YOUR DEVICE DISCOVERABLE",
                            Toast.LENGTH_LONG);

                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT);
                }
            }
        });
        button3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0){
                mBluetoothAdapter.disable();
                //out.append("TURN_OFF BLUETOOTH");
                Toast.makeText(getApplicationContext(), "TURNING_OFF BLUETOOTH", Toast.LENGTH_LONG);
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage (Message msg) {
            switch (msg.what)
            {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    // we will write it later
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void sendMessage(View view){
        Log.d("Deomi_RTU","Con_Blue");
    }
    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;
        public ServerClass () {
            try{
            serverSocket=mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try{
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                socket=serverSocket.accept();} catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket != null)
                {
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    //write some code for send/receive
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1)
        {
            device=device1;

            try{
            socket=device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            try {
                socket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }
}



