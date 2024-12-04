package helloswing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DatabaseConstants {
    // Error messages
    public static final String ERROR_NO_DATABASE = "Please select a database first";
    public static final String ERROR_NO_TABLE = "Please select a table first";
    public static final String ERROR_NO_ROW = "Please select a row first";
    public static final String ERROR_NO_SELECTION = "Please select a row first";
    public static final String ERROR_INVALID_NAME = "Name can only contain letters, numbers and underscore";
    public static final String ERROR_EMPTY_COLUMNS = "Columns cannot be empty";
    public static final String ERROR_CONNECTION = "Failed to connect to database";
    public static final String ERROR_UPDATE = "Failed to update record: %s";
    
    // Success messages
    public static final String SUCCESS_CREATE = "Created successfully!";
    public static final String SUCCESS_UPDATE = "Updated successfully!";
    public static final String SUCCESS_DELETE = "Deleted successfully!";
    public static final String SUCCESS_DROP = "Dropped successfully!";
    public static final String SUCCESS_CONNECT = "Connected to MySQL server successfully!";
    
    // Confirmation messages
    public static final String CONFIRM_DROP = "Are you sure you want to drop the table: %s?";
    public static final String CONFIRM_DELETE = "Are you sure you want to delete this record?";
    
    // Input prompts
    public static final String PROMPT_NEW_DB = "Enter new database name:";
    public static final String PROMPT_NEW_TABLE = "Enter new table name:";
    public static final String PROMPT_COLUMNS = "Enter columns:";
    public static final String PROMPT_SELECT = "Enter SELECT query:\n(e.g. SELECT * FROM tablename WHERE condition)";
    
    // UI labels
    public static final String LABEL_CURRENT_DB = "Current Database";
    public static final String LABEL_STATUS = "Status";
    public static final String LABEL_COMMANDS = "Commands";
    
    // SQL queries
    public static final String SQL_SHOW_DATABASES = "SHOW DATABASES";
    public static final String SQL_SHOW_TABLES = "SHOW TABLES";
    public static final String SQL_USE_DATABASE = "USE %s";
    public static final String SQL_CREATE_DATABASE = "CREATE DATABASE `%s`";
    public static final String SQL_CREATE_TABLE = "CREATE TABLE %s (%s)";
    public static final String SQL_DROP_TABLE = "DROP TABLE %s";

    public static final Set<String> SYSTEM_DATABASES = new HashSet<>(
    Arrays.asList("information_schema", "mysql", "performance_schema", "sys")
    );
}