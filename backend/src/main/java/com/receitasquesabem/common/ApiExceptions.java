package com.receitasquesabem.common;

/**
 * Exceções do domínio. Cada uma corresponde a um código HTTP,
 * traduzido em ApiExceptionHandler.
 */
public final class ApiExceptions {

    private ApiExceptions() {
    }

    /** 404 — o recurso não existe. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** 403 — existe, mas não é seu. */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    /** 409 — uma regra de negócio impede a operação. */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }
}
