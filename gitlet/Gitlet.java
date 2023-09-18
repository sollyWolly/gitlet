package gitlet;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.sql.Timestamp;
import java.io.File;
import java.io.Serializable;

/**
 * gitlet, a worse version of git.
 *
 * @author Solomon Cheung
 */
public class Gitlet implements Serializable {

    /**
     * gitlet folder (.gitlet).
     */
    static final String GITLET_FOLDER = ".gitlet";
    /**
     * gitlet object filename, not to be confused with GITLET_OBJECT.
     */
    static final String GITLET_OBJECT_FILENAME = "gitlet_object";
    /**
     * absolute gitlet object fileName.
     */
    static final String ABS_GITLET_OBJECT_FILENAME = GITLET_FOLDER
        + "/" + GITLET_OBJECT_FILENAME;
    /**
     * where to store the Gitlet class instance in .gitlet.
     */
    public static final Gitlet GITLET_OBJECT = loadGitlet();
    /**
     * folder for commits with filenames being commit hashes.
     */
    static final String COMMIT_FOLDER = "commits";
    /**
     * absolute gitlet commit folder.
     */
    static final String ABS_COMMIT_FOLDER = GITLET_FOLDER
        + "/" + COMMIT_FOLDER;
    /**
     * folder for all blobs with filenames being blob hashes.
     */
    static final String BLOB_FOLDER = "blobs";
    /**
     * absolute gitlet blob folder.
     */
    static final String ABS_BLOB_FOLDER = GITLET_FOLDER
        + "/" + BLOB_FOLDER;

    /**
     * maps branch names to branches.
     */
    private TreeMap<String, Branch> _branches = new TreeMap<>();

    /**
     * current branch.
     */
    private Branch _headBranch = null;

    /**
     * staged file names.
     */
    private StagingArea _stagingArea = new StagingArea();

    /**
     * file names staged to be removed.
     */
    private StagingArea _removingArea = new StagingArea();

    /**
     * get the hash from the file specified by fileName in HEAD.
     * @param fileName
     * @return gethashinHeadFromFileName
     */
    static String getHashInHeadFromFileName(String fileName) {
        return Gitlet.GITLET_OBJECT.getHead().getTrackedFiles().get(fileName);
    }

    /**
     * return the gitlet object file in the .gitlet folder.
     *
     * @return an instance of gitlet.
     * @throws IllegalArgumentException file not found.
     * @see Utils#readObject
     */
    static Gitlet loadGitlet() {
        File gitletFile = new File(System.getProperty("user.dir"),
            ABS_GITLET_OBJECT_FILENAME);
        if (!gitletFile.exists()) {
            return new Gitlet();
        }
        return Utils.readObject(gitletFile, Gitlet.class);
    }

    /**
     * Get the head commit currently being tracked.
     * Returns getHeadBranch().getCommit().
     * @return headbranchCommit
     */
    public Commit getHead() {
        return getHeadBranch().getCommit();
    }

    /**
     * get the hash of the head. returns getHeadBranch().getCommitHash().
     * @return headbranchCommitHash
     */
    public String getHeadHash() {
        return getHeadBranch().getCommitHash();
    }

    /**
     * get the head branch.
     * @return headBranch
     */
    public Branch getHeadBranch() {
        return _headBranch;
    }

    /**
     * get the map of all branches.
     * @return TreeMap of branches
     */
    TreeMap<String, Branch> getBranches() {
        return _branches;
    }

    /**
     * get a branch by name.
     * @param branch
     * @return an instance of branch.
     */
    public Branch getBranch(String branch) {
        return _branches.get(branch);
    }

    /**
     * get staging area.
     * @return stagingArea
     */
    public StagingArea getStagingArea() {
        return _stagingArea;
    }

    /**
     * get removing area.
     * @return removingArea
     */
    public StagingArea getRemovingArea() {
        return _removingArea;
    }

    /**
     * sets the headbranch to b.
     * @param b
     */
    public void setHeadBranch(Branch b) {
        _headBranch = b;
    }

    /**
     * add a branch to the _branches map.
     * @param branch
     */
    public void addBranch(Branch branch) {
        _branches.put(branch.getName(), branch);
    }

    /**
     * remove a branch.
     * @param branch
     * @return removed branch
     */
    public Branch removeBranch(Branch branch) {
        return _branches.remove(branch.getName());
    }

    /**
     * save the gitlet object file in the .gitlet folder.
     */
    public void save() {
        Utils.writeObject(new File(System.getProperty("user.dir"),
            ABS_GITLET_OBJECT_FILENAME), this);
    }

    public java.util.HashMap<String, Boolean> unstagedFiles() {
        java.util.HashMap<String, Boolean> files = new
            java.util.HashMap<String, Boolean>();

        for (Entry<String, String> stagedFileEntry : _stagingArea.getFiles().entrySet()) {
            File file = new File(System.getProperty("user.dir"), stagedFileEntry.getKey());

            if (!file.exists()) {
                if (!_removingArea.hasFile(stagedFileEntry.getKey())) {
                    files.put(stagedFileEntry.getKey(), true);
                }
                continue;
            }

            Blob tmpBlob = Blob.fromFileName(stagedFileEntry.getKey());

            if (!tmpBlob.getId().equals(stagedFileEntry.getValue())) {
                files.put(stagedFileEntry.getKey(), false);
            }
        }

        for (Entry<String, String> commitFileEntry : getHead().getTrackedFiles().entrySet()) {
            if (_stagingArea.isStaged(commitFileEntry.getKey())) {
                continue;
            }

            File file = new File(System.getProperty("user.dir"), commitFileEntry.getKey());

            if (!file.exists()) {
                if (!_removingArea.hasFile(commitFileEntry.getKey())) {
                    files.put(commitFileEntry.getKey(), true);
                }
                continue;
            }
            Blob tmpBlob = Blob.fromFileName(commitFileEntry.getKey());
            if (!tmpBlob.getId().equals(commitFileEntry.getValue())) {
                files.put(commitFileEntry.getKey(), false);
            }
        }

        return files;
    }


    public java.util.List<String> untrackedFiles() {
        java.util.ArrayList<String> files = new java.util.ArrayList<String>();
        java.util.List<String> localFiles = Utils.plainFilenamesIn(
            new File(System.getProperty("user.dir")));
        for (String file : localFiles) {
            if (getHead().getTrackedFiles().get(file) == null
                && !_stagingArea.isStaged(file)) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * blob file.
     */
    public static class Blob implements Serializable {
        /**
         * contents of a file.
         */
        private String _contents;

        /**
         * contents accessor.
         * @return contents
         */
        public String getContents() {
            return  _contents;
        }

        /**
         * initialize a blob and its contents.
         * @param content
         */
        Blob(String content) {
            _contents = content;
        }

        /**
         * load the blob from its file in .gitlet.
         * @see Utils#readContents(File)
         * @param hash
         * @return blob
         */
        static Blob fromHash(String hash) {
            File targetFile = new File(System.getProperty("user.dir"),
                ABS_BLOB_FOLDER + "/" + hash);

            if (!targetFile.exists()) {
                throw Utils.error("Blob::fromHash FILE MISSING "
                + "SOMETHING WENT WRONG @ %s", ABS_BLOB_FOLDER + "/" + hash);
            }

            return new Blob(Utils.readContentsAsString(targetFile));
        }

        /**
         * load the file into a blob.
         * @param fileName
         * @throws IllegalArgumentException file not valid, ioexcept
         * @return Blob Object from file
         */
        static Blob fromFileName(String fileName) {
            File stagingFile = new File(System.getProperty("user.dir"),
                fileName);
            return new Blob(Utils.readContentsAsString(stagingFile));
        }

        /**
         * compute and get the hash ID for the blob.
         * @see Utils#sha1
         * @return sha1
         */
        String getId() {
            return Utils.sha1(_contents);
        }

        /**
         * saves the blob in .gitlet.
         * @see Utils#writeContents
         */
        void save() {
            Utils.writeContents(new File(System.getProperty("user.dir"),
                ABS_BLOB_FOLDER + "/" + getId()),
                _contents);
        }
    }

    /**
     * Staging area.
     */
    class StagingArea implements Serializable {
        /**
         * set of filenames in the staging area.
         * (the value is the blob hash string).
         */
        private TreeMap<String, String> _area = new TreeMap<String, String>();

        /**
         * area accessor.
         * @return _area
         */
        public TreeMap<String, String> getArea() {
            return _area;
        }

        /**
         * add the filename blob map to the staging area.
         * @param fileName file name
         */
        void stage(String fileName) {
            Blob file = Blob.fromFileName(fileName);
            _area.put(fileName, file.getId());
        }

        /**
         * add the filename blob map to the staging area.
         * @param fileName file name
         * @param hash
         */
        void stage(String fileName, String hash) {
            _area.put(fileName, hash);
        }

        /**
         * remove filename from staging area.
         * @param fileName
         */
        void unstage(String fileName) {
            _area.remove(fileName);
        }

        /**
         * clear staging area.
         */
        void clear() {
            _area.clear();
        }

        /**
         * check if area has the file key.
         * @param fileName
         * @return
         */
        boolean hasFile(String fileName) {
            return _area.containsKey(fileName);
        }

        /**
         * Alias of hasFile.
         *
         * @param fileName
         * @return
         */
        boolean isStaged(String fileName) {
            return hasFile(fileName);
        }

        /**
         * check if area contains file value.
         * @param fileName
         * @return
         */
        boolean hasBlob(String fileName) {
            return _area.containsValue(fileName);
        }

        /**
         * returns _area.
         */
        TreeMap<String, String> getFiles() {
            return _area;
        }
    }

    static class Commit implements Serializable {
        /**
         * commit log message.
         */
        private String _message;
        /**
         * accessor for message.
         * @return _message
         */
        public String getMessage() {
            return _message;
        }

        /**
         * hash for parent commit.
         */
        private Commit _parentCommit;

        public Commit getParentCommit() {
            return _parentCommit;
        }

        /**
         * timestamp for commit.
         */
        private Timestamp _timestamp;

        public Timestamp getTimestamp() {
            return _timestamp;
        }

        /**
         * mapping of tracked file names to hash values for blobs.
         */
        private HashMap<String, String> _trackedFiles = new
            HashMap<String, String>();

        public HashMap<String, String> getTrackedFiles() {
            return _trackedFiles;
        }

        /**
         * whether the commit is a merge.
         */
        protected boolean _isMerge;

        /**
         * shhhhh we don't talk about this.
         */
        private static int magicNumber = 40;

        /**
         * blingo.
         * @return if it's a merge or not
         */
        public boolean isMerge() {
            return _isMerge;
        }

        /**
         * Initialize a commit and all of it's instance variables.
         * Stores the commit in .gitlet.
         * @param message
         * @param parentCommit
         */
        Commit(String message, Commit parentCommit) {
            _message = message;
            _parentCommit = parentCommit;
            if (parentCommit != null) {
                for (java.util.Map.Entry<String, String> entry
                    : parentCommit.getTrackedFiles().entrySet()) {
                    _trackedFiles.put(entry.getKey(), entry.getValue());
                }
            }
        }

        /**
         * load the commit from it's file in .gitlet.
         * @param hash
         * @return Object from hash
         */
        static Commit fromHash(String hash) {
            if (hash.length() < magicNumber) {
                for (String a : Utils.plainFilenamesIn(ABS_COMMIT_FOLDER)) {
                    if (a.substring(0, hash.length()).equals(hash)) {
                        hash = a;
                        break;
                    }
                }
            }

            File targetFile = new File(System.getProperty("user.dir"),
                ABS_COMMIT_FOLDER + "/" + hash);

            if (!targetFile.exists()) {
                throw Utils.error("Commit::fromHash FILE MISSING "
                    + "SOMETHING WENT WRONG @ %d",
                    ABS_COMMIT_FOLDER + "/" + hash);
            }

            return Utils.readObject(targetFile, Commit.class);
        }

        /**
         * get the blob hash of a tracked filename
         *  (stored in the tracked files).
         * @param fileName
         * @return file from tracked files
         */
        String getHashFromFileName(String fileName) {
            return _trackedFiles.get(fileName);
        }

        /**
         * compute and get the hash ID for the commit.
         *
         * @return hashID
         */
        String getID() {
            return Utils.sha1(Utils.serialize(this));
        }

        /**
         * set the timestamp for the commit.
         * @param timestamp
         */
        void setTimestamp(Timestamp timestamp) {
            this._timestamp = timestamp;
        }

        /**
         * saves the commit in .gitlet.
         *
         */
        void save() {
            Utils.writeObject(new File(System.getProperty("user.dir"),
                ABS_COMMIT_FOLDER + "/" + getID()), this);
        }

        /**
         * is the file being tracked in the current commit?
         *
         * @param fileName file name
         * @return true if being tracked.
         */
        boolean isTracked(String fileName) {
            return _trackedFiles.containsKey(fileName);
        }

        /**
         * track this file in this commit.
         *
         * @param fileName file name
         * @param sha1
         * @return return value of put()
         */
        String trackFile(String fileName, String sha1) {
            return _trackedFiles.put(fileName, sha1);
        }

        /**
         * untrack this file from this commit, by removing it.
         *
         * @param fileName
         * @return removed file
         */
        String unTrackFile(String fileName) {
            return _trackedFiles.remove(fileName);
        }

        /**
         * returns the parent commit.
         */
        Commit getParent() {
            return _parentCommit;
        }

        public String toString() {
            java.text.SimpleDateFormat dateFormat = new
                java.text.SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

            return String.format(
                "commit %s\nDate: %s\n%s",
                getID(),
                dateFormat.format(_timestamp),
                _message
            );
        }
    }

    /**
     * NOTE: do not bother! (for now, until friday)
     *
     */
    static class Merge extends Commit {
        /**
         * hash for secondary parent commit for a merge.
         */
        private Commit _parent2;

        /**
         * runs Commit's constructor first and then sets 2nd parent.
         * @param parent1
         * @param parent2
         * @param message
         */
        Merge(Commit parent1, Commit parent2, String message) {
            super(message, parent1);
            _parent2 = parent2;
            _isMerge = true;
        }

        /**
         * returns the 2nd parent commit.
         */
        Commit getParent2() {
            return _parent2;
        }

        public String toString() {
            java.text.SimpleDateFormat dateFormat = new
                java.text.SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

            return String.format(
                "commit %s\nMerge: %s %s\nDate: %s\n%s",
                getID(),
                getParentCommit().getID().substring(0, 7),
                getParent2().getID().substring(0, 7),
                dateFormat.format(getTimestamp()),
                getMessage());
        }
    }

    static class Branch implements java.io.Serializable {
        /**
         * name of branch.
         */
        private String _name;
        /**
         * hash for last commit of the branch.
         */
        private String _commitHash;

        /**
         * initialize and store a branch with a given name and head commit.
         * @param name
         * @param commitHash
         */
        Branch(String name, String commitHash) {
            _name = name;
            _commitHash = commitHash;
        }

        /**
         * deserialize the commit file pointed to by commitHash and return it.
         *
         */
        Commit getCommit() {
            File targetFile = new File(System.getProperty("user.dir"),
                ABS_COMMIT_FOLDER + "/" + _commitHash);

            if (!targetFile.exists()) {
                throw Utils.error("Branch::getCommit FILE MISSING "
                + "SOMETHING WENT WRONG @ %s",
                ABS_COMMIT_FOLDER + "/" + _commitHash);
            }

            return Utils.readObject(targetFile, Commit.class);
        }

        /**
         * set the commit pointer for the branch.
         * @param commitHash
         */
        void setCommit(String commitHash) {
            _commitHash = commitHash;
        }

        /**
         * return the hash of the branch's pointer.
         */
        String getCommitHash() {
            return _commitHash;
        }

        /**
         * return the name of the branch.
         *
         * @return
         */
        String getName() {
            return _name;
        }
    }
}
