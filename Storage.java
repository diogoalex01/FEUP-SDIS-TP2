import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Storage implements Serializable{
    private List<BigInteger> myStoredFiles = Collections.synchronizedList(new ArrayList<BigInteger>());
    private List<BigInteger> askedFiles = Collections.synchronizedList(new ArrayList<BigInteger>());
    private int availableSpace;

    public Storage() {
        availableSpace = -1;
    }

    /**
     * Gets Available Space
     */
    public int getAvailableSpace() {
        return this.availableSpace;
    }

    /**
     * Sets available Space
     */
    public void setAvailableSpace(int space) {
        this.availableSpace = space;
    }

    /**
     * Gets Stored Chunks List
     */
    public List<BigInteger> getmyStoredFiles() {
        return this.myStoredFiles;
    }

    /**
     * Checks if chunk is backedup
     */
    public boolean hasFileStored(BigInteger fileId) {
        if (this.myStoredFiles.contains(fileId)) {
            return true;
        }

        return false;
    }

    /**
     * Gets Asked Chunks List
     */
    public List<BigInteger> getmyAskedFiles() {
        return this.askedFiles;
    }

    /**
     * Checks if it asked the peers to backup the file
     */
    public boolean hasAskedForFile(BigInteger fileID) {
        if (this.askedFiles.contains(fileID)) {
            return true;
        }
        
        return false;
    }

    /**
     * Adds a new asked chunk
     */
    public void addAskedFile(BigInteger fileId) {
        this.askedFiles.add(fileId);
    }

    /**
     * Adds a new stored chunk
     */
    public void addStoredFile(BigInteger fileId) {
        this.myStoredFiles.add(fileId);
    }

}
