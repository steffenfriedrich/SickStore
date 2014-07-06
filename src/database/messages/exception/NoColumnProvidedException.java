package database.messages.exception;

public class NoColumnProvidedException extends DatabaseException {

    private static final long serialVersionUID = 1L;

    public NoColumnProvidedException(String string) {
        super(string);
    }

    public NoColumnProvidedException() {
    }
}
