package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.SearchResult;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private List<SearchResult> items;
    private OnSearchResultClickListener listener;

    public interface OnSearchResultClickListener {
        void onSearchResultClick(SearchResult result);
    }

    public SearchAdapter(List<SearchResult> items, OnSearchResultClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult item = items.get(position);
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.tvSubtitle.setText(item.getSubtitle() != null ? item.getSubtitle() : "");

        if (item.getType() == SearchResult.Type.TEST) {
            holder.ivIcon.setImageResource(R.drawable.ic_exam);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
            holder.tvTypeBadge.setText("TEST");
            holder.tvTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        } else if (item.getType() == SearchResult.Type.NOTE) {
            holder.ivIcon.setImageResource(R.drawable.ic_notes);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSecondary));
            holder.tvTypeBadge.setText("NOTE");
            holder.tvTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSecondary));
        } else if (item.getType() == SearchResult.Type.CATEGORY) {
            holder.ivIcon.setImageResource(R.drawable.ic_home);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSuccess));
            holder.tvTypeBadge.setText("CATEGORY");
            holder.tvTypeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSuccess));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSearchResultClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public void updateList(List<SearchResult> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvSubtitle, tvTypeBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivItemIcon);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvSubtitle = itemView.findViewById(R.id.tvItemSubtitle);
            tvTypeBadge = itemView.findViewById(R.id.tvTypeBadge);
        }
    }
}
