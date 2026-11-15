package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SabrReloadPlayerResponseTest {
    private static final String TOKEN =
            "Cj8aAwoBASILa0lvMkJBdWJPNmsqJDNpMnUvSzlvemdseURUUy9qZ1NtSklnVUFEcjNveDFWcTU0djCQteCV"
                    + "njQSJQEyjDJ5oJTjWD6MZq1_Q_dvW6YDrTBPJh4ed9vXps3v80eL_R4%3D";

    @Test
    void decodesTheTokenFromReloadPlaybackParams() throws Exception {
        final SabrReloadPlayerResponse response = SabrReloadPlayerResponse.decode(reloadPart());

        assertEquals(TOKEN, response.getReloadPlaybackParamsToken());
    }

    @Test
    void reloadPartRequestsReloadWithoutMalformedParts() throws Exception {
        final byte[] data = reloadPart();
        final SabrDecodedResponse decoded = SabrResponseDecoder.decodeParts(List.of(
                new UmpPart(SabrResponseDecoder.RELOAD_PLAYER_RESPONSE, data.length, data)));

        assertTrue(decoded.isReloadRequested());
        assertTrue(decoded.getMalformedParts().isEmpty());
        assertEquals(TOKEN, decoded.getReloadPlayerResponse().getReloadPlaybackParamsToken());
    }

    @Test
    void missingParamsLeaveTheTokenEmpty() throws Exception {
        assertNull(SabrReloadPlayerResponse.decode(new byte[0]).getReloadPlaybackParamsToken());
    }

    @Nonnull
    private static byte[] reloadPart() {
        final SabrProto.Writer params = new SabrProto.Writer();
        params.writeBytes(1, TOKEN.getBytes(StandardCharsets.UTF_8));
        final SabrProto.Writer part = new SabrProto.Writer();
        part.writeMessage(1, params.toByteArray());
        return part.toByteArray();
    }
}
