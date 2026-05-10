package com.example.imunidata.utils;

import com.example.imunidata.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(ErrorResponse.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ErrorResponse.ResourceNotFoundException ex) {
        ErrorResponse err = new ErrorResponse(
                "Recurso não encontrado",
                HttpStatus.NOT_FOUND.toString(),
                ex.getMessage() != null ? ex.getMessage() : "Not Found",
                System.currentTimeMillis()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ErrorResponse.ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ErrorResponse.ServiceUnavailableException ex) {
        ErrorResponse err = new ErrorResponse(
                "Serviço indisponível",
                HttpStatus.SERVICE_UNAVAILABLE.toString(),
                ex.getMessage() != null ? ex.getMessage() : "Service Unavailable",
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ErrorResponse.ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(ErrorResponse.ResourceAlreadyExistsException ex) {
        ErrorResponse err = new ErrorResponse(
                "Vacina já existe",
                HttpStatus.CONFLICT.toString(),
                ex.getMessage() != null ? ex.getMessage() : "Already Exits",
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ErrorResponse.GenericServiceException.class)
    public ResponseEntity<ErrorResponse> handleGenericServiceException(ErrorResponse.GenericServiceException ex) {
        ErrorResponse err = new ErrorResponse(
                "Erro interno",
                HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                ex.getMessage() != null ? ex.getMessage() : "Internal Server Error",
                System.currentTimeMillis()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }


}
