package com.alertalinkedin;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alertalinkedin.db.KeywordEntity;

import java.util.ArrayList;
import java.util.List;

public class KeywordAdapter extends RecyclerView.Adapter<KeywordAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(KeywordEntity keyword);
    }

    private List<KeywordEntity> keywords = new ArrayList<>();
    private final OnDeleteListener deleteListener;

    public KeywordAdapter(OnDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setKeywords(List<KeywordEntity> keywords) {
        this.keywords = keywords;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_keyword, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeywordEntity kw = keywords.get(position);
        holder.keywordText.setText(kw.value);
        holder.deleteButton.setOnClickListener(v ->
            new AlertDialog.Builder(v.getContext())
                .setTitle("Remover")
                .setMessage("Remover \"" + kw.value + "\"?")
                .setPositiveButton("Sim", (d, w) -> deleteListener.onDelete(kw))
                .setNegativeButton("Não", null)
                .show()
        );
    }

    @Override
    public int getItemCount() {
        return keywords.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView keywordText;
        final ImageButton deleteButton;

        ViewHolder(View view) {
            super(view);
            keywordText = view.findViewById(R.id.keywordText);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}
