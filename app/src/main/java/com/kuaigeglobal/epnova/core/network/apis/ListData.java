package com.kuaigeglobal.epnova.core.network.apis;

import java.util.List;
import java.util.Objects;

public class ListData<T> {

    private List<T> items;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListData<?> listData = (ListData<?>) o;
        return Objects.equals(items, listData.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
