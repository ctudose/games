package games.contracts;

public final class ContractError {

    private final String code;
    private final String message;

    public ContractError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
