Checks the last access time of files in HDFS.

Code from https://community.cloudera.com/t5/Support-Questions/Is-there-anyway-to-get-last-access-time-of-hdfs-files/td-p/153375

Given N number of days and a path, the program will recursively check all files in the path and list out those that have not been accessed in the last N days. The output is a list of files separated by the null character (for easy processing by du). The full path to the file is printed prefixed by `/hadoop-fuse`.

# Usage

```bash
java -jar FileStatusChecker*.jar PATH -atime DAYS

# Clone repo to hadoop login node and build jar.
./gradlew clean build

# Run it against your own directory.
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar /user/jonpot -atime 90

# Or send output to du to calculate usage through FUSE mount.
java -jar build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar /user/jonpot -atime 90 | sed -e 's/^/\/hadoop-fuse/' | tr '\n' '\0' | du -s -h -c --files0-from=-

# Or run it as user hdfs on hadoop login node to calculate all user directories.
cp ~/workspace/hdfs-access-time/build/libs/FileStatusChecker-0.0.1-SNAPSHOT.jar /tmp/
# switch to root.
USER_HOME_DIR=/hadoop-fuse/user/*
OUTPUT_FILE=last-access-audit-$(date "+%Y%m%d").txt
> ${OUTPUT_FILE}
for dir in ${USER_HOME_DIR}; do
    user="$(basename ${dir})"
    echo -n "${user}," >> ${OUTPUT_FILE}
    sudo user=${user} -u hdfs bash -c 'java -jar /tmp/FileStatusChecker-0.0.1-SNAPSHOT.jar /user/${user} -atime 180' | \
        sed -e 's/^/\/hadoop-fuse/' | tr '\n' '\0' | du -s -h -c --files0-from=- | tail -n 1 | cut -f 1 >> ${OUTPUT_FILE}
    #sudo user=${user} -u hdfs bash -c 'java -jar /tmp/FileStatusChecker-0.0.1-SNAPSHOT.jar /user/${user} -atime 180 -print0' | du -s -h -c --files0-from=- | tail -n 1 | cut -f 1 >> ${OUTPUT_FILE}
done

# Sort the output by size.
awk 'BEGIN { FS = "," } ; { print $2,$1 }' ${OUTPUT_FILE} | sort -hr

#echo `hadoop classpath`
#javac -cp `hadoop classpath` FileStatusChecker.java
#java FileStatusChecker
```
