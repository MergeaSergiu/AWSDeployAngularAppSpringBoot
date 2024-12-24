package AWS.API.exception;

public class RefreshTokenExpired extends RuntimeException{

    public RefreshTokenExpired(String message) {
        super(message);
    }
}
