package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerStore {

    private PlayerState state;
    private final List<Consumer<PlayerState>> listeners = new ArrayList<>();

    public void setState(PlayerState newState) {
        this.state = newState;
        notifyListeners();
    }

    public PlayerState getState() {
        return state;
    }

    public void subscribe(Consumer<PlayerState> l) {
        listeners.add(l);
    }

    private void notifyListeners() {
        listeners.forEach(l -> l.accept(state));
    }
}