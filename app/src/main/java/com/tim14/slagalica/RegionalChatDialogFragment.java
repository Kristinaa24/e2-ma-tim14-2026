package com.tim14.slagalica;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.tim14.slagalica.model.ChatMessage;
import com.tim14.slagalica.model.User;
import com.tim14.slagalica.repository.FirebaseCallback;
import com.tim14.slagalica.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

public class RegionalChatDialogFragment extends DialogFragment {

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

    public static RegionalChatDialogFragment newInstance() {
        return new RegionalChatDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_Slagalica);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.activity_regional_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestoreRepository = new FirestoreRepository(requireContext());
        chatRegionText = view.findViewById(R.id.chatRegionText);
        chatStatusText = view.findViewById(R.id.chatStatusText);
        emptyChatText = view.findViewById(R.id.emptyChatText);
        chatInput = view.findViewById(R.id.chatInput);
        sendChatButton = view.findViewById(R.id.sendChatButton);
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);

        Button closeButton = view.findViewById(R.id.chatBackButton);
        closeButton.setText(R.string.cancel);
        closeButton.setOnClickListener(v -> dismiss());

        chatAdapter = new RegionalChatAdapter(messages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
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

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private void loadChatContext() {
        chatStatusText.setText(R.string.chat_loading_status);

        firestoreRepository.getCurrentUser(new FirebaseCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) {
                    return;
                }

                currentUserId = user.id == null ? "" : user.id;
                chatAdapter.setCurrentUserId(currentUserId);
                currentRegionName = FirestoreRepository.canonicalRegionName(user.region);

                if (TextUtils.isEmpty(currentRegionName)) {
                    chatStatusText.setText(R.string.chat_region_missing);
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
                if (!isAdded()) {
                    return;
                }

                chatStatusText.setText(error);
                sendChatButton.setEnabled(false);
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
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
                        if (!isAdded()) {
                            return;
                        }

                        messages.clear();
                        messages.addAll(result);
                        chatAdapter.notifyDataSetChanged();
                        firestoreRepository.markCurrentUserChatNotificationsRead();
                        emptyChatText.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);

                        if (!messages.isEmpty()) {
                            chatRecyclerView.post(() ->
                                    chatRecyclerView.scrollToPosition(messages.size() - 1)
                            );
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            chatStatusText.setText(error);
                        }
                    }
                }
        );
    }

    private void sendMessage() {
        String messageText = chatInput.getText() == null
                ? ""
                : chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || TextUtils.isEmpty(currentRegionName)) {
            return;
        }

        sendChatButton.setEnabled(false);
        firestoreRepository.sendRegionalChatMessage(
                currentRegionName,
                messageText,
                new FirebaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (!isAdded()) {
                            return;
                        }

                        chatInput.setText("");
                        sendChatButton.setEnabled(true);
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) {
                            return;
                        }

                        sendChatButton.setEnabled(true);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }
        super.onDestroyView();
    }
}
