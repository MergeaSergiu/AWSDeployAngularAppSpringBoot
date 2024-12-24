package AWS.API.exception;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BucketAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseContainer> handleBucketAlreadyExistsException(BucketAlreadyExistsException ex) {
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.CONFLICT.value());
        errorResponseContainer.setErrorMessage(ex.awsErrorDetails().errorMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponseContainer> handleS3Exception(S3Exception ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.NOT_FOUND.value());
        errorResponseContainer.setErrorMessage(ex.awsErrorDetails().errorMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RefreshTokenExpired.class)
    public ResponseEntity<ErrorResponseContainer> handleRefreshTokenExpired(RefreshTokenExpired ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.NOT_ACCEPTABLE.value());
        errorResponseContainer.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponseContainer> handleExpiredJwtException(ExpiredJwtException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.UNAUTHORIZED.value());
        errorResponseContainer.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseContainer> handleAuthenticationException(AuthenticationException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponseContainer.setErrorMessage("Email or Password are invalid");
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseContainer> handleEntityNotFoundException(EntityNotFoundException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.NOT_FOUND.value());
        errorResponseContainer.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponseContainer> handleEntityExistsException(EntityExistsException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponseContainer.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseContainer> handleConstraintViolationException(ConstraintViolationException ex) {
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();
        errorResponseContainer.setHttpStatusCode(HttpStatus.BAD_REQUEST.value());
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String errorMessage = violation.getMessage();
            errorResponseContainer.setErrorMessage(errorMessage);
        }
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CloudFrontException.class)
    public ResponseEntity<ErrorResponseContainer> handleCloudFrontException(CloudFrontException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponseContainer.setErrorMessage(ex.awsErrorDetails().errorMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseContainer> handleIllegalArgumentException(IllegalArgumentException ex){
        ErrorResponseContainer errorResponseContainer = new ErrorResponseContainer();

        errorResponseContainer.setHttpStatusCode(HttpStatus.BAD_REQUEST.value());
        errorResponseContainer.setErrorMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponseContainer, HttpStatus.BAD_REQUEST);
    }
}
