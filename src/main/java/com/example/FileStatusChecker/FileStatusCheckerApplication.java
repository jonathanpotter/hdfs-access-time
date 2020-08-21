package com.example.FileStatusChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
//import java.util.*;
//import java.net.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.security.UserGroupInformation;
//import org.apache.hadoop.io.*;
//import org.apache.hadoop.mapred.*;
//import org.apache.hadoop.util.*;


// For Date Conversion from long to human readable.
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@SpringBootApplication
public class FileStatusCheckerApplication {

        public void walk( FileSystem fs, String hdfsFilePath, long limitTimestamp) {

            try {
	        FileStatus[] status = fs.listStatus(new Path(hdfsFilePath));  // you need to pass in your hdfs path
                if (status == null) return;

	        for (FileStatus fileStatus : status) {
                    if ( fileStatus.isDirectory() ) {
                        walk(fs, fileStatus.getPath().toString(), limitTimestamp);

                    } else {
	                long lastAccessTimeLong = fileStatus.getAccessTime();
	                Date lastAccessTimeDate = new Date(lastAccessTimeLong);
	                DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
                        if ( limitTimestamp > lastAccessTimeLong ) {
                            System.out.printf("%-82s\n",fileStatus.getPath().toString());
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
                        // Define which recent files will be ignored in days from current
                        long limitInDays = 90;
                        long limitInMillis = limitInDays * 24 * 60 * 60 * 1000;

                        long currentTimestamp = System.currentTimeMillis();
                        long limitTimestamp = currentTimestamp - limitInMillis;
                        
                        System.out.println("Listing files that have not been accessed in last " + limitInDays + " days.");
                        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
                        Date currentTimeDate = new Date(currentTimestamp);
                        Date limitTimeDate = new Date(limitTimestamp);
                        System.out.println("Current Date: " + df.format(currentTimeDate));
                        System.out.println("Limit Date: " + df.format(limitTimeDate)); 

                        // Create config for Cavium ThunderX.
                        Configuration conf = new Configuration();
                        conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
                        conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
                        conf.set("fs.default.name", "hdfs://cavium-nn02");

                        // Subject is taken from current user context
                        UserGroupInformation.setConfiguration(conf);
                        UserGroupInformation.loginUserFromSubject(null);

			FileSystem fs = FileSystem.get(conf);
			String hdfsFilePath = "/user/jonpot/";
                        FileStatusCheckerApplication fw = new FileStatusCheckerApplication();
                        fw.walk(fs, hdfsFilePath, limitTimestamp);

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
