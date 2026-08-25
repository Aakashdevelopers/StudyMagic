package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.ai.ChatMessage;

import io.noties.markwon.Markwon;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messages;
    private Markwon markwon;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (markwon == null) {
            markwon = Markwon.create(parent.getContext());
        }
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_ai, parent, false);
            return new AIViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).tvMessage.setText(message.getContent());
        } else if (holder instanceof AIViewHolder) {
            AIViewHolder aiHolder = (AIViewHolder) holder;
            String content = message.getContent();
            
            // Logic to split Short Trick if present in the format [TRICK]...[/TRICK]
            if (content.contains("[TRICK]") && content.contains("[/TRICK]")) {
                String[] parts = content.split("\\[TRICK\\]");
                String mainMsg = parts[0].trim();
                String trick = parts[1].split("\\[/TRICK\\]")[0].trim();
                
                markwon.setMarkdown(aiHolder.tvMessage, mainMsg);
                aiHolder.tvTrick.setText(trick);
                aiHolder.llTrick.setVisibility(View.VISIBLE);
            } else {
                markwon.setMarkdown(aiHolder.tvMessage, content);
                aiHolder.llTrick.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        UserViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTrick;
        View llTrick;
        AIViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTrick = itemView.findViewById(R.id.tvTrick);
            llTrick = itemView.findViewById(R.id.llTrick);
        }
    }
}