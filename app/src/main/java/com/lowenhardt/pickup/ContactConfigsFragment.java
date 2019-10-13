package com.lowenhardt.pickup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.lowenhardt.pickup.models.ContactConfig;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ContactConfigsFragment extends Fragment implements ContactConfigRecyclerViewAdapter.DeletedItemListener {

    private static final String TAG = ContactConfigsFragment.class.getSimpleName();
    private static final String ARG_COLUMN_COUNT = "column-count";
    private int columnCount = 1;
    private ContactConfigRecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private View noRemindersView;
    private FloatingActionButton fab;
    private Database database;
    private Snackbar snackbar;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactConfigsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new Database(getContext());

        if (getArguments() != null) {
            columnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removePendingDeletionReminder();
    }

    private void removePendingDeletionReminder() {
        if (adapter == null) {
            return;
        }
        final LongSparseArray<ContactConfig> remindersPendingDeletion = adapter.getItemsPendingDeletion();
        if (remindersPendingDeletion.size() == 0) {
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            Crashlytics.logException(new Exception(TAG+" | null activity on ContactConfigsFragment onDestroy, " +
                    "can't delete pending deletion reminders"));
            return;
        }

        if (snackbar != null) {
            snackbar.dismiss();
        }

        final Context applicationContext = activity.getApplicationContext();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Don't use the database from the fragment members
                Database database = new Database(applicationContext);
                List<Long> idsToDelete = new ArrayList<>();
                for(int i = 0; i < remindersPendingDeletion.size(); i++) {
                    idsToDelete.add(remindersPendingDeletion.keyAt(i));
                }
                database.removeContactConfigs(idsToDelete, getContext());
            }
        };
        new Handler().post(runnable);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_contact_configs, container, false);

        noRemindersView = layout.findViewById(R.id.list_no_items);
        recyclerView = layout.findViewById(R.id.contact_configs_list);
        fab = layout.findViewById(R.id.add_item_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), ContactConfigActivity.class));
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
                if (dy<0 && !fab.isShown())
                    fab.show();
                else if(dy>0 && fab.isShown())
                    fab.hide();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                LinearLayout.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);


        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        RemindersLoader remindersLoader = new RemindersLoader(this);
        remindersLoader.execute(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle(getString(R.string.contact_configs));
        }
    }

    private void setReminders(List<ContactConfig> contactConfigs) {
        if (contactConfigs.isEmpty()) {
            noRemindersView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noRemindersView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            Context context = recyclerView.getContext();
            if (columnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, columnCount));
            }

            adapter = new ContactConfigRecyclerViewAdapter(contactConfigs,
                    true, // FeatureFlights.isRemindersDeletionEnabled(),
                    database,
                    this,
                    getContext());
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void reminderDeleted(final ContactConfig contactConfig, int undoTimeout, int itemsLeft) {
        Activity activity = getActivity();
        if (activity == null) {
            Crashlytics.logException(new Exception(TAG+ " | null activity on reminderDeleted CB"));
            return;
        }

        String name = contactConfig.getName();
        View view = activity.findViewById(R.id.contact_configs_list_container);
        String text = view.getContext().getString(R.string.x_deleted, name);
        snackbar = Snackbar.make(view, text, undoTimeout)
                .setAction(view.getContext().getString(R.string.undo), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        adapter.undoDeletion(contactConfig);
                        noRemindersView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
        snackbar.show();

        if (itemsLeft == 0) {
            noRemindersView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noRemindersView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    static class RemindersLoader extends AsyncTask<Context, Void, List<ContactConfig>> {

        private WeakReference<ContactConfigsFragment> fragment;

        RemindersLoader(ContactConfigsFragment contactConfigsFragment) {
            fragment = new WeakReference<>(contactConfigsFragment);
        }

        @Override
        protected List<ContactConfig> doInBackground(Context... contexts) {
            Database database = new Database(contexts[0]);
            return database.getAllContactConfigs(contexts[0]);
        }

        @Override
        protected void onPostExecute(List<ContactConfig> scheduledReminders) {
            super.onPostExecute(scheduledReminders);
            if (fragment == null || fragment.get() == null) {
                return;
            }
            fragment.get().setReminders(scheduledReminders);
        }
    }
}
