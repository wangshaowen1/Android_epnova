package com.kuaigeglobal.epnova.core.network.apis.entity;


import android.os.Parcel;
import android.os.Parcelable;

public class UserBasicEntity implements Parcelable {

    /**
     * userToken : 0
     * nickName :
     * avatarUrl :
     * ageDesc :
     * city :
     */

    private String userToken;
    private String nickName;
    private String avatarUrl;
    private String ageDesc;
    private String city;
    private int gender;

    protected UserBasicEntity(Parcel in) {
        userToken = in.readString();
        nickName = in.readString();
        avatarUrl = in.readString();
        ageDesc = in.readString();
        city = in.readString();
        gender = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userToken);
        dest.writeString(nickName);
        dest.writeString(avatarUrl);
        dest.writeString(ageDesc);
        dest.writeString(city);
        dest.writeInt(gender);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserBasicEntity> CREATOR = new Creator<UserBasicEntity>() {
        @Override
        public UserBasicEntity createFromParcel(Parcel in) {
            return new UserBasicEntity(in);
        }

        @Override
        public UserBasicEntity[] newArray(int size) {
            return new UserBasicEntity[size];
        }
    };

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAgeDesc() {
        return ageDesc;
    }

    public void setAgeDesc(String ageDesc) {
        this.ageDesc = ageDesc;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

}
