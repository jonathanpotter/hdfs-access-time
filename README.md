A utility tool that functions similarly to find with HDFS awareness. Originally created to gather statistics around last access time of files in HDFS. The utility included with Hadoop, `hdfs dfs -find`, does not support atime queries.

The program will recursively check all files in the path and print their full path on a new line on stdout. Given  the argument `-atime` and N number of days, the list will be filtered to only output those files that have not been accessed in the last N days. 

You can prepend the FUSE mount and replace newlines with null characters and process with `du` to calculate the total usage in each users HDFS home directory. Optionally, the argument `-print0` will cause the output to be separated by null character instead of the newline character and prepend each line with `/hadoop-fuse`.

The argument `-print-atime` will include the last access time in the output. The argument `-print-size` will include the file size in the output.

## Usage

```bash
SYNOPSIS
      java -jar FileStatusChecker*.jar [<PATH>] [-atime <NUMBER_OF_DAYS>] [-print-atime]

Generic options supported are
<PATH>                              Recursively print all files in this path.
-atime <NUMBER_OF_DAYS>             Print only files that were last accessed more than N number days ago.
-print-atime                        Print last access time in epoch format.
-print-size                         Print file size in bytes.

---

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
done

# Sort the output by size.
awk 'BEGIN { FS = "," } ; { print $2,$1 }' ${OUTPUT_FILE} | sort -hr

# Generate a histogram.
# switch to regular user.
cp ~/workspace/hdfs-access-time/print-histogram.sh /tmp/
# switch to root.
USER_HOME_DIR=/hadoop-fuse/user/*
OUTPUT_FILE=last-access-histogram-$(date "+%Y%m%d").txt
> ${OUTPUT_FILE}
for dir in ${USER_HOME_DIR}; do
    user="$(basename ${dir})"
    echo -e "--- ${user}\n" >> ${OUTPUT_FILE}
    sudo user=${user} -u hdfs bash -c 'java -jar /tmp/FileStatusChecker-0.0.1-SNAPSHOT.jar /user/${user} -atime 0 -print-atime -print-size' | \
        /tmp/print-histogram.sh >> ${OUTPUT_FILE}
    echo -e "---\n" >> ${OUTPUT_FILE}
done
```
## Reference

- https://community.cloudera.com/t5/Support-Questions/Is-there-anyway-to-get-last-access-time-of-hdfs-files/td-p/153375
