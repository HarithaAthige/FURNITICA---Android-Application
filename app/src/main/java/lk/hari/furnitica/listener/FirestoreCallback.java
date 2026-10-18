package lk.hari.furnitica.listener;

public interface FirestoreCallback<T> {
    void onCallback(T data);
}
