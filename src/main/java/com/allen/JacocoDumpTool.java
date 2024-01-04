package com.allen;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JacocoDumpTool {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private final static Logger logger = LoggerFactory.getLogger(JacocoDumpTool.class);

    @Option(name = "-a", aliases = "--address", usage = "address")
    public String address;

    @Option(name = "-p", aliases = "--port", required = true, usage = "port")
    public int port;

    @Option(name = "-t", aliases = "--interval", usage = "intervalTime")
    public int intervalTime;

    public int doMain(String[] args) throws Exception {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            dump(address, port, intervalTime);
            return 0;
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: ");
            parser.printUsage(System.err);
            System.out.println();
            System.err.println("Example: ");
            System.err.println("  java -jar JacocoDumpTool.jar -a 127.0.0.1 -p 4345 -t 1000");
            return 1;
        }
    }

    public void dump(String ADDRESS, int PORT, int TIME) throws IOException {
        logger.info("ADDRESS: {}, PORT: {}, INTERVAL: {}", ADDRESS, PORT, TIME);
        while (true) {
            final FileOutputStream localFile = new FileOutputStream("jacoco-" + counter.get() + ".exec");
            final ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);

            // Open a socket to the coverage agent:
            final Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
            final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
            final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
            reader.setSessionInfoVisitor(localWriter);
            reader.setExecutionDataVisitor(localWriter);

            // Send a dump command and read the response:
            writer.visitDumpCommand(true, true);  // reset置为true,表示每次执行dump之后重置覆盖率
            if (!reader.read()) {
                throw new IOException("Socket closed unexpectedly.");
            }
            socket.close();
            localFile.close();
            logger.info("dump执行完成");
            counter.getAndAdd(1);
            try {
                Thread.sleep(TIME);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.exit(new JacocoDumpTool().doMain(args));
    }
}
