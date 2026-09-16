package com.msbatchproducer.msbatchproducer.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.NoSuchElementException;
@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail missing(NoSuchElementException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage()); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail tooLarge(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 25 MiB por archivo"); }
    @ExceptionHandler({JobExecutionAlreadyRunningException.class, JobInstanceAlreadyCompleteException.class, JobRestartException.class})
    public ProblemDetail conflict(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "La ejecución no puede iniciarse en su estado actual"); }
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ProblemDetail missingFile(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Falta el campo file en form-data"); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ProblemDetail media(Exception e) { return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Usa multipart/form-data y el campo file"); }
    @ExceptionHandler(Exception.class)
    public ProblemDetail failure(Exception e) {
        log.error("Error al gestionar la carga", e);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la solicitud; revisa el log del servicio");
    }
}
