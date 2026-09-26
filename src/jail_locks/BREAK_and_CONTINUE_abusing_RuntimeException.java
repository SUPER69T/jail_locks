package jail_locks;

public class BREAK_and_CONTINUE_abusing_RuntimeException extends RuntimeException {}

class BREAK extends  BREAK_and_CONTINUE_abusing_RuntimeException {}

class CONTINUE extends  BREAK_and_CONTINUE_abusing_RuntimeException {}
