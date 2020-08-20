package com.example.FileStatusChecker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//import java.io.*;
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

	public static void main(String[] args) {
		SpringApplication.run(FileStatusCheckerApplication.class, args);
		System.out.println("Getting last accessed time.");

		try {
                        // Create config for Cavium ThunderX.
                        Configuration conf = new Configuration();
                        conf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
                        conf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
                        conf.set("fs.default.name", "hdfs://cavium-nn02");

                        UserGroupInformation.setConfiguration(conf);
                        // Subject is taken from current user context
                        UserGroupInformation.loginUserFromSubject(null);

			FileSystem fs = FileSystem.get(conf);
			String hdfsFilePath = "hdfs://cavium-nn02/user/jonpot/aa.txt";
			FileStatus[] status = fs.listStatus(new Path(hdfsFilePath));  // you need to pass in your hdfs path

			for (FileStatus fileStatus : status) {
				long lastAccessTimeLong = fileStatus.getAccessTime();
				Date lastAccessTimeDate = new Date(lastAccessTimeLong);
				DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
				System.out.println("The file '" + hdfsFilePath + "' was accessed last at: " + df.format(lastAccessTimeDate));
			}

		} catch(Exception e) {
			System.out.println("File not found");
			e.printStackTrace();
		}
	}
}
