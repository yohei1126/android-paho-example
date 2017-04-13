package com.example.mqtt;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class MainActivity extends AppCompatActivity {

    private static final String serverUri = "tcp://test.mosquitto.org:1883";
    private static final String userName = "user1";
    private static final String password = "user1_token";
    private static final String appName = "app1";
    private static final String clientId = userName + "@" + appName;
    private static final String subTopic = "topic1";
    private static final String pubTopic = "topic1";
    private static final String TAG = "MainActivity";
    private MqttAndroidClient mqttAndroidClient;
    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText edit = (EditText)findViewById(R.id.editPublish);
        edit.setOnEditorActionListener((tv, actionId, event) -> {
            boolean retval = true;
            String publishMessage = tv.getText().toString();
            try {
                MqttMessage message = new MqttMessage();
                message.setPayload(publishMessage.getBytes());
                mqttAndroidClient.publish(pubTopic, message);
                tv.setText("");
                retval = false;
            } catch (Exception e) {
                Log.e(TAG, "Error Publishing: " + e.getMessage());
                e.printStackTrace();
            }
            return retval;
        });

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    Log.d(TAG, "Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    Log.d(TAG, "Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                final String subscribeMessage = new String(message.getPayload());
                Log.d(TAG, "messageArrived: " + topic + " : " + subscribeMessage);
                TextView tv = (TextView)findViewById(R.id.textSubscribe);
                tv.setText(subscribeMessage + "\n" + tv.getText());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    //EditView clear
                    Toast.makeText(MainActivity.this, "published", Toast.LENGTH_SHORT).show();
                    TextView pubTv = (TextView) findViewById(R.id.textPublish);
                    final String message = new String(token.getMessage().getPayload());
                    pubTv.setText(message + "\n" + pubTv.getText());
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(userName);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    try {
                        asyncActionToken.getSessionPresent();
                    } catch (Exception e) {
                        Log.e(TAG, e.getCause().toString());
                    }
                    Log.d(TAG, "connected to: " + serverUri);
                    Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_SHORT).show();
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to connect to: " + serverUri);
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to subscribe");
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }
}
