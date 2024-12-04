package com.backend.service;


import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

@Log4j2
@Service
@RequiredArgsConstructor
public class SftpService {

    @Value("${file-server.url}")
    private String SFTP_HOST;
    private static final int SFTP_PORT = 22;
    private static final String SFTP_USER = "sftpuser";
    private static final String SFTP_PASSWORD = "Lotte4!@12";

    private static final String BASE_SFTP_DIR = "uploads/";



    //user별 sftpUser 생성
    public boolean  createUserFolderOnSftp(String username) {
        String remoteDir = BASE_SFTP_DIR + username;

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // SFTP 서버에 폴더 생성
            channelSftp.mkdir(remoteDir);


            channelSftp.disconnect();
            session.disconnect();



            log.info("Folder created and permissions set successfully on SFTP: {}", remoteDir);
            return true;
        } catch (SftpException | JSchException e) {
            log.error("Failed to create folder on SFTP: {}", e.getMessage());
            return false;
        }
    }

    public boolean deleteUserFolderOnSftp(String username) {
        String remoteDir = BASE_SFTP_DIR + username;


        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 폴더 삭제 시 폴더 안에 파일이나 하위 폴더가 없어야 삭제 가능
            channelSftp.rmdir(remoteDir);

            channelSftp.disconnect();
            session.disconnect();

            log.info("Folder deleted successfully on SFTP: {}", remoteDir);
            return true;
        } catch (SftpException | JSchException e) {
            log.error("Failed to delete folder on SFTP: {}", e.getMessage());
            return false;
        }
    }





    public String createFolder(String folderName,String username) {

        String remoteDir = "";
        if(folderName.equals(username)){
            remoteDir = BASE_SFTP_DIR + username;
        }else{
            remoteDir = BASE_SFTP_DIR + username + "/"+ folderName;

        }
        log.info("remoteDIR!!! "+remoteDir);
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // SFTP 서버에 폴더 생성
            channelSftp.mkdir(remoteDir);
            log.info("Folder created successfully on SFTP: {}", remoteDir);

            channelSftp.disconnect();
            session.disconnect();

            log.info("Folder created and permissions set successfully on SFTP: {}", remoteDir);
            return remoteDir;
        } catch (Exception e) {
            log.error("Error while creating folder: {}", e.getMessage());
            return null;
        }
    }

    public boolean createNestedFoldersWithCommand(String nestedPath) {
        String remoteDir = BASE_SFTP_DIR + nestedPath; // 절대 경로로 조정
        String command = String.format("mkdir -p %s", remoteDir);

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);

            log.info("Executing command: {}", command);

            channelExec.connect();
            int exitStatus = channelExec.getExitStatus();
            channelExec.disconnect();
            session.disconnect();

            if (exitStatus == 0) {
                log.info("Nested folders created successfully with command: {}", remoteDir);
                return true;
            } else {
                log.error("Failed to create nested folders. Exit status: {}", exitStatus);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to create nested folders: {}", e.getMessage());
            return false;
        }
    }




  //사용자 생성시 root 폴더 생성 (폴더이름 -> username)
    public String createRootFolder(String folderName,String username) {

        String remoteDir = BASE_SFTP_DIR + username;
        log.info("remoteDIR!!! "+remoteDir);
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // SFTP 서버에 폴더 생성
            channelSftp.mkdir(remoteDir);
            log.info("Folder created successfully on SFTP: {}", remoteDir);

            channelSftp.disconnect();
            session.disconnect();

            log.info("Folder created and permissions set successfully on SFTP: {}", remoteDir);
            return remoteDir;
        } catch (Exception e) {
            log.error("Error while creating folder: {}", e.getMessage());
            return null;
        }
    }

    public String createNewFolder(String folderName,String parentPath) {
        String remoteDir = parentPath +"/"+ folderName;
        log.info("remoteDIR!!! "+remoteDir);
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // SFTP 서버에 폴더 생성
            channelSftp.mkdir(remoteDir);
            log.info("Folder created successfully on SFTP: {}", remoteDir);

            channelSftp.disconnect();
            session.disconnect();

            log.info("Folder created and permissions set successfully on SFTP: {}", remoteDir);
            return remoteDir;
        } catch (Exception e) {
            log.error("Error while creating folder: {}", e.getMessage());
            return null;
        }

    }


    public String uploadFile(String localFilePath, String remoteDir, String remoteFileName) {
        try {
            // SFTP 연결 설정
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 원격 디렉토리가 없는 경우 생성
            try {
                channelSftp.cd(remoteDir);
            } catch (SftpException e) {
                log.info("Remote directory does not exist. Creating directory: {}", remoteDir);
                channelSftp.cd(remoteDir);
            }

            // 파일 업로드
            channelSftp.put(localFilePath, remoteFileName);
            log.info("File uploaded successfully: {}/{}", remoteDir, remoteFileName);

            // 연결 종료
            channelSftp.disconnect();
            session.disconnect();
            return remoteDir+"/"+remoteFileName;
        } catch (JSchException | SftpException e) {
            log.error("File upload failed: {}", e.getMessage());
            return null;
        }
    }




    public boolean thumbnailFileUploads(String localFilePath,  String remoteFileName) {

       String remoteDir = BASE_SFTP_DIR+"thumbnails";

        try {
            // SFTP 연결 설정
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 원격 디렉토리가 없는 경우 생성
            try {
                channelSftp.cd(remoteDir);
            } catch (SftpException e) {
                log.info("Remote directory does not exist. Creating directory: {}", remoteDir);
                channelSftp.cd(remoteDir);
            }

            // 파일 업로드
            channelSftp.put(localFilePath, remoteFileName);
            log.info("File uploaded successfully: {}/{}", remoteDir, remoteFileName);

            // 연결 종료
            channelSftp.disconnect();
            session.disconnect();
            return true;
        } catch (JSchException | SftpException e) {
            log.error("File upload failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean downloadFile(String remoteDir, String remoteFileName, String localFilePath) {
        try {
            // SFTP 연결 설정
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 파일 다운로드
            String remoteFilePath = remoteDir + "/" + remoteFileName;
            channelSftp.get(remoteDir, localFilePath);
            log.info("File downloaded successfully: {} -> {}", remoteFilePath, localFilePath);

            // 연결 종료
            channelSftp.disconnect();
            session.disconnect();
            return true;
        } catch (JSchException | SftpException e) {
            log.error("File download failed: {}", e.getMessage());
            return false;
        }
    }


    private long calculateSizeRecursive(ChannelSftp channelSftp, String username) throws SftpException {
        long totalSize = 0;

        String path = BASE_SFTP_DIR+username;
        Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(path);

        for (ChannelSftp.LsEntry entry : entries) {
            String fileName = entry.getFilename();

            // 현재 디렉토리와 상위 디렉토리 제외
            if (".".equals(fileName) || "..".equals(fileName)) {
                continue;
            }

            if (entry.getAttrs().isDir()) {
                // 디렉토리인 경우 재귀적��로 크기 계산
                totalSize += calculateSizeRecursive(channelSftp, path + "/" + fileName);
            } else {
                // 파일 크기 합산
                totalSize += entry.getAttrs().getSize();
            }
        }

        return totalSize;

    }


    public boolean renameFolder(String currentPath, String newPath){
        try {
            // SFTP 연결 설정
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            channelSftp.rename(currentPath, newPath);
            System.out.println("폴더 이름이 성공적으로 변경되었습니다.");

            // 연결 종료
            channelSftp.disconnect();
            session.disconnect();
            return true;
        } catch (JSchException | SftpException e) {
            System.err.println("폴더 이름 변경 실패: " + e.getMessage());
            return false;
        }
    }

    public boolean delete(String path){
        try {
            // SFTP 연결 설정
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            String command = "rm -rf " + path;
            channelExec.setCommand(command);
            channelExec.connect();

            System.out.println("폴더와 하위 파일이 모두 삭제되었습니다.");

            channelExec.disconnect();
            session.disconnect();
            return true;
        } catch (JSchException e ) {
            System.err.println("폴더 삭제 실패: " + e.getMessage());
            return false;
        }

    }

    public long calculatedSize(String uid) {
        long size = 0;
        String path = BASE_SFTP_DIR + uid;

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
            session.setPassword(SFTP_PASSWORD);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();
            log.info("SFTP session connected to host: {}", SFTP_HOST);

            // SFTP 디렉토리 크기 계산 명령어
            String command = String.format("du -sh %s", path);
            log.info("Executing command: {}", command);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();

            // 명령어 실행 결과 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Command output: {}", line);
                // 명령어 결과가 "크기\t경로" 형식이므로 크기만 추출
                size = Long.parseLong((line.split("\\s+")[0]));
            }

            // 명령어 실행 종료 상태 확인
            int exitStatus = channel.getExitStatus();
            if (exitStatus == 0) {
                log.info("Command executed successfully, size: {}", size);
            } else {
                log.error("Command execution failed with exit status: {}", exitStatus);
            }

            channel.disconnect();
            session.disconnect();
            log.info("SFTP session disconnected");

        } catch (Exception e) {
            log.error("Error calculating folder size: {}", e.getMessage(), e);
        }



        return size;
    }




}
