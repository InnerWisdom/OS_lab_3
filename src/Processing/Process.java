package Processing;

import Memory.MemDispatcher;
import Memory.MemPage;

import java.util.Random;

public class Process
{
    private static int nextPID = 0;
    private static final Random rnd = new Random();
    private MemPage[] pages;

    private static final int minDataAmount = 32;
    private static final int maxDataAmount = 256;
    private static final int minWorkAmount = 2;
    private static final int maxWorkAmount = 6;

    private int workAmount;
    private boolean disposed = false;
    private final int memoryAmount;
    private final int pid;

    public boolean getWork() { return workAmount > 0; }
    public boolean getDisposal() { return disposed; }
    public int getRequiredMemoryAmount() { return memoryAmount; }
    public int getPID() { return pid; }


    public Process(MemDispatcher memDispatcher)
    {
        pid = nextPID++;
        workAmount = rnd.nextInt(maxWorkAmount-minWorkAmount) + minWorkAmount;
        memoryAmount = rnd.nextInt(maxDataAmount-minDataAmount) + minDataAmount;
        pages = memDispatcher.allocateMemory(this);
    }

    public void doWork()
    {
        if(getWork())
        {
            workAmount--;
            for (int i = 0; i < pages.length; i++)
            {
                if (rnd.nextBoolean())
                {
                    byte[] bytes = new byte[rnd.nextInt(MemDispatcher.getPageSize())];
                    rnd.nextBytes(bytes);
                    pages[i].setBytes(rnd.nextInt(MemDispatcher.getPageSize()), bytes);
                }
            }
        }
        else
        {
            dispose();
        }
    }

    private void dispose()
    {
        for (int i = 0; i < pages.length; i++)
        {
            pages[i].dispose();
        }
        disposed = true;
    }


    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PID=" + pid);
        sb.append(System.lineSeparator());
        if(!getDisposal())
        {
            sb.append("Pages:");
            sb.append(System.lineSeparator());
            for (int i = 0; i < pages.length; i++)
            {
                sb.append(pages[i].toString());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}

