package com.example.myapplication.Activity.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private EditText userInput;
    private Button sendButton;
    private LinearLayout chatLayout;
    private OkHttpClient client;

    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_main);

        userInput = findViewById(R.id.userInput);
        sendButton = findViewById(R.id.sendButton);
        chatLayout = findViewById(R.id.chatLayout);
        scrollView = findViewById(R.id.scrollView);
        client = new OkHttpClient();

        sendButton.setOnClickListener(v -> {
            String message = userInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage("You: " + message, true);
                sendMessageToGPT(message);
                userInput.setText("");
            }
        });
    }

    private void addMessage(String message, Boolean isUser) {
        View messageView = LayoutInflater.from(this).inflate(
                isUser ? R.layout.item_message_sent : R.layout.item_message_received,
                chatLayout,
                false);

        TextView messageText = messageView.findViewById(R.id.messageText);
        messageText.setText(message);
        chatLayout.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessageToGPT(String message) {
        String json = "{\n" +
                "  \"model\": \"gpt-4o-mini\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": \"" + message + "\"}\n" +
                "  ]\n" +
                "}";

        RequestBody body = RequestBody.create(
                json,
                MediaType.parse("application/json"));
        String apiKey = BuildConfig.OPENAI_API_KEY;

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("Bot: Failed to get response", false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    String botReply = parseResponse(res);
                    runOnUiThread(() -> addMessage("Bot: " + botReply, false));
                } else {
                    Log.e("error {}", response.toString());
                    runOnUiThread(() -> addMessage("Bot: Error from API", false));
                }
            }
        });
    }

    private String parseResponse(String responseBody) {
        try {
            JSONObject obj = new JSONObject(responseBody);
            return obj.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
        } catch (JSONException e) {
            return "Error parsing response.";
        }
    }
}
