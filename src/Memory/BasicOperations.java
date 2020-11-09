package Memory;

import Processing.Process;

import java.util.HashMap;

public class BasicOperations extends MemDispatcher {

    public HashMap<MemPage, byte[]> virtualPages;
    protected byte[] physicalMemory;
    private int pageSize;
    private MemPage[] physicalPages;

    public BasicOperations(HashMap<MemPage, byte[]> virtualPages, MemPage[] physicalPages, int pageSize, byte[] physicalMemory) {
        this.virtualPages = virtualPages;
        this.pageSize = pageSize;
        this.physicalPages = physicalPages;
        this.physicalMemory = physicalMemory;
    }

    protected boolean isPhysicalPageEqualToVirtualPage(MemPage page) {
        if (virtualPages.containsKey(page)) {
            byte[] bytes = virtualPages.get(page);
            for (int i = 0; i < pageSize; i++) {
                if (bytes[i] != physicalMemory[page.getPhysicalPageID() * pageSize + i])
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    protected int unloadMemoryPage(MemPage page) {
        int physicalMemoryID = page.getPhysicalPageID();
        if (!virtualPages.containsKey(page)) {
            virtualPages.put(page, new byte[pageSize]);
        }
        physicalPages[physicalMemoryID] = null;

        if (!isPhysicalPageEqualToVirtualPage(page)) {
            byte[] bytes = virtualPages.get(page);
            for (int i = 0; i < pageSize; i++) {
                bytes[i] = physicalMemory[physicalMemoryID * pageSize + i];
            }
            virtualPages.replace(page, bytes);
        }
        page.setPhysicalPageID(-1);
        return physicalMemoryID;
    }

    protected void clearData(MemPage page) {
        if (page.isInPhysicalMemory()) {
            for (int i = 0; i < pageSize; i++) {
                int physicalPosition = i + page.getPhysicalPageID() * pageSize;
                physicalMemory[physicalPosition] = 0;
            }
        }
    }

    protected int getFreePhysicalPageID() {
        for (int i = 0; i < physicalPages.length; i++) {
            if (physicalPages[i] == null)
                return i;
        }
        return -1;
    }

    protected MemPage[] allocatePhysicalMemory(Process process, int pageCount) {
        MemPage[] result = new MemPage[pageCount];
        for (int i = 0; i < pageCount; i++) {
            int freePageID = getFreePhysicalPageID();
            MemPage MemPage = new MemPage(this, process, freePageID);
            result[i] = MemPage;
            physicalPages[freePageID] = MemPage;
            clearData(MemPage);
        }
        return result;
    }


}
