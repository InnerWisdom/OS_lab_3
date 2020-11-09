package Memory;

import java.util.*;
import java.util.function.Function;
import Processing.Process;

public class MemDispatcher {
    public static final int printBytesPerRow = 8;
    private static final int pageSize = 32;
    private static final int memoryAmount = 256;
    private static final int swapFileSize = 2048;
    public HashMap<MemPage, byte[]> virtualPages;
    protected byte[] physicalMemory;
    private MemPage[] physicalPages;

    public MemDispatcher() {
        physicalMemory = new byte[memoryAmount];
        physicalPages = new MemPage[memoryAmount / pageSize];

        virtualPages = new HashMap<MemPage, byte[]>();
    }

    public static int getPageSize() {
        return pageSize;
    }

    public static int getMemoryAmount() {
        return memoryAmount;
    }

    private static int getTotalPageCount() {
        return memoryAmount / pageSize + swapFileSize / pageSize;
    }

    public static int getSwapFileSize() {
        return swapFileSize;
    }

    private int getPageCount(MemPage[] pages, Function<MemPage, Boolean> function) {
        int result = 0;
        for (int i = 0; i < pages.length; i++) {
            if (function.apply(pages[i])) {
                result++;
            }
        }
        return result;
    }

    private int getFreePhysicalPageCount() {
        return getPageCount(physicalPages, mp -> mp == null);
    }

    private int getUsedPhysicalPageCount() {
        return getPageCount(physicalPages, mp -> mp != null);
    }

    private int getTotalPhysicalPageCount() {
        return physicalPages.length;
    }

    private int getFreePhysicalPageID() {
        for (int i = 0; i < physicalPages.length; i++) {
            if (physicalPages[i] == null)
                return i;
        }
        return -1;
    }

    private boolean isPhysicalPageEqualToVirtualPage(MemPage page) {
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

    private int unloadMemoryPage(MemPage page) {
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

    private void loadMemoryPage(MemPage page) {
        int freePage = getFreePhysicalPageID();
        if (freePage > -1) {
            physicalPages[freePage] = page;
            byte[] bytes = virtualPages.get(page);
            page.setPhysicalPageID(freePage);

            for (int i = 0; i < pageSize; i++) {
                physicalMemory[freePage * pageSize + i] = bytes[i];
            }
        }
    }

    public MemPage getLeastRecentlyUsedMemoryPage() {
        MemPage LRU = physicalPages[0];
        for (int i = 0; i < physicalPages.length; i++) {
            if (physicalPages[i] != null) {
                if (LRU == null || LRU.getLastAccess() > physicalPages[i].getLastAccess()) {
                    LRU = physicalPages[i];
                }
            }
        }
        return LRU;
    }

    private void swapPages(MemPage pageToLoad) {
        MemPage pageToUnload = getLeastRecentlyUsedMemoryPage();
        unloadMemoryPage(pageToUnload);
        loadMemoryPage(pageToLoad);
    }

    private MemPage[] allocatePhysicalMemory(Process process, int pageCount) {
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

    public MemPage[] allocateMemory(Process process) {
        double pageCountRequiredDiv = process.getRequiredMemoryAmount() / pageSize;
        int pageCountRequiredMod = process.getRequiredMemoryAmount() % pageSize;
        int pageCountRequired = (int) pageCountRequiredDiv;
        if (pageCountRequiredMod > 0) {
            pageCountRequired++;
        }

        if (getFreePhysicalPageCount() >= pageCountRequired) {
            MemPage[] MemPages = allocatePhysicalMemory(process, pageCountRequired);
            return MemPages;
        } else {
            ArrayList<MemPage> MemPages = new ArrayList<MemPage>();
            int physicalPageCount = getFreePhysicalPageCount();
            MemPage[] pages = allocatePhysicalMemory(process, physicalPageCount);
            Collections.addAll(MemPages, pages);
            for (int i = 0; i < pageCountRequired - physicalPageCount; i++) {
                int physicalID = unloadMemoryPage(getLeastRecentlyUsedMemoryPage());
                MemPage MemPage = new MemPage(this, process, physicalID);
                MemPages.add(MemPage);
                physicalPages[physicalID] = MemPage;
                clearData(MemPage);
            }
            return MemPages.toArray(pages);
        }
    }

    public void setBytes(MemPage page, int virtualPosition, byte[] data) {
        if (!page.isInPhysicalMemory()) {
            if (getFreePhysicalPageCount() == 0) {
                swapPages(page);
            } else {
                loadMemoryPage(page);
            }
        }
        for (int i = 0; virtualPosition + i < pageSize && i < data.length; i++) {
            int physicalPosition = virtualPosition + i + page.getPhysicalPageID() * pageSize;
            physicalMemory[physicalPosition] = data[i];
        }
    }

    public byte[] getBytes(MemPage page) {
        if (!page.isInPhysicalMemory()) {
            if (getFreePhysicalPageCount() == 0) {
                swapPages(page);
            } else {
                loadMemoryPage(page);
            }
        }
        byte[] data = new byte[pageSize];
        for (int i = 0; i < pageSize; i++) {
            int physicalPosition = i + page.getPhysicalPageID() * pageSize;
            data[i] = physicalMemory[physicalPosition];
        }
        return data;
    }

    private void clearData(MemPage page) {
        if (page.isInPhysicalMemory()) {
            for (int i = 0; i < pageSize; i++) {
                int physicalPosition = i + page.getPhysicalPageID() * pageSize;
                physicalMemory[physicalPosition] = 0;
            }
        }
    }

    protected void dispose(MemPage page) {
        clearData(page);
        if (page.isInPhysicalMemory()) {
            physicalPages[page.getPhysicalPageID()] = null;
        } else {
            virtualPages.remove(page);
        }
    }

    public String getPhysicalMemoryCondition() {
        StringBuilder sb = new StringBuilder();
        sb.append("Phis memory condition: ");
        sb.append(System.lineSeparator());
        for (int i = 0; i < physicalPages.length; i++) {
            if (physicalPages[i] == null) {
                sb.append("Page is not occupied");
                sb.append(System.lineSeparator());
                for (int j = 0; j < pageSize; j++) {
                    sb.append(physicalMemory[j + pageSize * i] + " ");
                    if ((j + 1) % printBytesPerRow == 0)
                        sb.append(System.lineSeparator());
                }
            } else {
                sb.append(physicalPages[i].toString());
            }
        }
        return sb.toString();
    }

    public String getVirtualMemoryCondition() {
        StringBuilder sb = new StringBuilder();
        sb.append("Swap file condition: ");
        sb.append(System.lineSeparator());
        Set<MemPage> pages = virtualPages.keySet();
        Iterator<MemPage> pageIterator = pages.iterator();
        while (pageIterator.hasNext()) {
            MemPage page = pageIterator.next();
            if (!page.isInPhysicalMemory())
                sb.append(page.toString());
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
