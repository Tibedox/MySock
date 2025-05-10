package com.example.mysockets;

// MainActivity.java
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private EditText clientNumberEditText, messageEditText;
    private TextView receivedTextView;
    private Button sendButton;

    private MyData myData = new MyData();
    private MyData myData1 = new MyData();
    private Socket clientSocket;
    private BufferedWriter out;
    private BufferedReader in;
    private boolean isListening = true;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clientNumberEditText = findViewById(R.id.clientNumberEditText);
        messageEditText = findViewById(R.id.messageEditText);
        receivedTextView = findViewById(R.id.receivedTextView);
        sendButton = findViewById(R.id.sendButton);

        // Запускаем поток для прослушивания сервера
        new Thread(this::startListening).start();

        sendButton.setOnClickListener(v -> {
            try {
                int clientNumber = Integer.parseInt(clientNumberEditText.getText().toString());
                String message = messageEditText.getText().toString();

                myData.n = clientNumber;
                myData.s = message;

                // Отправляем данные на сервер
                new SendDataTask().execute(myData);
            } catch (NumberFormatException e) {
                receivedTextView.setText("Введите корректный номер клиента");
            }
        });
    }

    private void startListening() {
        try {
            clientSocket = new Socket("bbb.eduworks.ru", 80800);
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

            while (isListening) {
                String received = in.readLine();
                if (received != null) {
                    MyData receivedData = gson.fromJson(received, MyData.class);
                    myData1 = receivedData;

                    runOnUiThread(() ->
                            receivedTextView.setText(myData1.n + " " + myData1.s)
                    );
                }
            }
        } catch (IOException e) {
            runOnUiThread(() ->
                    receivedTextView.setText("Ошибка подключения: " + e.getMessage())
            );
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendDataTask extends AsyncTask<MyData, Void, Void> {
        @Override
        protected Void doInBackground(MyData... myData) {
            try {
                String json = gson.toJson(myData[0]);
                out.write(json + "\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false;
        try {
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}