package org.example;

public class PreferencesStore extends AbstractStore<PreferenceParams> {
    @Override
    protected PreferenceParams defaultState() {
        return new PreferenceParams(10, 3, false, true, true, true);
    }
}
