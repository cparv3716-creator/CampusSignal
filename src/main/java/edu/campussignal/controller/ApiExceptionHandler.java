package edu.campussignal.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import edu.campussignal.service.EmailActionNotFoundException;
import edu.campussignal.service.EmailNotFoundException;
import edu.campussignal.service.IncompleteEmailClassificationException;
import edu.campussignal.service.InvalidEmailActionTransitionException;
import edu.campussignal.service.ProfileNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ProfileNotFoundException.class)
    public ProblemDetail notFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Profile not found");
    }

    @ExceptionHandler(EmailNotFoundException.class)
    public ProblemDetail emailNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Email not found");
    }

    @ExceptionHandler(EmailActionNotFoundException.class)
    public ProblemDetail actionNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Action not found");
    }

    @ExceptionHandler(InvalidEmailActionTransitionException.class)
    public ProblemDetail invalidActionTransition() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Action status transition is not allowed");
    }

    @ExceptionHandler(IncompleteEmailClassificationException.class)
    public ProblemDetail incompleteEmailClassification() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Stored email classification is incomplete");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Profile could not be stored; email must be unique");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalidRequest() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid request: check required fields, values, and types");
    }
}
