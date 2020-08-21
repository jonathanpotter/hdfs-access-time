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

        public void walk( FileSystem fs, String hdfsFilePath ) {
        //public static String walk( String path ) {

            try {
	        FileStatus[] status = fs.listStatus(new Path(hdfsFilePath));  // you need to pass in your hdfs path
                //System.out.printf("%-82s%-22s\n","File","Last Accessed");
	        for (FileStatus fileStatus : status) {
                    if ( fileStatus.isDirectory() ) {
                        walk(fs, fileStatus.getPath().toString());

                    } else {
	                long lastAccessTimeLong = fileStatus.getAccessTime();
	                Date lastAccessTimeDate = new Date(lastAccessTimeLong);
	                DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
                        System.out.printf("%-82s%-22s\n",fileStatus.getPath().toString(),df.format(lastAccessTimeDate));
                    }
                }
            } catch(Exception e) {
               	System.out.println("File not found");
               	e.printStackTrace();
            }
            //File root = new File( path );
            //File[] list = root.listFiles();

            //if (list == null) return;

            //for ( File f : list ) {
            //    if ( f.isDirectory() ) {
            //        walk( f.getAbsolutePath() );
            //        System.out.println( "Dir:" + f.getAbsoluteFile() );
            //    }
            //    else {
            //        System.out.println( "File:" + f.getAbsoluteFile() );
            //    }
            //}
        }

	public static void main(String[] args) {
		SpringApplication.run(FileStatusCheckerApplication.class, args);

		try {
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
                        fw.walk(fs, hdfsFilePath);

		} catch(Exception e) {
			System.out.println("File not found");
			e.printStackTrace();
		}
	}
}
