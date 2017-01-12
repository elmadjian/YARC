package com.elmad.yarc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    public static String hostAddress;
    public static int hostPort;
    public static Client client = null;
    public static SharedPreferences pref;
    public static TextView status;

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
        if (!(previousAddress.equals("n/a")) && !(previousPort.equals("n/a")))
            connect();


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

    }

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
        else if (item.getItemId() == R.id.keyboard) {
            InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            mgr.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && client != null) {
            System.out.println(event.getKeyCode());
            if (event.isPrintingKey()) {
                char key = (char) event.getUnicodeChar();
                client.sendMessage("chr " + key + "\n");
            }
            else {

                //special cases!
                int key = event.getKeyCode();
                switch(key) {
                    case 55: client.sendMessage("key _ " + 59 + "\n"); break; //,
                    case 56: client.sendMessage("key _ " + 60 + "\n"); break; //.
                    case 62: client.sendMessage("key _ " + 65 + "\n"); break; //SPACE
                    case 66: client.sendMessage("key _ " + 36 + "\n"); break; //ENTER
                    case 67: client.sendMessage("key _ " + 22 + "\n"); break; //BACKSPACE
                    case 76: client.sendMessage("key _ " + 97 + "\n"); break; ///
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public static void connect() {
        System.out.println(hostAddress + "  " + hostPort);
        client = new Client(hostAddress, hostPort);
        client.execute();
        setStatus();
    }

    public static void setStatus() {
        if (client.isConnected()) {
            String result = "Connected to: " + hostAddress;
            status.setText(result);
        }
        else {
            String result = "Could not connect to: " + hostAddress;
            status.setText(result);
        }
    }

}
