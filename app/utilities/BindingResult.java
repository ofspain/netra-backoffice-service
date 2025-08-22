package utilities;
import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.Set;

public class BindingResult<T> {

    private final T instance;
    private final Set<? extends ConstraintViolation<T>> violations;

    public BindingResult(T instance, Set<? extends ConstraintViolation<T>> violations) {
        this.instance = instance;
        this.violations = violations == null ? Collections.emptySet() : violations;
    }

    public T getInstance() {
        return instance;
    }

    public boolean hasErrors() {
        return !violations.isEmpty();
    }

    public Set<? extends ConstraintViolation<T>> getViolations() {
        return violations;
    }
}
