public class CommandResult {
    public int command;
    public int position;
    public int arg1 = -1;
    public int arg2 = -1;
    public int result = -1;

    public CommandResult(int command, int position) {
        this.command = command;
        this.position = position;
    }
}
