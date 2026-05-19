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

        if (isMono16LittleEndianPcm(sourceFormat) && isMono16LittleEndianPcm(targetFormat)) {
            return resampleMono16LittleEndian(audioData, sourceFormat.getSampleRate(), targetFormat.getSampleRate());
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

    private static boolean isMono16LittleEndianPcm(AudioFormat format) {
        return format != null
                && AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                && format.getChannels() == 1
                && format.getSampleSizeInBits() == 16
                && !format.isBigEndian();
    }

    private static byte[] resampleMono16LittleEndian(byte[] audioData, float sourceRate, float targetRate) {
        if (audioData == null || audioData.length < 2 || sourceRate <= 0 || targetRate <= 0) {
            return audioData;
        }
        if (Math.abs(sourceRate - targetRate) < 0.0001f) {
            return audioData.clone();
        }

        int sourceSampleCount = audioData.length / 2;
        if (sourceSampleCount <= 0) {
            return audioData;
        }

        short[] sourceSamples = new short[sourceSampleCount];
        for (int i = 0; i < sourceSampleCount; i++) {
            int low = audioData[i * 2] & 0xFF;
            int high = audioData[i * 2 + 1];
            sourceSamples[i] = (short) ((high << 8) | low);
        }

        int targetSampleCount = Math.max(1, Math.round(sourceSampleCount * (targetRate / sourceRate)));
        byte[] result = new byte[targetSampleCount * 2];

        for (int i = 0; i < targetSampleCount; i++) {
            float sourcePosition = i * (sourceRate / targetRate);
            int leftIndex = (int) Math.floor(sourcePosition);
            int rightIndex = Math.min(leftIndex + 1, sourceSampleCount - 1);
            float fraction = sourcePosition - leftIndex;

            short left = sourceSamples[Math.min(leftIndex, sourceSampleCount - 1)];
            short right = sourceSamples[rightIndex];
            int interpolated = Math.round(left + (right - left) * fraction);

            result[i * 2] = (byte) (interpolated & 0xFF);
            result[i * 2 + 1] = (byte) ((interpolated >> 8) & 0xFF);
        }

        return result;
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
