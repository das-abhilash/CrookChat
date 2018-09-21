/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crookchat.videoPlayer;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import com.crookchat.R;

/**
 * Manages the {@link ExoPlayer}, the IMA plugin and all video playback.
 */
/* package */ final class PlayerManager {


    private SimpleExoPlayer player;
    private long contentPosition;
    private String contentUrl;

    PlayerManager(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public void init(Context context, SimpleExoPlayerView simpleExoPlayerView) {
        // Create a default track selector.
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // Create a player instance.
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

        // Bind the player to the view.
        simpleExoPlayerView.setPlayer(player);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getString(R.string.application_name)));

        // Produces Extractor instances for parsing the content media (i.e. not the ad).
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the content media (i.e. not the ad).
//    String contentUrl = context.getString(R.string.content_url);
//        String contentUrl = "https://firebasestorage.googleapis.com/v0/b/crookchat.appspot.com/o/phot%2Fusers%2FZz2jlIJjF5ZBf0bxqPxh2arz9jk21537413957322?alt=media&token=d16db015-a73c-47a7-828c-1b37614e1fa7";
        MediaSource contentMediaSource = new ExtractorMediaSource(
                Uri.parse(contentUrl), dataSourceFactory, extractorsFactory, null, null);

        // Prepare the player with the source.
        player.seekTo(contentPosition);
        player.prepare(contentMediaSource);
        player.setPlayWhenReady(true);
    }

    public void reset() {
        if (player != null) {
            contentPosition = player.getContentPosition();
            player.release();
            player = null;
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

}
