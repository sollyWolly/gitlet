package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Solomon Cheung
 */
public class UnitTest {

    /**
     * Run the JUnit tests in the loa package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored)  {
        System.setProperty("user.dir",
            "/Users/solcheung/repo/proj3/testing_unit");
        System.exit(textui.runClasses(UnitTest.class));
    }

    /**
     * A dummy test to avoid complaint.
     */
    @Test
    public void placeholderTest() {
        System.out.println("Working Directory = "
            + System.getProperty("user.dir"));
    }

    public void addFileToStagingTest() {
        Gitlet.Blob b = Gitlet.Blob.fromFileName("tester.py");
        assertEquals("bad sha1sum",
            "d67868685a420fe0b06a8107456c7b4c074a83d5",
            b.getId());
    }
}
