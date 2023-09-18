package gitlet;

import java.io.File;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

/**
 *
 * Command commmand command command command where them commands at.
 * @author Solomon Cheung
 */
public class Command {

    /** hashmap mapping command names to corresponding Command instances. */
    private static HashMap<String, Command> commandMap
        = new HashMap<String, Command>();
    /**
     * commandMap accessor.
     * @return commandMap
     */
    public static HashMap<String, Command> getCommandMap() {
        return commandMap;
    }
    /** name that runs the command. */
    private String _name;
    /** number of arguments for command. */
    private int _numArgs;

    /**
     * run the command.
     *
     * @throws GitletException
     * @param args
     */
    void run(String[] args) {
        if (!validateArgs(args)) {
            throw Utils.error("invalid argument count.");
        }
    }

    Command(String name, int argLen) {
        _name = name;
        _numArgs = argLen;
        commandMap.put(name, this);
    }

    /**
     * register all of our commands.
     *
     * contents:
     *  init
     *  add
     *  commit
     *  rm
     *  log
     *  global-log
     *  find
     *  status
     *  checkout
     *  branch
     *  rm-branch
     *  reset
     *  merge
     *
     */
    public static void registerCommands() {
        new Init("init", 0);
        new Add("add", 1);
        new CommitCommand("commit", 1);
        new Rm("rm", 1);
        new Log("log", 0);
        new GlobalLog("global-log", 0);
        new Checkout("checkout", -1);
        new Find("find", 1);
        new Status("status", 0);
        new BranchCommand("branch", 1);
        new RmBranch("rm-branch", 1);
        new Reset("reset", 1);
        new MergeCommand("merge", 1);
    }
    boolean validateArgs(String[] args) {
        if (args.length == _numArgs) {
            return true;
        }
        return false;
    }

    /**
     * init.
     */
    static class Init extends Command {
        Init(String name, int argLen) {
            super(name, argLen);
        }
        /**
         *  Initialize a gitlet repository.
         *  - Create a gitlet folder,
         *  - Create and store a new Gitlet instance,
         *  - a new commit,
         *  - and a new branch (master).
         *
         * @param args
         */
        void run(String[] args) {
            super.run(args);
            try {
                File git = new File(
                    System.getProperty("user.dir"), ".gitlet");
                if (git.exists()) {
                    System.out.println("A gitlet version-control system "
                        + "already exists in the current directory.");
                    return;
                }
                new File(System.getProperty("user.dir"),
                 ".gitlet").mkdirs();
                new File(System.getProperty("user.dir"),
                 ".gitlet/blobs").mkdirs();
                new File(System.getProperty("user.dir"),
                 ".gitlet/commits").mkdirs();
                Gitlet.Commit initCommit = new Gitlet.Commit(
                    "initial commit", null);
                initCommit.setTimestamp(new Timestamp(0));
                initCommit.save();
                Gitlet.Branch masterBranch = new Gitlet.Branch(
                    "master", initCommit.getID());
                Gitlet.GITLET_OBJECT.addBranch(masterBranch);
                Gitlet.GITLET_OBJECT.setHeadBranch(masterBranch);
                Gitlet.GITLET_OBJECT.save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * add files.
     */
    static class Add extends Command {
        Add(String name, int argLen) {
            super(name, argLen);
        }
        /**
         *  Adds files to be staged for commit.
         *  checks if file exists (if specified).
         *  If file is already staged but different,
         *  overwrites it in the staging area.
         *  If file is identical to stored file,
         *  removes it from staging area.
         *  If the file is in the removing area,
         *  remove it from the removing area.
         *  Create blobs for staged files.
         *  @param args
         */
        void run(String[] args) {
            super.run(args);
            File file = new File(
                System.getProperty("user.dir"),
                args[0]);
            String filename = args[0];
            if (!file.exists() && !file.isFile()) {
                System.out.println("File does not exist.");
                return;
            }
            if (Gitlet.GITLET_OBJECT.getRemovingArea()
                .hasFile(filename)) {
                Gitlet.GITLET_OBJECT.getRemovingArea()
                    .unstage(filename);
                Gitlet.GITLET_OBJECT.save();
                return;
            }
            Gitlet.Blob blobFile = Gitlet.Blob.fromFileName(filename);
            blobFile.save();
            if (blobFile.getId().equals(
                Gitlet.getHashInHeadFromFileName(filename))) {
                Gitlet.GITLET_OBJECT.getStagingArea().unstage(filename);
                Gitlet.GITLET_OBJECT.save();
                return;
            }
            Gitlet.GITLET_OBJECT.getStagingArea().stage(filename);
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * commit.
     */
    static class CommitCommand extends Command {
        CommitCommand(String name, int argLen) {
            super(name, argLen);
        }
        /** Takes a snapshot of all tracked and staged files
         *  and creates a new commit.
         *  Sets the new commits parent to the old head commit.
         *  Stores the commit.
         *  Combines the old tracked files and the new staged
         *  files to make the new map of tracked files.
         *  Clears staging and removing areas.
         *  Creates new blobs.
         * @param args */
        void run(String[] args) {
            try {
                super.run(args);
            } catch (GitletException ignore) {
                System.out.println("Please enter a commit message.");
                return;
            }
            String commitMessage = args[0];
            if (commitMessage.equals("")) {
                System.out.println("Please enter a commit message.");
                return;
            }
            if (Gitlet.GITLET_OBJECT.getStagingArea()
                .getFiles().size() == 0
                && Gitlet.GITLET_OBJECT.getRemovingArea()
                .getFiles().size() == 0) {
                System.out.println("No changes added to the commit.");
                return;
            }
            Gitlet.Commit newCommit = new Gitlet.Commit(commitMessage,
                Gitlet.GITLET_OBJECT.getHead());
            newCommit.setTimestamp(new Timestamp(System.currentTimeMillis()));
            for (java.util.Map.Entry<String, String> entry
                : Gitlet.GITLET_OBJECT.getStagingArea()
                .getFiles().entrySet()) {
                newCommit.trackFile(entry.getKey(), entry.getValue());
            }
            for (java.util.Map.Entry<String, String> entry
                : Gitlet.GITLET_OBJECT.getRemovingArea()
                .getFiles().entrySet()) {
                newCommit.unTrackFile(entry.getKey());
            }
            Gitlet.GITLET_OBJECT.getStagingArea().clear();
            Gitlet.GITLET_OBJECT.getRemovingArea().clear();
            newCommit.save();
            Gitlet.GITLET_OBJECT.getHeadBranch().setCommit(newCommit.getID());
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * remove files.
     */
    static class Rm extends Command {
        Rm(String name, int argLen) {
            super(name, argLen);
        }
        /**
         * Remove the file with the given name from the directory.
         *  If staged, unstage it.
         *  If the file is tracked by head, add to removing area.
         *  Uses gitlet.getStagingArea().isStaged, gitlet.getHead().isTracked.
         *  ---
         *  Unstage the file if it is currently staged for addition.
         *  If the file is tracked in the current commit,
         *  stage it for removal and remove the file from the working directory
         *  - if the user has not already done so.
         *  do not remove it unless it is tracked in the current commit).
         * @see Utils#restrictedDelete()
         * @param args
         */
        void run(String[] args) {
            super.run(args);
            String filename = args[0];
            if (!Gitlet.GITLET_OBJECT.getStagingArea().hasFile(filename)
                && !Gitlet.GITLET_OBJECT.getHead().isTracked(filename)) {
                System.out.println("No reason to remove the file.");
                return;
            }
            if (Gitlet.GITLET_OBJECT.getStagingArea()
                .hasFile(filename)) {
                Gitlet.GITLET_OBJECT.getStagingArea()
                    .unstage(filename);
                Gitlet.GITLET_OBJECT.save();
                return;
            }
            if (Gitlet.GITLET_OBJECT.getHead().isTracked(filename)) {
                Gitlet.GITLET_OBJECT.getRemovingArea().stage(filename, "");
                Utils.restrictedDelete(new File(
                    System.getProperty("user.dir"), filename));
            }
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * log.
     */
    static class Log extends Command {
        Log(String name, int argLen) {
            super(name, argLen);
        }
        /** Recursively traverse the linked list of commits
         *  starting from the head, printing info about each commit.
         * @param args */
        void run(String[] args) {
            Gitlet.Commit a = Gitlet.GITLET_OBJECT.getHead();
            while (a != null) {
                System.out.println("===\n" + a.toString() + "\n");
                a = a.getParent();
            }
        }
    }

    /**
     * global-log.
     */
    static class GlobalLog extends Command {
        GlobalLog(String name, int argLen) {
            super(name, argLen);
        }
        /** Go through every file in the commits directory in .gitlet
         *  using gitlet.Utils.plainFilenamesIn,
         *  deserialize it, and print relevant content.
         * @param args
         */
        void run(String[] args) {
            List<String> files = Utils.plainFilenamesIn(
                new File(System.getProperty("user.dir"),
                Gitlet.ABS_COMMIT_FOLDER));
            for (String file : files) {
                Gitlet.Commit commit = Gitlet.Commit.fromHash(file);
                System.out.println("===\n" + commit.toString() + "\n");
            }
        }
    }

    /**
     * checkout new branch.
     */
    static class Checkout extends Command {
        Checkout(String name, int argLen) {
            super(name, argLen);
        }
        /**
         * runs relevant checkout method.
         *  If a fileName is specified but not a commit hash,
         *    checkout(commitHash, fileName) is run
         *    with the commit hash of the head.
         *  If nothing is specified runs checkout on the active branch.
         * @param args
         */
        void run(String[] args) {
            if (args.length == 3 && args[1].equals("--")) {
                checkout(args[0], args[2]);
            } else if (args.length == 2 && args[0].equals("--")) {
                checkout(Gitlet.GITLET_OBJECT.getHeadHash(), args[1]);
            } else if (args.length == 1) {
                checkout(args[0]);
            } else {
                super.run(args);
            }
            Gitlet.GITLET_OBJECT.save();
        }

        /**
         * overwrite fileName's contents with the contents of the blob
         * with a hash of commit._trackedFiles.get(fileName).
         * @param commitHash
         * @param fileName
         */
        void checkout(String commitHash, String fileName) {
            Gitlet.Commit commit;
            try {
                commit = Gitlet.Commit.fromHash(commitHash);
            } catch (IllegalArgumentException ignore) {
                System.out.println("No commit with that id exists.");
                return;
            }
            if (!commit.isTracked(fileName)) {
                System.out.println("File does not exist in that commit.");
                return;
            }
            File globFile = new File(
                System.getProperty("user.dir"),
                Gitlet.ABS_BLOB_FOLDER + "/"
                 + commit.getHashFromFileName(fileName));
            File targetFile = new File(System.getProperty("user.dir"),
                fileName);
            if (!globFile.exists()) {
                throw Utils.error("File (blob) not found!!!");
            }
            Utils.writeContents(targetFile, Utils.readContents(globFile));
        }

        /**
         *  set gitlet.currentBranch to the branch name,
         *  and run checkout(fileName) on all the files in _trackedFiles.
         *  clears staging and removal area unless
         *  on branchName is current branch.
         *  @param branchName
         */
        void checkout(String branchName) {
            Gitlet.Branch branch = Gitlet.GITLET_OBJECT.getBranch(branchName);
            if (branch == null) {
                System.out.println("No such branch exists.");
                return;
            }
            if (Gitlet.GITLET_OBJECT.getHeadBranch()
                .getName().equals(branchName)) {
                System.out.println("No need to checkout the current branch.");
                return;
            }
            if (Gitlet.GITLET_OBJECT.getStagingArea().getFiles().size() != 0
                || Gitlet.GITLET_OBJECT.getRemovingArea().getFiles().size() != 0
                || Gitlet.GITLET_OBJECT.unstagedFiles().size() != 0
                || Gitlet.GITLET_OBJECT.untrackedFiles().size() != 0) {
                System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
                return;
            }
            for (String file : Utils.plainFilenamesIn(
                new File(System.getProperty("user.dir")))) {
                Utils.restrictedDelete(file);
            }
            Gitlet.GITLET_OBJECT.setHeadBranch(branch);
            for (java.util.Map.Entry<String, String> file
                : Gitlet.GITLET_OBJECT.getHead().getTrackedFiles().entrySet()) {
                Utils.writeContents(
                    new File(System.getProperty("user.dir"),
                        file.getKey()),
                    Gitlet.Blob.fromHash(file.getValue()).getContents());
            }
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * find.
     */
    static class Find extends Command {
        Find(String name, int argLen) {
            super(name, argLen);
        }
        /**
         * go through every commit file using gitlet.Utils.plainFilenamesIn
         *  in the commit directory,
         *  deserializing and checking against commit messages,
         *  storing those that match in an ArrayList.
         *  Afterwards, print out relevant info.
         * @param args
         */
        void run(String[] args) {
            super.run(args);
            String commitMsg = args[0];
            boolean found = false;
            List<String> files = Utils.plainFilenamesIn(
                new File(System.getProperty("user.dir"),
                Gitlet.ABS_COMMIT_FOLDER));
            for (String file : files) {
                Gitlet.Commit commit = Gitlet.Commit.fromHash(file);
                if (commit.getMessage().equals(commitMsg)) {
                    System.out.println(commit.getID());
                    found = true;
                }
            }
            if (!found) {
                System.out.println("Found no commit with that message.");
            }
        }

    }

    /**
     * status.
     */
    static class Status extends Command {
        Status(String name, int argLen) {
            super(name, argLen);
        }
        /**
         *  goes through gitlet._branches,
         *  printing relevant info,
         *  taking gitlet.getHeadBranch().getName() into account.
         *  */
        void displayBranches() {
            for (java.util.Map.Entry<String, Gitlet.Branch> entry
                : Gitlet.GITLET_OBJECT.getBranches().entrySet()) {
                if (entry.getKey().equals(
                    Gitlet.GITLET_OBJECT.getHeadBranch().getName())) {
                    System.out.print("*");
                }
                System.out.println(entry.getKey());
            }
        }

        /**
         *  goes through gitlet.getStagingArea().getFiles()
         *  and print relevant content.
         */
        void displayStagedFiles() {
            for (java.util.Map.Entry<String, String> files
                : Gitlet.GITLET_OBJECT.getStagingArea().getFiles().entrySet()) {
                System.out.println(files.getKey());
            }
        }

        /**
         *  goes through gitlet.getRemovingArea().getFiles()
         *  and print relevant content.
         */
        void displayRemovedFiles() {
            for (java.util.Map.Entry<String, String> files
                : Gitlet.GITLET_OBJECT.getRemovingArea()
                .getFiles().entrySet()) {
                System.out.println(files.getKey());
            }
        }

        /**
         *  uses Command.MergeCommand.modified to find modified files
         *  and print relevant content.
         *
         */
        void displayModificationsNotStagedForCommit() {
            for (java.util.Map.Entry<String, Boolean> entry
                : Gitlet.GITLET_OBJECT.unstagedFiles().entrySet()) {
                System.out.println(String.format("%s (%s)",
                    entry.getKey(), entry.getValue() ? "deleted" : "modified"));
            }
        }

        /**
         * uses Command.MergeCommand.ANotInB to find untracked files.
         *
         */
        void displayUntrackedFiles() {
            for (String entry : Gitlet.GITLET_OBJECT.untrackedFiles()) {
                System.out.println(entry);
            }
        }

        /** printing.
         * @param args
         */
        void run(String[] args) {
            System.out.println("=== Branches ===");
            displayBranches();
            System.out.println("\n=== Staged Files ===");
            displayStagedFiles();
            System.out.println("\n=== Removed Files ===");
            displayRemovedFiles();
            System.out.println("\n=== Modifications Not Staged For Commit ===");
            displayModificationsNotStagedForCommit();
            System.out.println("\n=== Untracked Files ===");
            displayUntrackedFiles();
        }
    }

    /**
     * creation of new branch.
     */
    static class BranchCommand extends Command {
        BranchCommand(String name, int argLen) {
            super(name, argLen);
        }
        void run(String[] args) {
            super.run(args);
            String branchName = args[0];
            if (Gitlet.GITLET_OBJECT.getBranch(branchName) != null) {
                System.out.println("A branch with that name already exists.");
                return;
            }
            Gitlet.Branch newBranch = new Gitlet.Branch(branchName,
                Gitlet.GITLET_OBJECT.getHeadBranch().getCommitHash());
            Gitlet.GITLET_OBJECT.addBranch(newBranch);
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * removal of branch.
     */
    static class RmBranch extends Command {
        RmBranch(String name, int argLen) {
            super(name, argLen);
        }
        void run(String[] args) {
            super.run(args);
            String branchName = args[0];
            if (Gitlet.GITLET_OBJECT.getHeadBranch()
                .getName().equals(branchName)) {
                System.out.println("Cannot remove the current branch.");
                return;
            }
            Gitlet.Branch branch = Gitlet.GITLET_OBJECT.getBranch(branchName);
            if (branch == null) {
                System.out.println("A branch with that name does not exist.");
                return;
            }
            Gitlet.GITLET_OBJECT.removeBranch(branch);
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * merge.
     */
    static class MergeCommand extends Command {
        MergeCommand(String name, int argLen) {
            super(name, argLen);
        }
        /** If split point is given branch, does nothing.
         *  If split point is gitlet.getHead(), checks out given branch.
         *  Prints relevant messages.
         *  @param args
         */
        void run(String[] args) {
            super.run(args);
            String targetBranchStr = args[0];
            Gitlet.Branch headBranch = Gitlet.GITLET_OBJECT
                .getHeadBranch();
            Gitlet.Branch targetBranch = Gitlet.GITLET_OBJECT
                .getBranch(targetBranchStr);
            if (!checkRun(targetBranchStr,
                headBranch, targetBranch, true)) {
                return;
            }
            Gitlet.Commit splitCommit = getSplitPoint(
                headBranch.getCommit(), targetBranch.getCommit());
            if (splitCommit.getID().equals(
                    targetBranch.getCommit().getID())) {
                System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
                return;
            }
            if (splitCommit.getID().equals(
                    headBranch.getCommit().getID())) {
                Gitlet.GITLET_OBJECT.getHeadBranch().setCommit(
                    targetBranch.getCommit().getID());
                System.out.println("Current branch fast-forwarded.");
                for (String file : Utils.plainFilenamesIn(
                    new File(System.getProperty("user.dir")))) {
                    Utils.restrictedDelete(file);
                }
                for (java.util.Map.Entry<String, String> file
                    : Gitlet.GITLET_OBJECT.getHead()
                        .getTrackedFiles().entrySet()) {
                    Utils.writeContents(
                        new File(System.getProperty("user.dir"), file.getKey()),
                        Gitlet.Blob.fromHash(file.getValue()).getContents());
                }
                Gitlet.GITLET_OBJECT.save();
                return;
            }
            Gitlet.Merge merge = new Gitlet.Merge(
                headBranch.getCommit(),
                targetBranch.getCommit(),
                String.format(
                    "Merged %s into %s.",
                    targetBranch.getName(),
                    headBranch.getName()));
            boolean bPrintMergeConflict = false;
            bPrintMergeConflict = runMain(headBranch,
                targetBranch, splitCommit,
                merge, false);
            if (bPrintMergeConflict) {
                System.out.println("Encountered a merge conflict.");
            }
            merge.setTimestamp(new Timestamp(System.currentTimeMillis()));
            merge.save();
            headBranch.setCommit(merge.getID());
            Gitlet.GITLET_OBJECT.save();
        }

        static boolean checkRun(
            String targetBranchStr, Gitlet.Branch headBranch,
            Gitlet.Branch targetBranch, boolean bool) {
            if (targetBranch == null) {
                System.out.println("A branch with that name does not exist.");
                return false;
            }
            if (Gitlet.GITLET_OBJECT.getStagingArea().getFiles().size() != 0
                || Gitlet.GITLET_OBJECT.getRemovingArea().getFiles().size() != 0
                || Gitlet.GITLET_OBJECT.unstagedFiles().size() != 0) {
                System.out.println("You have uncommitted changes.");
                return false;
            }
            if (Gitlet.GITLET_OBJECT.untrackedFiles().size() != 0) {
                System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
                return false;
            }
            if (headBranch.getName().equals(targetBranchStr)) {
                System.out.println("Cannot merge a branch with itself.");
                return false;
            }
            return true;
        }

        static boolean runMain(Gitlet.Branch headBranch,
            Gitlet.Branch targetBranch, Gitlet.Commit splitCommit,
            Gitlet.Merge merge, boolean bool) {
            HashMap<String, String> headSplitDiff = intersection(
                headBranch.getCommit().getTrackedFiles(),
                splitCommit.getTrackedFiles());
            HashMap<String, String> targetSplitDiff = intersection(
                targetBranch.getCommit().getTrackedFiles(),
                splitCommit.getTrackedFiles());
            HashMap<String, String> existsInHeadAndTarget = intersection(
                headBranch.getCommit().getTrackedFiles(),
                targetBranch.getCommit().getTrackedFiles());
            HashMap<String, String> onlyInHead = aNotInB(
                headBranch.getCommit().getTrackedFiles(),
                splitCommit.getTrackedFiles());
            HashMap<String, String> onlyInTarget = aNotInB(
                targetBranch.getCommit().getTrackedFiles(),
                splitCommit.getTrackedFiles());
            HashMap<String, String> inTargetButNotInHead = aNotInB(
                onlyInTarget, onlyInHead);
            HashMap<String, String> inHeadButNotInTarget = aNotInB(
                onlyInHead, onlyInTarget);
            HashMap<String, String> headModified = modified(
                splitCommit.getTrackedFiles(),
                headBranch.getCommit().getTrackedFiles());
            HashMap<String, String> targetModified = modified(
                splitCommit.getTrackedFiles(),
                targetBranch.getCommit().getTrackedFiles());

            bool = existRun(existsInHeadAndTarget, targetBranch,
                splitCommit, merge, bool);
            inTargetNoHeadRun(inTargetButNotInHead, headSplitDiff, merge);
            for (java.util.Map.Entry<String, String> files
                : inHeadButNotInTarget.entrySet()) {
                if (targetSplitDiff.get(files.getKey()) == null) {
                    continue;
                }
                merge.trackFile(files.getKey(), files.getValue());
                Utils.writeContents(
                    new File(System.getProperty("user.dir"),
                    files.getKey()),
                    Gitlet.Blob.fromHash(files.getValue()).getContents());
            }
            for (java.util.Map.Entry<String, String> files
                : inHeadButNotInTarget.entrySet()) {
                merge.trackFile(files.getKey(), files.getValue());
            }
            for (java.util.Map.Entry<String, String> files
                : inTargetButNotInHead.entrySet()) {
                merge.trackFile(files.getKey(), files.getValue());
                Utils.writeContents(
                    new File(System.getProperty("user.dir"),
                    files.getKey()),
                    Gitlet.Blob.fromHash(files.getValue()).getContents());
            }
            bool = headModifiedRun(headModified, targetModified,
                targetBranch, splitCommit, merge, bool);
            bool = targetModifiedRun(headModified, targetModified,
                headBranch, splitCommit, merge, bool);
            return bool;
        }

        static boolean existRun(HashMap<String, String> existsInHeadAndTarget,
            Gitlet.Branch targetBranch, Gitlet.Commit splitCommit,
            Gitlet.Merge merge, boolean bool) {
            for (java.util.Map.Entry<String, String> files
                : existsInHeadAndTarget.entrySet()) {
                if (!files.getValue().equals(targetBranch.getCommit()
                    .getTrackedFiles().get(files.getKey()))
                    && !targetBranch.getCommit()
                    .getTrackedFiles().get(files.getKey()).equals(
                        splitCommit.getTrackedFiles().get(
                            files.getKey())) && !files.getValue().equals(
                        splitCommit.getTrackedFiles().get(
                            files.getKey()))) {
                    bool = true;
                    File glob = new File(System.getProperty("user.dir"),
                        files.getKey());
                    Gitlet.Blob nBlob = mergeFiles(files.getValue(),
                        targetBranch.getCommit().getTrackedFiles()
                        .get(files.getKey()));
                    merge.trackFile(files.getKey(), nBlob.getId());
                    Utils.restrictedDelete(glob);
                    Utils.writeContents(glob, nBlob.getContents());
                } else {
                    String hashOfFile = files.getValue();
                    if (files.getValue().equals(splitCommit
                        .getTrackedFiles().get(files.getKey()))) {
                        hashOfFile = targetBranch.getCommit()
                        .getTrackedFiles().get(files.getKey());
                    }
                    merge.trackFile(files.getKey(), hashOfFile);
                    Utils.restrictedDelete(
                        new File(System.getProperty("user.dir"),
                        files.getKey()));
                    Utils.writeContents(
                        new File(System.getProperty("user.dir"),
                        files.getKey()),
                        Gitlet.Blob.fromHash(hashOfFile).getContents());
                }
            }
            return bool;
        }
        static void inTargetNoHeadRun(
            HashMap<String, String> inTargetButNotInHead,
            HashMap<String, String> headSplitDiff, Gitlet.Merge merge) {
            for (java.util.Map.Entry<String, String> files
                : inTargetButNotInHead.entrySet()) {
                if (headSplitDiff.get(files.getKey()) == null) {
                    continue;
                }
                merge.trackFile(files.getKey(), files.getValue());
                Utils.writeContents(
                    new File(System.getProperty("user.dir"),
                    files.getKey()),
                    Gitlet.Blob.fromHash(files.getValue()).getContents());
            }
        }
        static boolean headModifiedRun(HashMap<String, String> headModified,
            HashMap<String, String> targetModified, Gitlet.Branch targetBranch,
            Gitlet.Commit splitCommit,
            Gitlet.Merge merge, boolean bool) {
            for (java.util.Map.Entry<String, String> files
                : headModified.entrySet()) {
                File glob = new File(System.getProperty("user.dir"),
                    files.getKey());
                if (!files.getValue().equals("")) {
                    continue;
                }
                if (splitCommit.isTracked(files.getKey())
                    && targetBranch.getCommit().getTrackedFiles().get(
                        files.getKey()) != null
                    && !splitCommit.getTrackedFiles().get(
                        files.getKey()).equals(
                        targetBranch.getCommit().getTrackedFiles().get(
                            files.getKey()))) {
                    bool = true;
                    Gitlet.Blob nBlob = mergeFiles(files.getValue(),
                        targetModified.get(files.getKey()));
                    merge.trackFile(files.getKey(), nBlob.getId());
                    Utils.restrictedDelete(glob);
                    Utils.writeContents(glob, nBlob.getContents());
                } else {
                    merge.unTrackFile(files.getKey());
                    Utils.restrictedDelete(glob);
                }
            }
            return bool;
        }

        static boolean targetModifiedRun(HashMap<String, String> headModified,
            HashMap<String, String> targetModified, Gitlet.Branch headBranch,
            Gitlet.Commit splitCommit,
            Gitlet.Merge merge, boolean bool) {
            for (java.util.Map.Entry<String, String> files
                : targetModified.entrySet()) {
                File glob = new File(System.getProperty("user.dir"),
                    files.getKey());
                if (!files.getValue().equals("")) {
                    continue;
                }
                if (splitCommit.isTracked(
                    files.getKey())
                    && headBranch.getCommit().getTrackedFiles().get(
                        files.getKey()) != null
                    && !splitCommit.getTrackedFiles().get(
                        files.getKey()).equals(
                        headBranch.getCommit().getTrackedFiles().get(
                            files.getKey()))) {
                    bool = true;
                    Gitlet.Blob nBlob = mergeFiles(headModified.get(
                        files.getKey()), files.getValue());
                    merge.trackFile(files.getKey(), nBlob.getId());
                    Utils.restrictedDelete(glob);
                    Utils.writeContents(glob, nBlob.getContents());
                } else {
                    merge.unTrackFile(files.getKey());
                    Utils.restrictedDelete(glob);
                }
            }
            return bool;
        }

        static Gitlet.Blob mergeFiles(String hash1, String hash2) {
            return mergeFilesReal(
                hash1 == null || hash1.equals("") ? ""
                : Gitlet.Blob.fromHash(hash1).getContents(),
                hash2 == null || hash2.equals("") ? ""
                : Gitlet.Blob.fromHash(hash2).getContents()
            );
        }

        static Gitlet.Blob mergeFiles(Gitlet.Blob file1, Gitlet.Blob file2) {
            return mergeFilesReal(
                file1.getContents(),
                file2.getContents()
            );
        }

        static Gitlet.Blob mergeFilesReal(String content1, String content2) {
            String newContent = String.format(
                "<<<<<<< HEAD\n%s=======\n%s>>>>>>>\n",
                content1,
                content2);
            Gitlet.Blob nBlob = new gitlet.Gitlet.Blob(newContent);
            nBlob.save();
            return nBlob;
        }

        /**
         * get the split point of the 2 commits.
         * The split point is a latest common ancestor of
         *  the current and given branch heads:
         * @param a
         * @param b
         * @return splitpoint
         */
        static Gitlet.Commit getSplitPoint(Gitlet.Commit a, Gitlet.Commit b) {
            while (a != null) {
                Gitlet.Commit currentTr = a;
                Gitlet.Commit targetTr = b;
                Gitlet.Commit retValue = recursiveIterateCurrentSearchTarget(
                    currentTr, targetTr);
                if (retValue != null) {
                    return retValue;
                }
                a = a.getParent();
            }
            return null;
        }

        /**
         * recusively iterate "current" (including merges)
         *  for searching in "target".
         * @param currentTr
         * @param targetTr
         * @return search for current in target
         */
        static Gitlet.Commit recursiveIterateCurrentSearchTarget(
            Gitlet.Commit currentTr, Gitlet.Commit targetTr) {
            if (currentTr == null) {
                return null;
            }
            Gitlet.Commit retVal = null;
            if (currentTr.isMerge()) {
                retVal = recursiveIterateCurrentSearchTarget(
                    ((Gitlet.Merge) currentTr).getParent2(), targetTr);
                if (retVal != null) {
                    return retVal;
                }
            }
            retVal = recursiveSearchTarget(currentTr, targetTr);
            if (retVal != null) {
                return retVal;
            }
            return null;
        }

        /**
         * recusively search for "current" in "target".
         * even thier merges.
         * @param currentTr
         * @param targetTr
         * @return current if it exists in target
         */
        static Gitlet.Commit recursiveSearchTarget(Gitlet.Commit currentTr,
            Gitlet.Commit targetTr) {
            if (currentTr == null) {
                return null;
            }
            while (targetTr != null) {
                if (targetTr.getID().equals(currentTr.getID())) {
                    return targetTr;
                }
                if (targetTr.isMerge()) {
                    Gitlet.Commit retVal = recursiveSearchTarget(currentTr,
                        ((Gitlet.Merge) targetTr).getParent2());
                    if (retVal != null) {
                        return retVal;
                    }
                }
                targetTr = targetTr.getParent();
            }
            return null;
        }

        /**
         * intersection of tracked filenames in commit A and B
         *  with blobs from A.
         * @param a
         * @param b
         * @return intfMap
         */
        static HashMap<String, String> intersection(HashMap<String, String> a,
            HashMap<String, String> b) {
            HashMap<String, String> intfMap = new HashMap<>();
            for (java.util.Map.Entry<String, String> aEntry : a.entrySet()) {
                String fHash = b.get(aEntry.getKey());

                if (fHash == null) {
                    continue;
                }

                intfMap.put(aEntry.getKey(), aEntry.getValue());
            }

            return intfMap;
        }

        /**
         * list of tracked file names in A that aren't in B, with blobs from A.
         * @param A
         * @param B
         * @return
         */
        static HashMap<String, String> aNotInB(HashMap<String, String> A,
            HashMap<String, String> B) {
            HashMap<String, String> intfMap = new HashMap<>();
            for (java.util.Map.Entry<String, String> aEntry : A.entrySet()) {
                String fHash = B.get(aEntry.getKey());

                if (fHash == null) {
                    intfMap.put(aEntry.getKey(), aEntry.getValue());
                }
            }

            return intfMap;
        }

        /**
         * lists files in B that have changed from A or are not in A.
         * Maps to blobs from B. deleted files are included with empty blob.
         *
         * @param A
         * @param B
         * @return
         */
        static HashMap<String, String> modified(HashMap<String, String> A,
            HashMap<String, String> B) {
            HashMap<String, String> intfMap = new HashMap<>();
            for (java.util.Map.Entry<String, String> aEntry : A.entrySet()) {
                String fHash = B.get(aEntry.getKey());

                if (fHash == null) {
                    intfMap.put(aEntry.getKey(), "");
                    continue;
                }

                if (fHash.equals(aEntry.getValue())) {
                    continue;
                }

                intfMap.put(aEntry.getKey(), fHash);
            }

            return intfMap;
        }
    }

    /**
     * reset.
     */
    static class Reset extends Command {
        Reset(String name, int argLen) {
            super(name, argLen);
        }
        /** Sets the pointer for the current branch to the given commit ID,
         *  then runs the "checkout" command with no arguments.
         *  Goes through all files, removing any that aren't keys
         *  in commit.getTrackedFiles().
         *  Clears staging and removal area.
         * @param args
         */
        void run(String[] args) {
            super.run(args);
            String commitHash = args[0];

            Gitlet.Commit commit;
            try {
                commit = Gitlet.Commit.fromHash(commitHash);
            } catch (IllegalArgumentException ignore) {
                System.out.println("No commit with that id exists.");
                return;
            }

            if (Gitlet.GITLET_OBJECT.unstagedFiles().size() != 0
                || Gitlet.GITLET_OBJECT.untrackedFiles().size() != 0) {
                System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
                return;
            }
            Gitlet.GITLET_OBJECT.getStagingArea().clear();
            Gitlet.GITLET_OBJECT.getRemovingArea().clear();
            for (String file
                : Utils.plainFilenamesIn(
                    new File(System.getProperty("user.dir")))) {
                Utils.restrictedDelete(file);
            }
            Gitlet.GITLET_OBJECT.getHeadBranch().setCommit(commit.getID());

            for (java.util.Map.Entry<String, String> file
                : commit.getTrackedFiles().entrySet()) {
                Utils.writeContents(
                    new File(System.getProperty("user.dir"), file.getKey()),
                    Gitlet.Blob.fromHash(file.getValue()).getContents()
                );
            }
            Gitlet.GITLET_OBJECT.save();
        }
    }

    /**
     * built diff.
     */
    static class Diff extends Command {
        Diff(String name, int argLen) {
            super(name, argLen);
        }

        void run(String[] args) {
            if (args.length == 1) {
                return;
            } else if (args.length == 2) {
                diffBranchFromCurrent(args[1]);
                return;
            }
            super.run(args);
            String commitHash1 = args[0];
            String commitHash2 = args[1];
        }

        void diffBranchFromCurrent(String branch1) {
            for (String file : Utils.plainFilenamesIn(
                new File(System.getProperty("user.dir")))) {
                Utils.restrictedDelete(file);
            }
        }

        void diffBranchFromBranch(String branch1, String branch2) {

        }
    }
}
