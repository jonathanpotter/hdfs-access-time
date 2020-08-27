#!/bin/bash

read_input() {

    # Units are milliseconds.
    currentTime=$(date +%s%3N)
    millisPerDay=$((1000 * 60 * 60 * 24))
    minAccessTime=${currentTime}
    maxAccessTime=1
    maxDaysAllowed=1460
    fileCount=0
    groupSize=30
    groupCount=$((${maxDaysAllowed} / ${groupSize}))
    # Initialize histogram array.
    for ((i=0;i<${groupCount};i++)); do
        groupValue[$i]=0
    done

    # Each line is a file. First field is file path. Second field is atime. 
    while read line
    do
        accessTime=$(echo "$line" | cut -d "," -f 2)

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
        ((groupValue[${groupId}]+=1))
        ((fileCount+=1))
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
    echo "   Total files:  ${fileCount}"
    if [ ${fileCount} -gt 0 ]; then
        #echo "   Average atime age in days:  127.981  days"
        echo "   Oldest atime age file in days:  ${oldestAccessTimeDay}  days"
        echo "   Youngest atime age file in days:  ${youngestAccessTimeDay}  days"
        #echo "   Standard deviation atime age in days:  98.5828  days"
        echo ""
        lastGroup=$(((${oldestAccessTimeDay} / ${groupSize}) + 1))
        for ((i=0;i<${lastGroup};i++)); do
            groupStartAge=$(($i * ${groupSize}))
            groupEndAge=$((($i * ${groupSize}) + ${groupSize}))
            printf "  [ %4s  - %4s days ]:    %6s\n" ${groupStartAge} ${groupEndAge} ${groupValue[$i]}
        done
    fi
    echo ""
    # See https://www.admin-magazine.com/HPC/Articles/Understanding-the-Status-of-Your-Filesystem
}

read_input
print_results
