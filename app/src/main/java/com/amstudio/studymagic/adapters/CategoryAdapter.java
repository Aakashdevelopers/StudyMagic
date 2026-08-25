package com.amstudio.studymagic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amstudio.studymagic.R;
import com.amstudio.studymagic.models.Category;
import com.squareup.picasso./**/Picasso;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<Category> categories;
    private final OnCategoryClickListener listener;
    private int layoutId = R.layout.item_category;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public CategoryAdapter(List<Category> categories, int layoutId, OnCategoryClickListener listener) {
        this.categories = categories;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());
        
        if (category.getIconUrl() != null && !category.getIconUrl().isEmpty()) {
            Picasso.get()
                    .load(category.getIconUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageResource(category.getIconRes() != 0 ? category.getIconRes() : R.drawable.ic_launcher_background);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
        }
    }
}