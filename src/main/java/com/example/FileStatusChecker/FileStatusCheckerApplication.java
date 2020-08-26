package com.example.FileStatusChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.security.UserGroupInformation;

// For Date Conversion from long to human readable.
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootApplication
public class FileStatusCheckerApplication {

  public void walk(FileSystem fs, String hdfsFilePath, long limitTimestamp, boolean printNull, boolean printAccessTime) {

    try {
      FileStatus[] status = fs.listStatus(new Path(hdfsFilePath));
      if (status == null) return;

      for (FileStatus fileStatus: status) {
        if (fileStatus.isDirectory()) {
          walk(fs, fileStatus.getPath().toString(), limitTimestamp, printNull, printAccessTime);

        } else {
          long lastAccessTimeLong = fileStatus.getAccessTime();
          Date lastAccessTimeDate = new Date(lastAccessTimeLong);
          DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
          if (limitTimestamp > lastAccessTimeLong) {
            if (printNull) {
              System.out.printf("/hadoop-fuse%s\u0000", fileStatus.getPath().getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toString());
            } else {
              if (printAccessTime) {
                //System.out.printf("%s,%s\n", fileStatus.getPath().getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toString(), df.format(lastAccessTimeDate));
                System.out.printf("%s,%s\n", fileStatus.getPath().getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toString(), lastAccessTimeLong);
              } else {
                System.out.printf("%s\n", fileStatus.getPath().getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toString());
              }
            }
            //System.out.println("└── " + df.format(lastAccessTimeDate));
          }
        }
      }
    } catch(Exception e) {
      System.out.println("File not found");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    SpringApplication.run(FileStatusCheckerApplication.class, args);

    try {
      // Set defaults
      long limitInDays = 0;
      boolean printNull = false;
      boolean printAccessTime = false;

      // Evaluate arguments
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-atime")) {
          if (i + 1 < args.length) {
            limitInDays = Long.parseLong(args[i + 1]);
          }
          else {
            System.out.println("Error: Option -atime requires a value.");
            System.exit(1);
          }
        }
        if (args[i].equals("-print0")) {
          printNull = true;
        }
        if (args[i].equals("-u")) {
          printAccessTime = true;
        }
      }

      // Define which recent files will be ignored in days from current
      long limitInMillis = limitInDays * 24 * 60 * 60 * 1000;

      long currentTimestamp = System.currentTimeMillis();
      long limitTimestamp = currentTimestamp - limitInMillis;

      DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
      Date currentTimeDate = new Date(currentTimestamp);
      Date limitTimeDate = new Date(limitTimestamp);
      //System.out.println("Listing files that have not been accessed in last " + limitInDays + " days.");
      //System.out.println("Current Date: " + df.format(currentTimeDate));
      //System.out.println("Limit Date: " + df.format(limitTimeDate));
      // Create config for Cavium ThunderX.
      Configuration conf = new Configuration();
      conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
      conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
      conf.set("fs.default.name", "hdfs://cavium-nn02");

      // Subject is taken from current user context
      UserGroupInformation.setConfiguration(conf);
      UserGroupInformation.loginUserFromSubject(null);

      FileSystem fs = FileSystem.get(conf);
      String hdfsFilePath = args[0];
      FileStatusCheckerApplication fw = new FileStatusCheckerApplication();
      fw.walk(fs, hdfsFilePath, limitTimestamp, printNull, printAccessTime);

    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
