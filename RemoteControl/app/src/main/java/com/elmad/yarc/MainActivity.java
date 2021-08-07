package com.elmad.yarc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class MainActivity extends AppCompatActivity {

    public static String hostAddress;
    public static int hostPort;
    public static Client client = null;
    public static SharedPreferences pref;
    public static TextView status;
    public static Thread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        status = (TextView) findViewById(R.id.status);
        final ImageView surface = (ImageView) findViewById(R.id.surface);
        pref = PreferenceManager.getDefaultSharedPreferences(this);

        //CHECK CONNECTION
        String previousAddress = pref.getString("RCHostAddress", "n/a");
        String previousPort    = pref.getString("RCHostPort", "n/a");
        if (!(previousAddress.equals("n/a")) && !(previousPort.equals("n/a"))) {
            hostAddress = previousAddress;
            hostPort = Integer.parseInt(previousPort);
            connect();
        }
        else {
            String result = "No previous server found. Please, set up a new connection.";
            status.setText(result);
        }


        //SURFACE CONTROLS
        surface.setOnTouchListener(new View.OnTouchListener() {
            int previousX = 0;
            int previousY = 0;
            long duration = 0;
            boolean scroll = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                int touchX = (int) event.getX();
                int touchY = (int) event.getY();
                int imageX = touchX * 960 / surface.getMeasuredWidth();
                int imageY = touchY * 540 / surface.getMeasuredHeight();

                if (client != null) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            int dx = imageX - previousX;
                            int dy = imageY - previousY;
                            if (event.getPointerCount() < 2) {
                                String message = "mov _ " + dx + " " + dy + "\n";
                                client.sendMessage(message);
                            }
                            else {
                                if (dy > 0) client.sendMessage("scu\n");
                                if (dy < 0) client.sendMessage("scd\n");
                            }
                            break;
                        case MotionEvent.ACTION_DOWN:
                            duration = event.getEventTime();
                            break;
                        case MotionEvent.ACTION_UP:
                            if ((event.getEventTime() - duration) < 250)
                                client.sendMessage("lmc\n");
                            break;
                    }
                }
                previousX = imageX;
                previousY = imageY;
                return true;
            }
        });


        //LBUTTON CONTROLS
        final Button lbutton = (Button) findViewById(R.id.leftButton);
        lbutton.setOnTouchListener(new View.OnTouchListener() {
            boolean state = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (client != null)
                            client.sendMessage("lmd\n");
                        state = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (client != null)
                            client.sendMessage("lmu\n");
                        state = false;
                        break;
                }
                lbutton.setPressed(state);
                return true;
            }
        });


        //RBUTTON CONTROLS
        Button rbutton = (Button) findViewById(R.id.rightButton);
        rbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (client != null)
                    client.sendMessage("rmc\n");
            }
        });

        //VOLUME CONTROL
        final SeekBar seekBar = (SeekBar) findViewById(R.id.volumeSeek);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progress = seekBar.getProgress();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressVal, boolean fromUser) {
                progress = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progress = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                client.sendMessage("vol _ " + progress + "\n");
            }
        });

        //GOBACK BUTTON
        ImageButton goBackButton =  (ImageButton) findViewById(R.id.goback_btn);
        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (client != null)
                    client.sendMessage("gbk\n");
            }
        });

        //ACK TIMER
        if (client != null)
            client.startTimer();
    }


    //MENU CONTROLS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.config) {
            Intent intent = new Intent(this, Configuration.class);
            startActivity(intent);
        }
        else if (item.getItemId() == R.id.home) {
            if (client.isSynced()) {
                client.sendMessage("hom\n");
            }
        }
        else if (item.getItemId() == R.id.keyboard) {
            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        else if (item.getItemId() == R.id.power) {
            //power down routine
            if (client.isSynced()) {
                client.sendMessage("pwr\n");
                item.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_power_off_24dp));
            }
            //power on
            else {
                String previousMAC = pref.getString("RCMACAddress", "n/a");
                if (!(previousMAC.equals("n/a"))) {
                    try {
                        byte[] macBytes = getMacBytes(previousMAC);
                        byte[] bytes = new byte[6 + 16 * macBytes.length];
                        for (int i = 0; i < 6; i++)
                            bytes[i] = (byte) 0xff;
                        for (int i = 6; i < bytes.length; i += macBytes.length)
                            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
                        client.sendWoL(bytes);
                        item.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_power_on_24dp));
                    } catch (Exception e) {
                        String result = "Failed to send Wake-on-Lan packet: " + e.toString();
                        status.setText(result);
                    }
                }
                else {
                    String result = "No previous MAC address configured. Please, set up a new one.";
                    status.setText(result);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && client != null) {
            if (event.isPrintingKey()) {
                char key = (char) event.getUnicodeChar();
                client.sendMessage("chr " + key + "\n");
            }
            else {
                //special cases!
                int key = event.getKeyCode();
                switch(key) {
                    case 62: client.sendMessage("key _ " + 65 + "\n"); break; //SPACE
                    case 66: client.sendMessage("key _ " + 36 + "\n"); break; //ENTER
                    case 67: client.sendMessage("key _ " + 22 + "\n"); break; //BACKSPACE
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void connect() {
        client = new Client(hostAddress, hostPort);
        thread = new Thread(client);
        thread.start();
        client.sendMessage("ack\n");
    }

    public static void setStatus() {
        if (client.isSynced()) {
            String result = "Synced to: " + hostAddress;
            status.setText(result);
        }
        else {
            String result = "Could not sync to server: " + hostAddress;
            status.setText(result);
        }
    }

    //based on http://www.jibble.org/wake-on-lan/
    public static byte[] getMacBytes(String macString) throws IllegalArgumentException {
        byte[] bytes = new byte[6];
        String[] hex = macString.split("(\\:|\\-)");
        if (hex.length != 6) {
            throw new IllegalArgumentException("The MAC address you entered is invalid.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex digit in MAC address.");
        }
        return bytes;
    }

}
