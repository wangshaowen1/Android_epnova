package com.nhsd.mattise.internal.ui.livedata;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nhsd.mattise.internal.entity.Album;

public class AlbumViewModel extends ViewModel {


    private final MutableLiveData<Album> albumData = new MutableLiveData<>();
    private final MutableLiveData<Integer> refreshEvent = new MutableLiveData<>();

    public void refresh() {
        Integer value = refreshEvent.getValue();
        if (value == null) {
            refreshEvent.setValue(0);
            return;
        }
        refreshEvent.setValue(value + 1);
    }

    public MutableLiveData<Album> getAlbumData() {
        return albumData;
    }

    public MutableLiveData<Integer> getRefreshEvent() {
        return refreshEvent;
    }

    public void setAlbumData(Album album) {
        this.albumData.setValue(album);
    }
}
