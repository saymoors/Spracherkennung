package mephi.audio;

public enum RecognitionLanguage {
    RUSSIAN("ru-RU"),
    ENGLISH("en-US");

    public static final String DEFAULT_CODE = "ru-RU";

    private final String code;

    RecognitionLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean isSupported(String code) {
        for (RecognitionLanguage language : values()) {
            if (language.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}
