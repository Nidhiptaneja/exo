package com.example.exoplayer;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.widget.TextView;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.util.Assertions;

import java.util.Locale;

public class AnalyticsTextViewHelper implements AnalyticsListener, Runnable
{
    private static final int REFRESH_INTERVAL_MS = 1;
    private final SimpleExoPlayer player;
    private final TextView textView;
    private boolean started;
    private AnalyticsListener.EventTime eventTime;
    private long currentBandwidth=0;
    private int currentCounter=0;


    public AnalyticsTextViewHelper(SimpleExoPlayer player, TextView textView)
    {
            Assertions.checkArgument(player.getApplicationLooper() == Looper.getMainLooper());
            this.player = player;
            this.textView = textView;
    }
    public final void start() {
        if (started) {
            return;
        }
        started = true;
        player.addAnalyticsListener(this);
        updateAndPost();
    }

    /**
     * Stops periodic updates of the {@link TextView}. Must be called from the application's main
     * thread.
     */
    public final void stop() {
        if (!started) {
            return;
        }
        started = false;
        player.removeAnalyticsListener(this);
        textView.removeCallbacks(this);
    }

    // Player.EventListener implementation.

    @Override
    public void onVideoSizeChanged(
            EventTime eventTime,
            int width,
            int height,
            int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {

        currentCounter++;
    }

    @Override
    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate)
    {
        currentBandwidth = bitrateEstimate / 1024;
    }
    // Runnable implementation.

    @Override
    public final void run() {
        updateAndPost();
    }

    // Protected methods.

    @SuppressLint("SetTextI18n")
    protected final void updateAndPost() {
        textView.setText(getDebugString());
        textView.removeCallbacks(this);
        textView.postDelayed(this, REFRESH_INTERVAL_MS);
    }

    /**
     * Returns the debugging information string to be shown by the target {@link TextView}.
     */
    protected String getDebugString() {
        return getPlayerStateString() + getVideoString() + getAudioString() + getCustomString();
    }

    protected String getCustomString() {
        return String.format("QOE Values Bandwidth %s, Switch %s", currentBandwidth, currentCounter);
    }

    /**
     * Returns a string containing player state debugging information.
     */
    protected String getPlayerStateString() {
        String playbackStateString;
        switch (player.getPlaybackState()) {
            case Player.STATE_BUFFERING:
                playbackStateString = "buffering";
                break;
            case Player.STATE_ENDED:
                playbackStateString = "ended";
                break;
            case Player.STATE_IDLE:
                playbackStateString = "idle";
                break;
            case Player.STATE_READY:
                playbackStateString = "ready";
                break;
            default:
                playbackStateString = "unknown";
                break;
        }
        return String.format(
                "playWhenReady:%s playbackState:%s window:%s",
                player.getPlayWhenReady(), playbackStateString, player.getCurrentWindowIndex());
    }

    /**
     * Returns a string containing video debugging information.
     */
    protected String getVideoString() {
        Format format = player.getVideoFormat();
        DecoderCounters decoderCounters = player.getVideoDecoderCounters();
        if (format == null || decoderCounters == null) {
            return "";
        }
        return "\n"
                + format.sampleMimeType
                + "(id:"
                + format.id
                + " r:"
                + format.width
                + "x"
                + format.height
                + getPixelAspectRatioString(format.pixelWidthHeightRatio)
                + getDecoderCountersBufferCountString(decoderCounters)
                + ")";
    }

    /**
     * Returns a string containing audio debugging information.
     */
    protected String getAudioString() {
        Format format = player.getAudioFormat();
        DecoderCounters decoderCounters = player.getAudioDecoderCounters();
        if (format == null || decoderCounters == null) {
            return "";
        }
        return "\n"
                + format.sampleMimeType
                + "(id:"
                + format.id
                + " hz:"
                + format.sampleRate
                + " ch:"
                + format.channelCount
                + getDecoderCountersBufferCountString(decoderCounters)
                + ")";
    }

    private static String getDecoderCountersBufferCountString(DecoderCounters counters) {
        if (counters == null) {
            return "";
        }
        counters.ensureUpdated();
        return " sib:" + counters.skippedInputBufferCount
                + " sb:" + counters.skippedOutputBufferCount
                + " rb:" + counters.renderedOutputBufferCount
                + " db:" + counters.droppedBufferCount
                + " mcdb:" + counters.maxConsecutiveDroppedBufferCount
                + " dk:" + counters.droppedToKeyframeCount;
    }

    private static String getPixelAspectRatioString(float pixelAspectRatio) {
        return pixelAspectRatio == Format.NO_VALUE || pixelAspectRatio == 1f ? ""
                : (" par:" + String.format(Locale.US, "%.02f", pixelAspectRatio));
    }

}




