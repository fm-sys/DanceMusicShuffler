package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractStore<T> {
    private T state;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public AbstractStore() {
        state = defaultState();
    }

    protected abstract T defaultState();

    public void setState(T newState) {
        this.state = newState;
        notifyListeners();
    }

    public T get() {
        return state;
    }

    public void subscribe(Consumer<T> l) {
        listeners.add(l);
    }

    protected void notifyListeners() {
        listeners.forEach(l -> l.accept(state));
    }
}
