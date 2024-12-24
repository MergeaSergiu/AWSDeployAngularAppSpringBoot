package AWS.API.exception;

import lombok.Data;

@Data
public class ErrorResponseContainer {
    private String errorMessage;
    private Integer httpStatusCode;
}
