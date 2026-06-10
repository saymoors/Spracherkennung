package mephi.enums;

public enum AudioFormat {
    MP3("MP3", "audio/mpeg", "MP3"),
    WAV("WAV", "audio/x-pcm;bit=16", "PCM_S16LE"),
    FLAC("FLAC", "audio/flac", "FLAC"),
    OGG("OGG", "audio/ogg", "OPUS");

    private final String extension;
    private final String contentType;
    private final String sberAudioEncoding;

    AudioFormat(String extension, String contentType, String sberAudioEncoding) {
        this.extension = extension;
        this.contentType = contentType;
        this.sberAudioEncoding = sberAudioEncoding;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSberAudioEncoding() {
        return sberAudioEncoding;
    }

    public static boolean isSupported(String extension) {
        for (AudioFormat audioFormat : values()) {
            if (audioFormat.extension.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    public static AudioFormat fromExtension(String extension) throws Exception {
        for (AudioFormat audioFormat : values()) {
            if (audioFormat.extension.equals(extension)) {
                return audioFormat;
            }
        }

        throw new Exception("Неподдерживаемый формат файла");
    }
}
