import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FtpService {

    @Autowired
    private FtpConfig ftpConfig;

    private FTPClient connectAndLogin() throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpConfig.getServer(), ftpConfig.getPort());
        ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    public void uploadFile(List<String> data, String fileName) throws IOException {
        String remoteFilePath = ftpConfig.getUploadFolder() + "/" + fileName;

        try (FTPClient ftpClient = connectAndLogin()) {
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);

            Workbook workbook;
            if (inputStream != null) {
                workbook = WorkbookFactory.create(inputStream);
            } else {
                workbook = new XSSFWorkbook();
            }

            Sheet sheet = workbook.createSheet("Sheet1");

            int rowCount = sheet.getLastRowNum();
            for (String item : data) {
                Row row = sheet.createRow(++rowCount);
                Cell cell = row.createCell(0);
                cell.setCellValue(item);
            }

            try (OutputStream outputStream = ftpClient.storeFileStream(remoteFilePath)) {
                workbook.write(outputStream);
            }

            ftpClient.completePendingCommand();
        }
    }

    public InputStream downloadExcel(String remoteFilePath) throws IOException {
        FTPClient ftpClient = connectAndLogin();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);

            if (!success) {
                throw new IOException("Failed to download file from FTP server.");
            }

            byte[] excelBytes = outputStream.toByteArray();
            return new ByteArrayInputStream(excelBytes);
        } finally {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

public boolean doesResultFileExist(String uploadedFileName) throws IOException {
    String successResultFileName = "success-" + uploadedFileName;
    String errorResultFileName = "error-" + uploadedFileName;

    String successRemoteResultFilePath = ftpConfig.getOutputFolder() + "/" + successResultFileName;
    String errorRemoteResultFilePath = ftpConfig.getOutputFolder() + "/" + errorResultFileName;

    try (FTPClient ftpClient = connectAndLogin()) {
        InputStream successInputStream = ftpClient.retrieveFileStream(successRemoteResultFilePath);
        InputStream errorInputStream = ftpClient.retrieveFileStream(errorRemoteResultFilePath);

        return (successInputStream != null || errorInputStream != null);
    }
}

      @Scheduled(fixedDelay = 60000)  // Check every minute, adjust as needed
    public void checkAndProcessResultFile() {
        try {
            List<String> uploadedFiles = getUploadedFiles(); // Assuming you have a method to get uploaded file names
            for (String uploadedFileName : uploadedFiles) {
                if (doesResultFileExist(uploadedFileName)) {
                    String resultFileName = generateResultFileName(uploadedFileName);
                    InputStream resultFileStream = downloadExcel(ftpConfig.getOutputFolder() + "/" + resultFileName);
                    // TODO: Add logic to process the result file (e.g., read and handle the file)

                    // Once processed, you may want to delete the result file from the "output" folder
                    deleteResultFile(resultFileName);
                }
            }
        } catch (IOException e) {
            // Handle any exceptions here
            e.printStackTrace();
        }
    }

    private List<String> getUploadedFiles() throws IOException {
        try (FTPClient ftpClient = connectAndLogin()) {
            FTPFile[] ftpFiles = ftpClient.listFiles(ftpConfig.getUploadFolder());
            List<String> uploadedFiles = new ArrayList<>();
            for (FTPFile ftpFile : ftpFiles) {
                if (ftpFile.isFile()) {
                    uploadedFiles.add(ftpFile.getName());
                }
            }
            return uploadedFiles;
        }
    }

    private void deleteResultFile(String resultFileName) throws IOException {
        try (FTPClient ftpClient = connectAndLogin()) {
            String resultFilePath = ftpConfig.getOutputFolder() + "/" + resultFileName;
            ftpClient.deleteFile(resultFilePath);
        }
    }

    private String generateResultFileName(String uploadedFileName) {
        String prefix = "error-";
        if (uploadedFileName.startsWith("success-")) {
            prefix = "success-";
        }

        String originalFileName = StringUtils.stripFilenameExtension(uploadedFileName.replaceFirst("^success-|^error-", ""));
        return prefix + originalFileName + ".xlsx";
    }

    public boolean doesResultFileExist() {
        String[] foldersToCheck = { "success", "error" };

        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Establish a session to the SFTP server
            session = jsch.getSession(ftpConfig.getUsername(), ftpConfig.getServer(), 22);
            session.setConfig("StrictHostKeyChecking", "no"); // Disable host key checking
            session.setPassword(ftpConfig.getPassword());
            session.connect();

            // Open a channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            for (String folder : foldersToCheck) {
                try {
                    channelSftp.cd("output/" + folder);
                    return true; // Folder exists, result file may be present
                } catch (SftpException e) {
                    // Folder doesn't exist or an exception occurred (e.g., folder not found)
                    // Continue to the next folder
                }
            }
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return false; // Result file not found in either success or error folders
    }

    public InputStream downloadExcel(String remoteFilePath) throws IOException {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Establish a session to the SFTP server
            session = jsch.getSession(ftpConfig.getUsername(), ftpConfig.getServer(), 22);
            session.setConfig("StrictHostKeyChecking", "no"); // Disable host key checking
            session.setPassword(ftpConfig.getPassword());
            session.connect();

            // Open a channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Download the Excel file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);

            byte[] excelBytes = outputStream.toByteArray();
            return new ByteArrayInputStream(excelBytes);
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
            throw new IOException("Failed to download file from SFTP server.");
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
}
