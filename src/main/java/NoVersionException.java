import java.lang.Exception;

public class NoVersionException extends Exception {

    public NoVersionException( String errorMessage, int versionNumber ){
        super(" Non ci sono entry per la versione " + Integer.toString( versionNumber ) );
    }

}