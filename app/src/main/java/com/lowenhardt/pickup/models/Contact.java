package com.lowenhardt.pickup.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {

    public String name;
    public String phoneNumber;

    public Contact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phoneNumber);
    }

    private Contact(Parcel in) {
        name = in.readString();
        phoneNumber = in.readString();
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };
}
