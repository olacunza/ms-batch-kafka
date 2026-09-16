package com.msmonitoreo.msmonitoreo.exception;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.transaction.CannotCreateTransactionException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ConstraintViolationException.class,MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalid(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"page debe estar entre 0 y 10000; size entre 1 y 100.");
    }

    @ExceptionHandler({DataAccessException.class,CannotCreateTransactionException.class})
    public ProblemDetail unavailable(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo consultar la base de datos. Intenta nuevamente.");
    }

}
