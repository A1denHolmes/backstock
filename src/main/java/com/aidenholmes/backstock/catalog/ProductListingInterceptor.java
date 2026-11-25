package com.aidenholmes.backstock.catalog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public class ProductListingInterceptor implements HandlerInterceptor {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "createdAt");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        validateParam("page", request.getParameter("page"), 0);
        validateParam("size", request.getParameter("size"), 1);
        validateSort(request.getParameterValues("sort"));

        return true;
    }

    private static void validateParam(String name, @Nullable String rawParam, int minValue) {
        if (rawParam == null) {
            return;
        }

        int param;
        try {
            param = Integer.parseInt(rawParam);
        } catch (NumberFormatException e) {
            throw invalidParam(name, minValue, e);
        }

        if (param < minValue) {
            throw invalidParam(name, minValue, null);
        }
    }

    private static void validateSort(String @Nullable [] params) {
        if (params == null) {
            return;
        }

        for (String param : params) {
            String[] parts = param.split(",", -1);

            if (parts.length > 2 || !ALLOWED_SORT_FIELDS.contains(parts[0])) {
                throw invalidSort();
            }

            if (parts.length == 2 && Sort.Direction.fromOptionalString(parts[1]).isEmpty()) {
                throw invalidSort();
            }
        }
    }

    private static ResponseStatusException invalidSort() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Parameter \"sort\" must follow the format field[,direction]. Allowed fields are "
                        + String.join(", ", ALLOWED_SORT_FIELDS) + ". Allowed directions are asc and desc.");
    }

    private static ResponseStatusException invalidParam(String name, int minValue, @Nullable Exception cause) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Parameter \"" + name + "\" must be an integer greater than or equal to " + minValue,
                cause);
    }
}
