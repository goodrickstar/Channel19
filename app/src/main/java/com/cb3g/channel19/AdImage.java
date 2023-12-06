package com.cb3g.channel19;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AdImage implements Serializable, Parcelable {
    @SerializedName("url")
    @Expose
    private String url;
    public final static Parcelable.Creator<AdImage> CREATOR = new Creator<AdImage>() {
        public AdImage createFromParcel(Parcel in) {
            return new AdImage(in);
        }

        public AdImage[] newArray(int size) {
            return (new AdImage[size]);
        }
    };
    private final static long serialVersionUID = -8976650109064996664L;

    protected AdImage(Parcel in) {
        this.url = ((String) in.readValue((String.class.getClassLoader())));
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(url);
    }

    public int describeContents() {
        return 0;
    }
}