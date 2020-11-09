package Processing;

import Memory.MemDispatcher;
import Memory.MemGetter;

import java.util.ArrayList;
import java.util.Random;

public class SysCore {
    private static final Random rnd = new Random();

    private static final int minProcessCount = 2;
    private static final int maxProcessCount = 8;

    private int processCount;
    private ArrayList<Process> processes;
    private MemDispatcher memDispatcher;

    private boolean working = false;
    private int I = 0;

    public SysCore() {
        processes = new ArrayList<Process>();
        processCount = rnd.nextInt(maxProcessCount + minProcessCount) / 2;
        memDispatcher = new MemDispatcher();
        checker();
    }

    public void checker() {
        boolean num = true;

        System.out.println(processInitiator());
        System.out.println(getPhysicalMemoryCondition());
        System.out.println(getVirtualMemoryCondition());

        while (num) {
            System.out.println(next());
            if (!num) break;

            System.out.println(getPhysicalMemoryCondition());
            if (!num) break;

            System.out.println(getVirtualMemoryCondition());
            num=false;
        }
    }

    public String processInitiator() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < processCount; i++) {
            Process process = new Process(memDispatcher);
            processes.add(process);
            sb.append("Process initializing:");
            sb.append(System.lineSeparator() + process.toString() + System.lineSeparator());
        }
        return sb.toString();
    }


    public String next() {
        if (processes.size() > 0) {
            if (I < processes.size()) {
                StringBuilder sb = new StringBuilder();
                Process current = processes.get(I);
                sb.append("Performing:" + System.lineSeparator());
                if (working) {
                    current.doWork();
                    I++;
                }
                working = !working;
                sb.append(current.toString());
                if (current.getDisposal()) {
                    sb.append("Process has ended");
                    processes.remove(current);
                }
                return sb.toString();
            } else {
                I = 0;
                return next();
            }
        } else {
            return "All processes have ended";
        }
    }

    public String getPhysicalMemoryCondition() {
        return memDispatcher.getMem(true);
    }

    public String getVirtualMemoryCondition() {
        return memDispatcher.getMem(false);
    }
}
