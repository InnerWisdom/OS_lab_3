package Memory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class MemGetter {

    private int pageSize;
    private MemPage[] physicalPages;
    protected byte[] physicalMemory;
    public static final int printBytesPerRow=8;
    public HashMap<MemPage, byte[]> virtualPages;

    public MemGetter(int pageSize, MemPage[] physicalPages, byte[] physicalMemory, HashMap<MemPage,byte[]> virtualPages) {
        this.pageSize = pageSize;
        this.physicalPages=physicalPages;
        this.physicalMemory=physicalMemory;
        this.virtualPages=virtualPages;

    }
    public String getPhysicalMemoryCondition() {
        StringBuilder sb = new StringBuilder();
        sb.append("Phys memory condition: ");
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
