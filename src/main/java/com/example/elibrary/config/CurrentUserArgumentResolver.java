package com.example.elibrary.config;

import com.example.elibrary.exception.ApiException;
import com.example.elibrary.exception.ErrorCode;
import com.example.elibrary.exception.ErrorResponse.FieldViolation;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

/**
 * Resolves {@link CurrentUserId} from the {@code X-User-Id} request header.
 * This is an identity simulation for the assignment, not authentication.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER_NAME = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String raw = webRequest.getHeader(HEADER_NAME);
        if (raw == null || raw.isBlank()) {
            throw validationError("must be provided");
        }
        long userId;
        try {
            userId = Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw validationError("must be a positive integer");
        }
        if (userId <= 0) {
            throw validationError("must be a positive integer");
        }
        return userId;
    }

    private ApiException validationError(String reason) {
        return new ApiException(ErrorCode.VALIDATION_ERROR,
                "The " + HEADER_NAME + " header " + reason + ".",
                List.of(new FieldViolation(HEADER_NAME, reason)));
    }
}
