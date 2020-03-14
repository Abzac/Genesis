public class CommandResult {
    public int command;
    public int position = 0;
    public int arg1 = -1;
    public int arg2 = -1;
    public int result = -1;

    public CommandResult(int command) {
        this.command = command;
    }
}
