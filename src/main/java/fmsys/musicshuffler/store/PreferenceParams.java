package fmsys.musicshuffler.store;

public record PreferenceParams(int count, int cooldown, boolean groupPlaylists, boolean showSidePanel, boolean showCover, boolean showBackground) {
}