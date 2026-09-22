package jail_locks;

public class BREAK_and_CONTINUE_abusing_exceptions extends RuntimeException {}

class BREAK_abusing_RuntimeExceptions extends  BREAK_and_CONTINUE_abusing_exceptions {}

class CONTINUE_abusing_RuntimeExceptions extends  BREAK_and_CONTINUE_abusing_exceptions {}
