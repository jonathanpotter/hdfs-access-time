Checks the last access time of a file in HDFS.

Code from https://community.cloudera.com/t5/Support-Questions/Is-there-anyway-to-get-last-access-time-of-hdfs-files/td-p/153375

# Use It

```bash
./gradlew clean build
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar

# Or send it to du.
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar | sed -e 's/^/\/hadoop-fuse/' | xargs du -h -c

#echo `hadoop classpath`
#javac -cp `hadoop classpath` FileStatusChecker.java
#java FileStatusChecker
```
