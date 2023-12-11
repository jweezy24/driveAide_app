package com.example.driveaide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class CustomRecyclerViewAdapter extends RecyclerView.Adapter<CustomRecyclerViewAdapter.ViewHolder> {
    // List of data to be displayed in the RecyclerView.
    private List<ItemData> list;
    // Presumably declared elsewhere in the Adapter class.
    private Context context;

    CustomRecyclerViewAdapter(List<ItemData> list, Context context) {
        this.list = list;
        this.context = context;
    }

    // This method is called when a new ViewHolder gets created.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the single_unit layout and initializing a new ViewHolder instance.
        View view = LayoutInflater.from(context).inflate(R.layout.single_unit, parent, false);
        return new ViewHolder(view);
    }

    // This method binds the data to the ViewHolder.
    @NonNull
    @Override
    public void onBindViewHolder(@NonNull CustomRecyclerViewAdapter.ViewHolder holder, int position) {
        // Setting the name and value in the TextViews from the itemData instance at this position.
        holder.name_view.setText(list.get(position).itemName);
        holder.val_view.setText(list.get(position).itemValue);
    }

    // This method returns the size of the data set.
    @Override
    public int getItemCount() {
        return list.size();
    }

    // ViewHolder class represents the UI components (i.e., Views) of a single list item in the RecyclerView.
    class ViewHolder extends RecyclerView.ViewHolder {
        // TextView to display the name.
        private TextView name_view;
        // TextView to display the value.
        private TextView val_view;

        // Initializing the TextViews.
        ViewHolder(View itemView) {
            super(itemView);
            name_view = itemView.findViewById(R.id.item_name);
            val_view = itemView.findViewById(R.id.item_value);
        }
    }
}
