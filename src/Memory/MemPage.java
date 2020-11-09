package Memory;

import Processing.Process;

public class MemPage {
    private static final int printBytesPerRow = MemDispatcher.printBytesPerRow;
    private static int pageSize = MemDispatcher.getPageSize();
    private final MemDispatcher memDispatcher;
    private final Process owner;
    private int physicalPageID;
    private long lastEdit;
    private long lastRead;

    public MemPage(MemDispatcher memDispatcher, Process owner, int pageID) {
        this.memDispatcher = memDispatcher;
        this.owner = owner;
        this.physicalPageID = pageID;
        lastRead = getNextTimeStamp();
        lastEdit = getNextTimeStamp();
    }

    private static long getNextTimeStamp() {
        return System.nanoTime();
    }

    public boolean isOwnedBy(Process process) {
        return owner == process;
    }

    public int getPhysicalPageID() {
        return physicalPageID;
    }

    protected void setPhysicalPageID(int ID) {
        physicalPageID = ID;
    }

    public int getPhysicalMemoryStartPosition() {
        return physicalPageID * pageSize;
    }

    public long getLastEdit() {
        return lastEdit;
    }

    public long getLastRead() {
        return lastRead;
    }

    public long getLastAccess() {
        if (lastRead > lastEdit)
            return lastRead;
        else
            return lastEdit;
    }

    public boolean isInPhysicalMemory() {
        if (physicalPageID > -1)
            return true;
        else
            return false;
    }

    public void setByte(int virtualPosition, byte data) {
        setBytes(virtualPosition, new byte[]{data});
    }

    public void setBytes(int virtualPosition, byte[] data) {
        lastEdit = getNextTimeStamp();
        memDispatcher.setBytes(this, virtualPosition, data);
    }

    public byte[] getBytes() {
        lastRead = getNextTimeStamp();
        return memDispatcher.getBytes(this);
    }

    public void dispose() {
        memDispatcher.dispose(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Page, PID=" + owner.getPID() + " Page_ID=" + physicalPageID);
        sb.append(System.lineSeparator());
        if (isInPhysicalMemory()) {
            sb.append("Is in phis memory");
            sb.append(System.lineSeparator());
            //так, а не getBytes(), что-бы не перезаписывать поле lastRead
            for (int i = 0; i < pageSize; i++) {
                sb.append(memDispatcher.physicalMemory[getPhysicalMemoryStartPosition() + i] + " ");
                if ((i + 1) % printBytesPerRow == 0)
                    sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        } else {
            sb.append("Is in swap file");
            sb.append(System.lineSeparator());
            byte[] bytes = memDispatcher.virtualPages.get(this);
            for (int i = 0; i < pageSize; i++) {
                sb.append(bytes[i] + " ");
                if ((i + 1) % printBytesPerRow == 0)
                    sb.append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
