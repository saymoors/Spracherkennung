package mephi.enums;

public enum TranscriptionLanguage {
    RU_RU("ru-RU"),
    EN_US("en-US");

    public static final String DEFAULT_CODE = "ru-RU";

    private final String code;

    TranscriptionLanguage(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static boolean isSupported(String code) {
        for (TranscriptionLanguage language : values()) {
            if (language.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static TranscriptionLanguage fromCode(String code) throws Exception {
        for (TranscriptionLanguage language : values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }

        throw new Exception("Неподдерживаемый язык распознавания");
    }
}
