# **Gitlet Design Document**
author: Hamza Kundi and Solomon Cheung
<hr>
<hr>
<hr>

## **1. Classes and Data Structures**

### **Main**
Main class that contains input handling.

### **Gitlet**
Gitlet class that stores branches, current branch, and the staging and removal areas.
#### - Fields
1. static String GITLET_FOLDER: gitlet folder (.gitlet)
2. static String GITLET_OBJECT: where to store the Gitlet class instance in .gitlet
3. static String COMMIT_FOLDER: folder for commits with filenames being commit hashes.
4. static String BLOB_FOLDER: folder for all blobs with filenames being blob hashes.
5. HashMap<String, Branch> _branches: maps branch names to branches.
6. Branch _headBranch: current branch
7. StagingArea _stagingArea: staged file names.
8. StagingArea _removingArea: file names staged to be removed.

### **StagingArea**
Class for staging and removal areas.
#### - Fields
1. HashSet<String> _area: set of filenames in the staging area.

### **Commit**
Class defining a commit. Is serializable.
#### - Fields
1. String _message: commit log message.
2. String _parentCommitHash: hash for parent commit.
3. TimeStamp _timestamp: timestamp for commit.
4. HashMap<String, String> _trackedFiles: mapping of tracked file names to hash values for blobs.
5. boolean _isMerge: whether the commit is a merge.

### **Merge extends Commit**
Class defining a merge as a subclass of commit.
#### - Fields
1. String _parent2: hash for secondary parent commit for a merge.

### **Branch**
Class defining a branch.
#### - Fields
1. String _name: name of branch.
2. String _commitHash: hash for last commit of the branch.

### **Blob**
Class defining a file contents
#### - Fields
1. String _contents: contents of a file

### **Command**
Class that contains nested subclasses for each command.
#### - Fields
1. public static HashMap<String, Command> COMMAND_MAP: hashmap mapping command names to corresponding Command instances.
2. String _name: name that runs the command.
3. int _numArgs: number of arguments for command.
#### - Nested Subclasses
1. Init
2. Add
3. CommitCommand
4. Rm
5. Log
6. GlobalLog
7. Checkout
8. Find
9. Status
10. BranchCommand
11. RmBranch
12. MergeCommand
13. Reset

<hr>
<hr>
<hr>

## **2. Algorithms**

### **Main**
1. main(String[] args): load serialized data from Gitlet.gitletOjectFile. handle input.

### **Gitlet**
1. static String getHashFromFileName(String fileName): get the hash from the file specified by fileName in HEAD.
2. static Gitlet loadGitlet(): return the gitlet object file in the .gitlet folder
3. Commit getHead(): get the head commit currently being tracked. returns getHeadBranch().getCommit().
4. String getHeadHash(): get the hash of the head. returns getHeadBranch().getCommitHash().
4. Branch getHeadBranch(): get the head branch.
5. HashMap<String, Branch> getBranches(): get the map of all branches
6. Branch getBranch(String branch): get a branch by name.
7. StagingArea getStagingArea(): get staging area.
8. StagingArea getRemovingArea(): get removing area.
9.  void addBranch(Branch branch): add a branch to the _branches map.
10. void save(): save the gitlet object file in the .gitlet folder

### **StagingArea**
1. void stage(String fileName): add the filename blob map to the staging area.
2. void unstage(String fileName): remove filename from staging area.
3. void clear(): clear staging area.
4. HashSet<String> getFiles(): returns _area.

### Commit
1. Commit(String message, Commit parentCommit): initialize a commit and all of it's instance variables. stores the commit in .gitlet.
2. static Commit fromHash(String hash): load the commit from it's file in .gitlet.
3. static String getHashFromFileName(String fileName): get the blob hash of a tracked filename (stored in the tracked files).
4. static Commit fromAbrvHash(String hash): compares the hash to every file in the commit directory. if only one file matches, return it.
5. String getID(): compute and get the hash ID for the commit.
6. void setTimestamp(Timestamp timestamp): set the timestamp for the commit.
7. void save(): saves the commit in .gitlet.
8. boolean isTracked(String fileName): is the file being tracked in the current commit?
9. HashMap<String, String> getTrackedFiles(): returns a hashmap of tracked filenames mapped on to blob hashes.
10. Commit getParent(): returns the parent commit.

### Merge
1. Merge(String parent1, String parent2, String message): runs Commit's constructor first and then sets 2nd parent.
2. Commit getParent2(): returns the 2nd parent commit.

### Branch
1. Branch(String name, String commitHash): initialize and store a branch with a given name and head commit.
2. void setCommit(String commitHash): set the commit pointer for the branch
3. Commit getCommit(): deserialize the commit file pointed to by commitHash and return it.
4. String getCommitHash(): return the hash of the branch's pointer.
5. String getName(): return the name of the branch.

### Blob
1. static Blob fromHash(String hash): load the blob from it's file in .gitlet.
2. static Blob fromFileName(String fileName): load the file into a blob.
3. Blob(String content): initialize a blob and it's contents
4. String getId(): compute and get the hash ID for the blob.
5. void save(): saves the blob in .gitlet.

<hr>

## Command Algorithms
### Command
1. void run(String[] args): do nothing. meant to be overridden.
2. boolean validateArgs(String[] args): validate input format (length of args) using the numArgs instance variable. returns false if invalid arguments.

### Init
1. void run(String[] args): Initialize a gitlet repository. Create a gitlet folder. Create and store a new Gitlet instance, a new commit, and a new branch.

### Add
1. void run(String[] args): adds files to be staged for commit. checks if file exists (if specified). adds all files in directory if specified a directory. If file is already staged but different, overwrites it in the staging area. If file is identical to stored file, removes it from staging area. If the file is in the removing area, remove it from the removing area. Uses gitlet.isStaged and gitlet.getHashFromFileName. create blobs for staged files.

### CommitCommand
1. void run(String[] args): takes a snapshot of all tracked and staged files and creates a new commit. sets the new commits parent to the old head commit. tores the commit. Combines the old tracked files and the new staged files to make the new map of tracked files. clears staging and removing areas. creates new blobs.

### Rm
1. void run(String[] args): remove the file with the given name from the directory. if staged, unstage it. If the file is tracked by head, add to removing area. Uses gitlet.getStagingArea().isStaged, gitlet.getHead().isTracked.

### Log
1. void run(String[] args): recursively traverse the linked list of commits starting from the head, printing info about each commit.

### GlobalLog
1. void run(String[] args): go through every file in the commits directory in .gitlet using gitlet.Utils.plainFilenamesIn, deserialize it, and print relevant content.

### Checkout
1. void run(String[] args): runs relevant checkout method. If a fileName is specified but not a commit hash, checkout(commitHash, fileName) is run with the commit hash of the head. If nothing is specified runs checkout on the active branch.
2. void checkout(String commitHash, String fileName): overwrite fileName's contents with the contents of the blob with a hash of  commit._trackedFiles.get(fileName)
3. void checkout(String branchName): set gitlet.currentBranch to the branch name, and run checkout(fileName) on all the files in _trackedFiles. clears staging and removal area unless on branchName is current branch.

### Find
1. void run(String[] args): go through every commit file using gitlet.Utils.plainFilenamesIn in the commit directory, deserializing and checking against commit messages, storing those that match in an ArrayList. Afterwards, print out relevant info.

### Status
1. void run(String[] args):
2. void displayBranches(): goes through gitlet._branches, printing relevant info, taking gitlet.getHeadBranch().getName() into account.
3. void displayStagedFiles(): goes through gitlet.getStagingArea().getFiles() and print relevant content.
4. void displayRemovedFiles(): goes through gitlet.getRemovingArea().getFiles() and print relevant content.
5. void displayModificationsNotStagedForCommit(): uses Command.MergeCommand.modified to find modified files and print relevant content.
6. void displayUntrackedFiles(): uses Command.MergeCommand.ANotInB to find untracked files.

### BranchCommand
1. void run(String[] args): creates a new branch with the given name and adds it to to the gitlet._branches hashmap. points the branch at the current pointer.

### RmBranch
1. void run(String[] args): removes the branch with the specified name from the gitlet._branches hashmap. Makes sure the branch doesn't equal the current branch.

### Reset
1. void run(String[] args): sets the pointer for the current branch to the given commit ID, then runs the "checkout" command with no arguments. goes through all files, removing any that aren't keys in commit.getTrackedFiles(). clears staging and removal area.

### MergeCommand
1. void run(String[] args): if split point is given branch, does nothing. if split point is gitlet.getHead(), checks out given branch. prints relevant messages.
- ANotInB(modified(given, split), modified(head, split)) -> changed to given + staged.
- ANotInB(ANotInB(given, splitPoint), head) -> changed to given + staged.
- ANotInB(head, ANotInB(splitPoint, given)) -> removed + untracked.
- modified(modified(head, splitPoint), modified(given, splitPoint)) -> merge conflict -> special replace contents

2. static Commit getSplitPoint(Commit a, Commit b): get the split point of the 2 commits.
3. static HashMap<String, String> intersection(HashMap<String, String> A, HashMap<String, String> B):
   intersection of tracked filenames in commit A and B, with blobs from A.
4. static HashMap<String, String> ANotInB(HashMap<String, String> A, HashMap<String, String> B):
   list of tracked file names in A that aren't in B, with blobs from A.
5. static HashMap<String, String> modified(HashMap<String, String> A, HashMap<String, String> B):
   lists files in A that have changed from B or are not in B. maps to blobs from A. deleted files are included with empty blob.

<hr>
<hr>
<hr>

## **3. Persistence**

### init
- Create a new Gitlet object that gets saved in the .gitlet directory under the name Gitlet.GITLET_OBJECT
- Create a new commit in the .commit directory
- Create a new branch that is then stored in HashMap _branches so we can access them later.

### add [file]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Take [file] and store it in HashMap _stagingArea where it can be accessed later.
- Rewrite the gitlet object file to accomodate for this new staging area.

### commit [message]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Creates a new commit on the current branch and stores it in a file in Gitlet.COMMIT_FOLDER so it can be accessed later.
- Remove all elements from the staging area, so that future commits won't take a snapshot of unstaged files.
- Modifies the current branch to point to the new commit.
- Rewrite the gitlet object file to accomodate for the modified branch.

### rm [file]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Removes [file] from the staging area if it is already in there.
- Adds [file] to the removing area.
- Destructively removes [file] from the gitlet directory, so that it cannot be retrived from future commits (unless you reset).
- Rewrite the gitlet object file.

### checkout [file] | [commit-id] [file] | [branch]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Moves the current branch pointer to branch [name]
- Rewrite the gitlet object file to accomodate.

### branch [branch]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Creates a new branch with the [branch] and adds it to the gitlet._branches hashmap.
- Updates the branch pointer to point to the current pointer.
- Rewrite the gitlet object.

### rm-branch [branch]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- Deletes the branch named [branch] from the gitlet._branches map.
- Rewrite the gitlet object.

### merge [branch]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- May rewrite file contents or add files.
- Creates a new commit and stores it in a file in Gitlet.COMMIT_FOLDER so it can be accessed later.
- May rewrite gitlet object after modifying staging and removing areas.

### reset [commit-id]
- Deserializes the gitlet object from it's file in .gitlet/{Gitlet.COMMIT_FOLDER}
- May rewrite file contents.
- Rewrites gitlet object after possibly modifying branches, staging areas, and removing areas..

## **4. Design Diagram**

![Gitlet Design Diagram](gitlet-design.png)