package fmsys.musicshuffler.store;

public class PreferencesStore extends AbstractStore<PreferenceParams> {
    @Override
    protected PreferenceParams defaultState() {
        return new PreferenceParams(10, 3, false, true, true, true);
    }

    public void updateCount(int count) {
        setState(new PreferenceParams(count, get().cooldown(), get().groupPlaylists(), get().showSidePanel(), get().showCover(), get().showBackground()));
    }

    public void updateCooldown(int cooldown) {
        setState(new PreferenceParams(get().count(), cooldown, get().groupPlaylists(), get().showSidePanel(), get().showCover(), get().showBackground()));
    }

    public void updateGroupPlaylists(boolean groupPlaylists) {
        setState(new PreferenceParams(get().count(), get().cooldown(), groupPlaylists, get().showSidePanel(), get().showCover(), get().showBackground()));
    }

    public void updateShowSidePanel(boolean showSidePanel) {
        setState(new PreferenceParams(get().count(), get().cooldown(), get().groupPlaylists(), showSidePanel, get().showCover(), get().showBackground()));
    }

    public void updateShowCover(boolean showCover) {
        setState(new PreferenceParams(get().count(), get().cooldown(), get().groupPlaylists(), get().showSidePanel(), showCover, get().showBackground()));
    }

    public void updateShowBackground(boolean showBackground) {
        setState(new PreferenceParams(get().count(), get().cooldown(), get().groupPlaylists(), get().showSidePanel(), get().showCover(), showBackground));
    }
}
