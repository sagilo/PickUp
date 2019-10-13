package com.lowenhardt.pickup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.lowenhardt.pickup.models.Contact;
import com.lowenhardt.pickup.models.ContactConfig;
import com.lowenhardt.pickup.views.CustomAutoCompleteView;
import com.warkiz.widget.IndicatorSeekBar;

import java.util.ArrayList;


public class ContactConfigActivity extends AppCompatActivity implements AsyncQueryContacts.QueryCB {

    private static final String TAG = ContactConfigActivity.class.getSimpleName();

    View mRootView;
    boolean loadedExistingContactConfig = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_config);

        final Intent intent = getIntent();

        Crashlytics.log(Log.DEBUG, TAG, "Creating ContactConfigActivity");

        ContactConfigDialogFragment fragment = ContactConfigDialogFragment.newInstance(intent.getExtras());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, fragment, "contact_config_gragment");
        ft.commit();

        mRootView = findViewById(R.id.newReminderRoot);
    }

    @Override
    public void onBackPressed() {
        ContactConfigDialogFragment fragment = getFragment();
        if (fragment == null || !fragment.hasChanged()) {
            onSuperBackPressed();
            return;
        }

        String title;
        String message;
        String negative;
        if (loadedExistingContactConfig) {
            title = getString(R.string.discard_changes);
            message = getString(R.string.contact_config_changes_wont_be_saved);
            negative = getString(R.string.yes_discard);
        } else {
            title = getString(R.string.delete_config);
            message = getString(R.string.contact_config_changes_wont_be_saved);
            negative = getString(R.string.yes_delete);
        }

        new MaterialDialog.Builder(this)
                .title(title)
                .content(message)
                .cancelable(true)
                .negativeText(negative)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                        ContactConfigActivity.this.onSuperBackPressed();
                    }
                })
                .positiveText(R.string.no_go_back)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.cancel();
                    }
                })
                .show();
    }

    private void onSuperBackPressed(){
        super.onBackPressed();
    }

    @Override
    public void onSuccess(ArrayList<Contact> contacts) {

    }

    @Override
    public void onFailure() {

    }

    public static class ContactConfigDialogFragment extends DialogFragment implements AsyncQueryContacts.QueryCB {
        private final String TAG = ContactConfigDialogFragment.class.getSimpleName();

        View rootView;
        View layoutContainer;
        View progressBarLayout;
        EditText nameET;
        EditText phoneNumberET;
        IndicatorSeekBar callsIntervalSB;
        IndicatorSeekBar numCallsBeforeModeChangeSB;
        IndicatorSeekBar volumeWhenUnmutedSB;

        ArrayAdapter<Contact> autoCompleteArrayAdapter;
        CustomAutoCompleteView contactsTextView;

        long loadedContactConfigId = -1;

        static ContactConfigDialogFragment newInstance(Bundle bundle) {
            ContactConfigDialogFragment fragment = new ContactConfigDialogFragment();
            if (bundle != null) {
                fragment.setArguments(bundle);
            }
            return fragment;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            final ContactConfigActivity activity = (ContactConfigActivity) getActivity();
            if (activity == null) {
                Crashlytics.log(Log.WARN, TAG, "null activity in onCreateView()");
                return null;
            }

            rootView = inflater.inflate(R.layout.dialog_contact_config, container, false);

            layoutContainer = rootView.findViewById(R.id.contactConfigContainer);
            progressBarLayout = rootView.findViewById(R.id.progressBar);

            showProgressBar(true);

            new AsyncQueryContacts(activity, this).execute();

            nameET = rootView.findViewById(R.id.contactConfigNameEditText);
            nameET.requestFocus();

            phoneNumberET = rootView.findViewById(R.id.contactConfigPhoneNumberEditText);
            callsIntervalSB = rootView.findViewById(R.id.callsIntervalMinutesSeekBar);
            numCallsBeforeModeChangeSB = rootView.findViewById(R.id.numCallsToChangeModeSeekBar);
            volumeWhenUnmutedSB = rootView.findViewById(R.id.volumeWhenUnmutedSeekBar);

            Toolbar toolbar = rootView.findViewById(R.id.toolbar);
            toolbar.setTitle(R.string.new_contact_config);
            activity.setSupportActionBar(toolbar);

            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeButtonEnabled(true);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_close_24dp);
            }

            setHasOptionsMenu(true);

            Log.d(TAG, "Starting ContactConfigDialogFragment");

            ContactConfig contactConfig = ContactConfig.fromIntent(getArguments());
            if (contactConfig != null) {
                loadedContactConfigId = contactConfig.getId();
                populateContactConfigInViews(contactConfig);
                activity.loadedExistingContactConfig = true;
            }

            return rootView;
        }

        private void showProgressBar(boolean show) {
            if (show) {
                Crashlytics.log(Log.INFO, TAG, "Showing progress bar");
                layoutContainer.setVisibility(View.GONE);
                progressBarLayout.setVisibility(View.VISIBLE);
            } else {
                Crashlytics.log(Log.INFO, TAG, "Hiding progress bar");
                layoutContainer.setVisibility(View.VISIBLE);
                progressBarLayout.setVisibility(View.GONE);
            }
        }

        private void populateContactConfigInViews(ContactConfig contactConfig) {
            nameET.setText(contactConfig.getName());
            phoneNumberET.setText(contactConfig.getPhoneNumber());
            callsIntervalSB.setProgress(contactConfig.getCallIntervalMinutes());
            numCallsBeforeModeChangeSB.setProgress(contactConfig.getNumCallsToChangeMode());
            volumeWhenUnmutedSB.setProgress(contactConfig.getVolumeWhenUnmuted());
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = super.onCreateDialog(savedInstanceState);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            return dialog;
        }

        @Override
        public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
            menu.clear();
            Activity activity = getActivity();
            if (activity != null) {
                activity.getMenuInflater().inflate(R.menu.menu_contact_config, menu);
            }
        }

        boolean hasChanged() {
            return !Utils.getString(nameET).isEmpty() || !Utils.getString(phoneNumberET).isEmpty();
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_save) {
                save();
                return true;
            } else if (id == android.R.id.home) { // x button
                Activity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                }

                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        void initContactsAutoComplete(ArrayList<Contact> contacts) {
            autoCompleteArrayAdapter = new AutoCompleteArrayAdapter(getActivity(), R.layout.list_item_contact, contacts);
            contactsTextView = rootView.findViewById(R.id.contactConfigNameEditText);
            contactsTextView.setThreshold(2); // start from 2nd character
            contactsTextView.setAdapter(autoCompleteArrayAdapter);
            CustomAutoCompleteTextChangedListener textChangedListener =
                    new CustomAutoCompleteTextChangedListener(contactsTextView, contacts, getActivity());
            contactsTextView.addTextChangedListener(textChangedListener);
            contactsTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                    Contact contact = (Contact)parent.getItemAtPosition(pos);
                    contactsTextView.setText(contact.name);
                    phoneNumberET.setText(contact.phoneNumber);
                }
            });
        }

        private void save() {
            Activity activity = getActivity();
            if (activity == null) {
                Crashlytics.log(Log.WARN, TAG, "Activity not attached (null activity)");
                showMessage(R.string.error_occurred_contact_dev, null);
                return;
            }

            String name = Utils.getString(nameET);
            if (name.isEmpty()) {
                showMessage(R.string.new_contact_config_missing_name, activity);
                return;
            }

            String phoneNumber = Utils.getString(phoneNumberET);
            if (phoneNumber.isEmpty()) {
                showMessage(R.string.new_contact_config_missing_phone_number, activity);
                return;
            }

            int intervalMinutes = callsIntervalSB.getProgress();
            int numCallsBeforeChangingMode = numCallsBeforeModeChangeSB.getProgress();
            int volumeWhenUnmuted = volumeWhenUnmutedSB.getProgress();

            ContactConfig contactConfig = new ContactConfig(loadedContactConfigId,
                    name,
                    phoneNumber,
                    intervalMinutes * 60,
                    numCallsBeforeChangingMode,
                    volumeWhenUnmuted,
                    null);

            Database db = new Database(activity);
            contactConfig = db.addOrUpdate(contactConfig, activity);
            Crashlytics.log(Log.INFO, TAG, "Added new ContactConfig to DB: " + contactConfig);

            activity.finish();
        }

        void showMessage(int message, Activity activity) {
            showMessage(getString(message), activity);
        }

        void showMessage(String message, Activity activity) {
            if (activity != null) {
                hideKeyboard(activity);
            }
            final Snackbar sb = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
            sb.show();
        }

        private void hideKeyboard(Activity activity) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager == null) {
                return;
            }
            inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getRootView().getWindowToken(), 0);
        }

        @Override
        public void onSuccess(ArrayList<Contact> contacts) {
            Crashlytics.log(Log.INFO, TAG, "Query contacts succeeded, setting in layout");
            initContactsAutoComplete(contacts);
            showProgressBar(false);
        }

        @Override
        public void onFailure() {
            showProgressBar(false);
        }

        private class AutoCompleteArrayAdapter extends ArrayAdapter<Contact> {

            Context mContext;
            int layoutResourceId;
            ArrayList<Contact> data;

            private AutoCompleteArrayAdapter(Context mContext, int layoutResourceId, ArrayList<Contact> data) {
                super(mContext, layoutResourceId, data);

                this.layoutResourceId = layoutResourceId;
                this.mContext = mContext;
                this.data = data;
            }

            @Override
            public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent) {
                try{
                    if (convertView == null){
                        // inflate the layout
                        LayoutInflater inflater = getActivity().getLayoutInflater();
                        convertView = inflater.inflate(layoutResourceId, parent, false);
                    }

                    // object item based on the position
                    Contact contact = data.get(position);

                    TextView name = convertView.findViewById(R.id.contact_list_item_name);
                    name.setText(contact.name);

                    TextView email = convertView.findViewById(R.id.contact_list_item_phone_number);
                    email.setText(contact.phoneNumber);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return convertView;
            }
        }

        private class CustomAutoCompleteTextChangedListener implements TextWatcher {

            Context context;
            AutoCompleteTextView autoCompleteTextView;
            ArrayList<Contact> allContacts;

            CustomAutoCompleteTextChangedListener(AutoCompleteTextView textView, ArrayList<Contact> allContacts, Context context){
                this.autoCompleteTextView = textView;
                this.allContacts = allContacts;
                this.context = context;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence userInput, int start, int before, int count) {
                if (autoCompleteTextView.isPerformingCompletion()) { // if it was item click
                    return;
                }
                try{
                    // update the adapter
                    autoCompleteArrayAdapter.notifyDataSetChanged();
                    // filter items
                    ArrayList<Contact> filteredList = filterItems(userInput.toString().toLowerCase());
                    // update the adapter
                    autoCompleteArrayAdapter = new AutoCompleteArrayAdapter(context, R.layout.list_item_contact, filteredList);
                    autoCompleteTextView.setAdapter(autoCompleteArrayAdapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private ArrayList<Contact> filterItems(String userInput) {
                String userInputLower = userInput.toLowerCase();
                ArrayList<Contact> filtered = new ArrayList<>();
                for (Contact contact : allContacts) {
                    if (contact.name.toLowerCase().startsWith(userInputLower) ||
                            contact.phoneNumber.toLowerCase().startsWith(userInputLower)) {
                        filtered.add(contact);
                    }
                }
                return filtered;
            }
        }
    }

    ContactConfigDialogFragment getFragment(){
        Fragment f = getSupportFragmentManager().findFragmentByTag("contact_config_gragment");
        if(f instanceof ContactConfigDialogFragment) {
            return (ContactConfigDialogFragment) f;
        }

        return null;
    }
}
