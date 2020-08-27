#!/bin/bash

read_input() {

    # Units are milliseconds.
    currentTime=$(date +%s%3N)
    millisPerDay=$((1000 * 60 * 60 * 24))
    minAccessTime=${currentTime}
    maxAccessTime=1
    maxDaysAllowed=1460             # Upper limit of oldest access time in days.
    fileCountSum=0                  # Running total count of all files.
    groupSize=30                    # Number of days in each histogram group. 
    groupCount=$((${maxDaysAllowed} / ${groupSize}))    # Number of histogram groups.
    for ((i=0;i<${groupCount};i++)); do
        groupFileCount[$i]=0        # Running total count of files in group.
        groupFileSize[$i]=0         # Running total sum of file sizes in group.
    done


    # Each line is a file. First field is file path. Second field is atime. 
    while IFS="," read -r ignorePath accessTime fileSize
    do

        # If atime is larger or smaller than any previous, set new min or max.
        if [ "$accessTime" -lt "$minAccessTime" ]; then
            minAccessTime=${accessTime}
        fi
        if [ "$accessTime" -gt "$maxAccessTime" ]; then
            maxAccessTime=${accessTime}
        fi

        # Increment the corresponding histogram group.
        ageDays=$(((${currentTime} - ${accessTime}) / ${millisPerDay}))
        if [ "$ageDays" -gt "$maxDaysAllowed" ]; then
            echo "Some files have not been accessed in more than ${maxDaysAllowed} days."
            echo "The utility does not support files this old."
            exit 1
        fi
        groupId=$((${ageDays} / ${groupSize}))
        ((groupFileCount[${groupId}]+=1))
        ((groupFileSize[${groupId}]+=${fileSize}))
        ((fileCountSum+=1))
    done < "${1:-/dev/stdin}"
}

print_results() {

    oldestAccessTime=$((${currentTime} - ${minAccessTime}))
    youngestAccessTime=$((${currentTime} - ${maxAccessTime}))
    oldestAccessTimeDay=$((${oldestAccessTime} / ${millisPerDay}))
    youngestAccessTimeDay=$((${youngestAccessTime} / ${millisPerDay}))

    echo "   =============="
    echo "   Atime results:"
    echo "   =============="
    echo "   Total files:  ${fileCountSum}"
    if [ ${fileCountSum} -gt 0 ]; then
        #echo "   Average atime age in days:  127.981  days"
        echo "   Oldest atime age file in days:  ${oldestAccessTimeDay}  days"
        echo "   Youngest atime age file in days:  ${youngestAccessTimeDay}  days"
        #echo "   Standard deviation atime age in days:  98.5828  days"
        echo ""
        echo "      LAST ACCESSED         FILE COUNT        SIZE (GB)"
        lastGroup=$(((${oldestAccessTimeDay} / ${groupSize}) + 1))
        for ((i=0;i<${lastGroup};i++)); do
            groupStartAge=$(($i * ${groupSize}))
            groupEndAge=$((($i * ${groupSize}) + ${groupSize}))
            groupFileSizeGB=$(echo "scale=1; ${groupFileSize[$i]} / 1024 / 1024 / 1024" | bc)
            printf "  [ %4s  - %4s days ]         %6s         %8s\n" ${groupStartAge} ${groupEndAge} ${groupFileCount[$i]} ${groupFileSizeGB}
        done
    fi
    echo ""
    # See https://www.admin-magazine.com/HPC/Articles/Understanding-the-Status-of-Your-Filesystem
}

read_input
print_results
