package mephi.validation;

public interface ValidationRule<T> {
    ValidationRule<T> setNext(ValidationRule<T> nextRule);

    void validate(T data) throws Exception;
}
