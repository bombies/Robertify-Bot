package main.utils.ftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class FtpClient implements AutoCloseable {

    private final String server;
    private final int port;
    private final String user;
    private final String password;
    @Getter
    private Session client;
    private ChannelSftp sftp;

    public FtpClient(String server, int port, String user, String password) throws JSchException {
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;

        this.client = new JSch().getSession(user, server, port);
        this.client.setPassword(password);
    }

    public FtpClient(String server, String user, String password) throws IOException, JSchException {
        final var serverSplit = server.split(":");
        this.user = user;
        this.password = password;

        if (serverSplit.length < 2) {
            this.server = serverSplit[0];
            this.port = 22;
            this.client = new JSch().getSession(user, server, 22);
        } else {
            this.server = serverSplit[0];
            this.port = Integer.parseInt(serverSplit[1]);
            this.client = new JSch().getSession(user, server, port);
        }

        this.client.setPassword(password);
    }

    public void connect() throws JSchException {
        this.client.connect(5);
    }

    public ChannelSftp getSftp() throws JSchException {
        final var ret = (ChannelSftp) this.client.openChannel("sftp");
        this.sftp = ret;
        ret.connect(5);
        return ret;
    }

    @Override
    public void close() {
        if (this.sftp != null && this.sftp.isConnected())
            this.sftp.exit();
        this.client.disconnect();
    }
}
