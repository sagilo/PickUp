package com.lowenhardt.pickup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chauthai.swipereveallayout.SwipeRevealLayout;
import com.chauthai.swipereveallayout.ViewBinderHelper;
import com.crashlytics.android.Crashlytics;
import com.lowenhardt.pickup.models.ContactConfig;

import java.util.Collections;
import java.util.List;

import static io.fabric.sdk.android.Fabric.TAG;

public class ContactConfigRecyclerViewAdapter extends RecyclerView.Adapter<ContactConfigRecyclerViewAdapter.ViewHolder> {

    private final LongSparseArray<ContactConfig> pendingDeletionItems = new LongSparseArray<>();
    private final List<ContactConfig> values;
    private final ViewBinderHelper viewBinderHelper = new ViewBinderHelper();
    private final boolean deletionEnabled;
    private final Database database;
    private final Context context;
    private final DeletedItemListener listener;
    private final Handler handler; // handler for running delayed runnables
    private final LongSparseArray<Runnable> pendingRunnables = new LongSparseArray<>();

    ContactConfigRecyclerViewAdapter(List<ContactConfig> values,
                                     boolean deletionEnabled,
                                     Database database,
                                     DeletedItemListener listener,
                                     Context context) {
        viewBinderHelper.setOpenOnlyOne(true);
        this.values = values;
        Collections.sort(this.values);
        this.deletionEnabled = deletionEnabled;
        this.database = database;
        this.context = context;
        this.listener = listener;
        handler = new Handler(Looper.getMainLooper());
    }

    interface DeletedItemListener {
        void reminderDeleted(ContactConfig contactConfig, int undoTimeout, int itemsLeft);
    }

    @Override
    public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_config_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final ContactConfig contactConfig = values.get(position);
        holder.contactConfig = contactConfig;

        viewBinderHelper.bind(holder.swipeLayout, holder.contactConfig.toString());

        holder.name.setText(contactConfig.getName());
        holder.phoneNumber.setText(contactConfig.getPhoneNumber());

        String intervalSummary = context.getString(R.string.contact_config_interval_summary,
                contactConfig.getNumCallsToChangeMode(),
                contactConfig.getCallIntervalMinutes());
        holder.intervalSummary.setText(intervalSummary);
        holder.volume.setText(Integer.toString(contactConfig.getVolumeWhenUnmuted()));

        holder.swipeLayout.close(false);
        holder.swipeLayout.setLockDrag(!deletionEnabled);

        if (deletionEnabled) {
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            database.removeContactConfig(contactConfig.getId(), context);
                            pendingRunnables.remove(contactConfig.getId());
                            pendingDeletionItems.remove(contactConfig.getId());
                        }
                    };

                    int timeout = 3000; //FeatureFlights.getReminderDeletionUndoTimeoutMs();
                    pendingRunnables.append(contactConfig.getId(), runnable);
                    handler.postDelayed(runnable, timeout);
                    Crashlytics.log(Log.INFO, TAG, "Posting handler for reminder deletion, " +
                            "timeoutMs: "+timeout+", id: "+contactConfig.getId());

                    listener.reminderDeleted(contactConfig, timeout-500, values.size()-1);

                    holder.swipeLayout.close(false);
                    pendingDeletionItems.append(contactConfig.getId(), contactConfig);
                    values.remove(position);
                    notifyDataSetChanged();
                }
            });
        }

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ContactConfigActivity.class);
                holder.contactConfig.fillIntent(intent);
                context.startActivity(intent);
            }
        });
    }

    void undoDeletion(ContactConfig reminder) {
        long reminderId = reminder.getId();
        Crashlytics.log(Log.INFO, TAG, "Undo deletion, removing pending runnable, id: "+reminderId);
        pendingDeletionItems.remove(reminder.getId());
        Runnable runnable = pendingRunnables.get(reminderId);
        if (runnable != null) {
            pendingRunnables.remove(reminderId);
            handler.removeCallbacks(runnable);
        }

        values.add(reminder);
        Collections.sort(values);
        notifyDataSetChanged();
    }

    LongSparseArray<ContactConfig> getItemsPendingDeletion() {
        handler.removeCallbacksAndMessages(null);
        return pendingDeletionItems;
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public void saveStates(Bundle outState) {
        viewBinderHelper.saveStates(outState);
    }

    public void restoreStates(Bundle inState) {
        viewBinderHelper.restoreStates(inState);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        SwipeRevealLayout swipeLayout;
        final FrameLayout layout;
        final TextView name;
        final TextView phoneNumber;
        final TextView intervalSummary;
        final TextView volume;
        final View deleteButton;

        ContactConfig contactConfig;

        ViewHolder(View view) {
            super(view);
            swipeLayout = view.findViewById(R.id.contact_config_swipe_layout);
            layout = view.findViewById(R.id.contact_config_item_layout);
            name = view.findViewById(R.id.contact_config_name);
            phoneNumber = view.findViewById(R.id.contact_config_phone_number);
            intervalSummary = view.findViewById(R.id.contact_config_calls_before_change_mode);
            volume = view.findViewById(R.id.contact_config_volume);
            deleteButton = view.findViewById(R.id.item_delete);
        }

        @Override
        public @NonNull String toString() {
            return super.toString() + " '" + name.getText() + "'";
        }
    }
}
