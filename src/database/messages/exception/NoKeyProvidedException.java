package database.messages.exception;

public class NoKeyProvidedException extends DatabaseException {

    private static final long serialVersionUID = -6016241919421973475L;

    public NoKeyProvidedException() {
    }

    public NoKeyProvidedException(String string) {
        super(string);
    }

}
