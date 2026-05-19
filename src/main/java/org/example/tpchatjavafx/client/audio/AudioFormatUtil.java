package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class AudioFormatUtil {

    public static final AudioFormat NETWORK_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);

    private AudioFormatUtil() {
    }

    public static byte[] convert(byte[] audioData, AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (audioData == null || audioData.length == 0 || sourceFormat == null || targetFormat == null) {
            return audioData;
        }
        if (sameFormat(sourceFormat, targetFormat)) {
            return audioData;
        }

        try {
            int frameSize = Math.max(1, sourceFormat.getFrameSize());
            long frameLength = audioData.length / frameSize;
            try (AudioInputStream sourceStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), sourceFormat, frameLength);
                 AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = convertedStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            return audioData;
        }
    }

    public static boolean sameFormat(AudioFormat a, AudioFormat b) {
        return a != null && b != null
                && a.getSampleRate() == b.getSampleRate()
                && a.getSampleSizeInBits() == b.getSampleSizeInBits()
                && a.getChannels() == b.getChannels()
                && a.isBigEndian() == b.isBigEndian()
                && a.getEncoding().equals(b.getEncoding());
    }
}
