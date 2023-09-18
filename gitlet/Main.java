package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Solomon Cheung
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> ....
     *
     */
    public static void main(String[] args) {
        Command.registerCommands();
        if (args.length < 1) {
            System.out.println("Please enter a command.");
            return;
        }
        Command fCommand = Command.getCommandMap().get(args[0]);
        if (fCommand == null) {
            System.out.println("No command with that name exists.");
            return;
        }
        if (!args[0].equals("init")) {
            File git = new File(System.getProperty("user.dir"), ".gitlet");
            if (!git.exists()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
        }

        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);

        try {
            fCommand.run(newArgs);
        } catch (GitletException e) {
            System.out.println("Incorrect operands.");
            if (!e.getMessage().equals("invalid argument count.")) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
