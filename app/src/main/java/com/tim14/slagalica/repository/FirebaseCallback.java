package com.tim14.slagalica.repository;
public interface FirebaseCallback<T> {
    void onSuccess(T result);
    void onError(String error);
}