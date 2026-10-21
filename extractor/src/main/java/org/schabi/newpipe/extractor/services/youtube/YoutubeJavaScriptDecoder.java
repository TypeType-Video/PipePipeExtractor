package org.schabi.newpipe.extractor.services.youtube;

import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface YoutubeJavaScriptDecoder {
    @Nonnull
    PlayerData getPlayerData(@Nonnull String videoId) throws ParsingException;

    @Nonnull
    default PlayerData getPlayerData(@Nonnull final String videoId,
                                     @Nullable final String clientName)
            throws ParsingException {
        return getPlayerData(videoId);
    }

    @Nonnull
    YoutubeApiDecoder.BatchDecodeResult decodeBatch(
            @Nonnull String playerId,
            @Nullable List<String> signatures,
            @Nullable List<String> throttlingParameters) throws ParsingException;

    @Nonnull
    default YoutubeApiDecoder.BatchDecodeResult decodeBatch(
            @Nonnull final String playerId,
            @Nullable final String clientName,
            @Nullable final List<String> signatures,
            @Nullable final List<String> throttlingParameters) throws ParsingException {
        return decodeBatch(playerId, signatures, throttlingParameters);
    }

    final class PlayerData {
        @Nonnull
        private final String playerId;
        private final int signatureTimestamp;

        public PlayerData(@Nonnull final String playerId, final int signatureTimestamp) {
            this.playerId = playerId;
            this.signatureTimestamp = signatureTimestamp;
        }

        @Nonnull
        public String getPlayerId() {
            return playerId;
        }

        public int getSignatureTimestamp() {
            return signatureTimestamp;
        }
    }
}
