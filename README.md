Checks the last access time of a file in HDFS.

Code from https://community.cloudera.com/t5/Support-Questions/Is-there-anyway-to-get-last-access-time-of-hdfs-files/td-p/153375

# Use It

```bash
# Clone repo to hadoop login node and build jar.
./gradlew clean build

# Run it against your own directory.
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar 90 /user/jonpot

# Or send output to du to calculate usage.
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar 90 /user/jonpot | sed -e 's/^/\/hadoop-fuse/' | xargs du -h -c

# Or run it as user hdfs on hadoop login node to calculate all user directories.
cp ~/workspace/hdfs-access-time/build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar /tmp/
# switch to root.
sudo -u hdfs bash -c 'java -jar /tmp/FileStatusChecker-0.0.1-SNAPSHOT.jar 90 /user/jonpot' | sed -e 's/^/\/hadoop-fuse/' | xargs du -h -c

#echo `hadoop classpath`
#javac -cp `hadoop classpath` FileStatusChecker.java
#java FileStatusChecker
```
