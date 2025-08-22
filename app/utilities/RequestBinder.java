package utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A lightweight binder that bypasses Play's built-in validation,
 * maps request data into a POJO, and applies custom validation (JSR-303).
 */
public class RequestBinder {

    private final FormFactory formFactory;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Inject
    public RequestBinder(FormFactory formFactory,
                         ObjectMapper objectMapper,
                         Validator validator) {
        this.formFactory = formFactory;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> BindingResult<T> bind(Http.Request request, Class<T> clazz) {
        DynamicForm form = formFactory.form().bindFromRequest(request);

        Map<String, String> data = new HashMap<>();
        form.rawData().forEach(data::put);

        T instance = objectMapper.convertValue(data, clazz);

        Set<ConstraintViolation<T>> violations = validator.validate(instance);

        return new BindingResult<>(instance, violations);
    }
}

/**
 * public Result saveInstitution(Http.Request request) {
 *     BindingResult<FinancialInstitution> result = binder.bind(request, FinancialInstitution.class);
 *
 *     if (result.hasErrors()) {
 *         return badRequest("Validation failed: " + result.getViolations());
 *     }
 *
 *     FinancialInstitution fi = result.getInstance();
 *     return ok("Saved: " + fi.getName());
 * }
 */
