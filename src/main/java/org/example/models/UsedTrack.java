package org.example.models;

import se.michaelthelin.spotify.model_objects.IPlaylistItem;

public record UsedTrack(IPlaylistItem track, PlaylistModel from) {
}