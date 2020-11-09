package Memory;

import java.util.*;
import java.util.function.Function;
import Processing.Process;

public class MemDispatcher {
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

    public String getMem(boolean typeofMem){

        MemGetter memGetter= new MemGetter(pageSize,physicalPages,physicalMemory,virtualPages);
        if(typeofMem){
            return memGetter.getPhysicalMemoryCondition();
        }
        else {
            return memGetter.getVirtualMemoryCondition();
        }
    }

    public MemPage[] allocateMemory(Process process) {
        double pageCountRequiredDiv = process.getRequiredMemoryAmount() / pageSize;
        int pageCountRequiredMod = process.getRequiredMemoryAmount() % pageSize;
        int pageCountRequired = (int) pageCountRequiredDiv;
        if (pageCountRequiredMod > 0) {
            pageCountRequired++;
        }
        BasicOperations bas=new BasicOperations(virtualPages,physicalPages,pageSize,physicalMemory);
        if (getFreePhysicalPageCount() >= pageCountRequired) {
            MemPage[] MemPages = bas.allocatePhysicalMemory(process, pageCountRequired);
            return MemPages;
        } else {
            ArrayList<MemPage> MemPages = new ArrayList<MemPage>();
            int physicalPageCount = getFreePhysicalPageCount();
            MemPage[] pages = bas.allocatePhysicalMemory(process, physicalPageCount);
            Collections.addAll(MemPages, pages);
            for (int i = 0; i < pageCountRequired - physicalPageCount; i++) {
                int physicalID = bas.unloadMemoryPage(getLeastRecentlyUsedMemoryPage());
                MemPage MemPage = new MemPage(this, process, physicalID);
                MemPages.add(MemPage);
                physicalPages[physicalID] = MemPage;
                clear(MemPage);
            }
            return MemPages.toArray(pages);
        }
    }

    private int getFreePage(){
        BasicOperations bas=new BasicOperations(virtualPages,physicalPages,pageSize,physicalMemory);
        return bas.getFreePhysicalPageID();
    }

    private void loadMemoryPage(MemPage page) {
        int freePage=getFreePage();
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
        BasicOperations bas=new BasicOperations(virtualPages,physicalPages,pageSize,physicalMemory);
        bas.unloadMemoryPage(pageToUnload);
        loadMemoryPage(pageToLoad);
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

    protected void clear(MemPage page){
        BasicOperations bas=new BasicOperations(virtualPages,physicalPages,pageSize,physicalMemory);
        bas.clearData(page);
    }

    protected void dispose(MemPage page) {
        clear(page);
        if (page.isInPhysicalMemory()) {
            physicalPages[page.getPhysicalPageID()] = null;
        } else {
            virtualPages.remove(page);
        }
    }

}
