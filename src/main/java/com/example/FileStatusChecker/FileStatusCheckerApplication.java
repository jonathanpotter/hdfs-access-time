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

  public void walk(FileSystem fs, String hdfsFilePath, long limitTimestamp,
      boolean printFuse, boolean printNull, boolean printAccessTime,
      boolean printFileSize) {

    try {
      FileStatus[] status = fs.listStatus(new Path(hdfsFilePath));
      if (status == null) return;

      for (FileStatus fileStatus: status) {
        if (fileStatus.isDirectory()) {
          walk(fs, fileStatus.getPath().toString(), limitTimestamp, printFuse,
              printNull, printAccessTime, printFileSize);
        } else {
          long lastAccessTimeLong = fileStatus.getAccessTime();
          Date lastAccessTimeDate = new Date(lastAccessTimeLong);
          StringBuilder sb = new StringBuilder();
          if (limitTimestamp > lastAccessTimeLong) {
            // Add hadoop fuse mount prefix.
            if (printFuse) {
              sb.append("/hadoop-fuse");
            }
            // Add path and filename.
            sb.append(fileStatus.getPath().getPathWithoutSchemeAndAuthority(fileStatus.getPath()).toString());
            // Add last access time.
            if (printAccessTime) {
              sb.append(",").append(lastAccessTimeLong);
              // Or human readable timestamp.
              //DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
              //sb.append(",").append(df.format(lastAccessTimeDate));
            }
            // Add file size.
            if (printFileSize) {
              sb.append(",").append(fileStatus.getLen());
            }
            // Add line terminator.
            if (printNull) {
                sb.append("\u0000");
            } else {
                sb.append("\n");
            }
            System.out.printf("%s", sb.toString());
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
      boolean printFuse = false;
      boolean printNull = false;
      boolean printAccessTime = false;
      boolean printFileSize = false;

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
        if (args[i].equals("-print-atime")) {
          printAccessTime = true;
        }
        if (args[i].equals("-print-size")) {
          printFileSize = true;
        }
      }

      // Define which recent files will be ignored in days from current
      long limitInMillis = limitInDays * 24 * 60 * 60 * 1000;

      long currentTimestamp = System.currentTimeMillis();
      long limitTimestamp = currentTimestamp - limitInMillis;

      Date currentTimeDate = new Date(currentTimestamp);
      Date limitTimeDate = new Date(limitTimestamp);
      //DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
      //System.out.println("Listing files that have not been accessed in last "
      //    + limitInDays + " days.");
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
      fw.walk(fs, hdfsFilePath, limitTimestamp, printFuse, printNull, printAccessTime,
          printFileSize);

    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
