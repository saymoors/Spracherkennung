package mephi.validation;

public abstract class BaseValidationRule<T> implements ValidationRule<T> {
    private ValidationRule<T> nextRule;

    @Override
    public ValidationRule<T> setNext(ValidationRule<T> nextRule) {
        this.nextRule = nextRule;
        return nextRule;
    }

    @Override
    public void validate(T data) throws Exception {
        check(data);

        if (nextRule != null) {
            nextRule.validate(data);
        }
    }

    protected abstract void check(T data) throws Exception;
}
