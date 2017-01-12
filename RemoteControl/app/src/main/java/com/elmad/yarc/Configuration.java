package com.elmad.yarc;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import java.net.Socket;

public class Configuration extends AppCompatActivity {

    private AutoCompleteTextView ipAddress;
    private EditText port;
    public Socket clientSock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        ipAddress = (AutoCompleteTextView) findViewById(R.id.host_ip);
        port = (EditText) findViewById(R.id.host_port);

        String previousAddress = MainActivity.pref.getString("RCHostAddress", "n/a");
        if (!(previousAddress).equals("n/a"))
            ipAddress.setText(previousAddress);

        String previousPort = MainActivity.pref.getString("RCHostPort", "n/a");
        if (!(previousPort).equals("n/a"))
            port.setText(previousPort);

        Button connButton = (Button) findViewById(R.id.conn_button);
        connButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.hostAddress = ipAddress.getText().toString();
                MainActivity.hostPort = Integer.parseInt(port.getText().toString());
                SharedPreferences.Editor edit = MainActivity.pref.edit();
                edit.putString("RCHostAddress", ipAddress.getText().toString());
                edit.putString("RCHostPort", port.getText().toString());
                edit.apply();
                MainActivity.connect();
                Configuration.super.onBackPressed();
            }
        });
    }
}

