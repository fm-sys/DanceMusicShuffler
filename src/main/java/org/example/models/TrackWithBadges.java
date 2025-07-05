package org.example.models;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;

import java.util.List;

public record TrackWithBadges(IPlaylistItem track, List<String> badges) {
}