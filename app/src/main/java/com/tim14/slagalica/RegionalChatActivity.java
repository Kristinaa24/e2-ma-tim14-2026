package com.tim14.slagalica;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.tim14.slagalica.model.ChatMessage;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class RegionalChatActivity extends AppCompatActivity {

    public static final String EXTRA_REGION_NAME = "region_name";

    private TextView chatRegionText;
    private TextView chatStatusText;
    private TextView emptyChatText;
    private EditText chatInput;
    private Button sendChatButton;
    private RecyclerView chatRecyclerView;

    private FirestoreRepository firestoreRepository;
    private ListenerRegistration messageListener;
    private RegionalChatAdapter chatAdapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private String currentUserId = "";
    private String currentRegionName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regional_chat);

        firestoreRepository = new FirestoreRepository(this);
        chatRegionText = findViewById(R.id.chatRegionText);
        chatStatusText = findViewById(R.id.chatStatusText);
        emptyChatText = findViewById(R.id.emptyChatText);
        chatInput = findViewById(R.id.chatInput);
        sendChatButton = findViewById(R.id.sendChatButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        findViewById(R.id.chatBackButton).setOnClickListener(v -> finish());

        chatAdapter = new RegionalChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(false);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        sendChatButton.setOnClickListener(v -> sendMessage());
        chatInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        loadChatContext();
    }

    private void loadChatContext() {
        chatStatusText.setText(R.string.chat_loading_status);

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUserId = user.id == null ? "" : user.id;
                chatAdapter.setCurrentUserId(currentUserId);

                String requestedRegion = getIntent().getStringExtra(EXTRA_REGION_NAME);
                String resolvedRegion = TextUtils.isEmpty(requestedRegion)
                        ? user.region
                        : requestedRegion;
                currentRegionName = FirestoreRepository.canonicalRegionName(resolvedRegion);

                if (TextUtils.isEmpty(currentRegionName)) {
                    chatStatusText.setText(R.string.chat_region_missing);
                    Toast.makeText(
                            RegionalChatActivity.this,
                            R.string.chat_region_missing,
                            Toast.LENGTH_SHORT
                    ).show();
                    sendChatButton.setEnabled(false);
                    return;
                }

                chatRegionText.setText(getString(R.string.chat_region_format, currentRegionName));
                chatStatusText.setText(R.string.chat_ready_status);
                startListeningForMessages();
                firestoreRepository.markCurrentUserChatNotificationsRead();
            }

            @Override
            public void onError(String error) {
                chatStatusText.setText(error);
                Toast.makeText(RegionalChatActivity.this, error, Toast.LENGTH_SHORT).show();
                sendChatButton.setEnabled(false);
            }
        });
    }

    private void startListeningForMessages() {
        if (messageListener != null) {
            messageListener.remove();
        }

        messageListener = firestoreRepository.listenToRegionalChatMessages(
                currentRegionName,
                new FirebaseCallback<List<ChatMessage>>() {
                    @Override
                    public void onSuccess(List<ChatMessage> result) {
                        messages.clear();
                        messages.addAll(result);
                        chatAdapter.notifyDataSetChanged();
                        firestoreRepository.markCurrentUserChatNotificationsRead();
                        emptyChatText.setVisibility(messages.isEmpty()
                                ? android.view.View.VISIBLE
                                : android.view.View.GONE);

                        if (!messages.isEmpty()) {
                            chatRecyclerView.post(() ->
                                    chatRecyclerView.scrollToPosition(messages.size() - 1)
                            );
                        }
                    }

                    @Override
                    public void onError(String error) {
                        chatStatusText.setText(error);
                    }
                }
        );
    }

    private void sendMessage() {
        String messageText = chatInput.getText() == null
                ? ""
                : chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (TextUtils.isEmpty(currentRegionName)) {
            Toast.makeText(this, R.string.chat_region_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        sendChatButton.setEnabled(false);
        firestoreRepository.sendRegionalChatMessage(
                currentRegionName,
                messageText,
                new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        chatInput.setText("");
                        sendChatButton.setEnabled(true);
                    }

                    @Override
                    public void onError(String error) {
                        sendChatButton.setEnabled(true);
                        Toast.makeText(RegionalChatActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        super.onDestroy();
    }
}
