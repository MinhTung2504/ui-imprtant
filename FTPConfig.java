import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FtpConfig {

    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port}")
    private int port;

    @Value("${ftp.username}")
    private String username;

    @Value("${ftp.password}")
    private String password;

    @Value("${ftp.uploadFolder}")
    private String uploadFolder;  // Fixed upload folder on the FTP server

    // getters and setters
    public String getUploadFolder() {
        return uploadFolder;
    }
}
