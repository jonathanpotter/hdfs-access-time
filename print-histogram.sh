#!/bin/bash

read_input() {

    # Units are milliseconds.
    currentTime=$(date +%s%3N)
    millisPerDay=$((1000 * 60 * 60 * 24))
    minAccessTime=${currentTime}
    maxAccessTime=1
    maxDaysAllowed=1825             # Upper limit of oldest access time in days.
    fileCountSum=0                  # Running total count of all files.
    fileSizeSum=0                   # Running total sum of all file sizes.
    binSize=30                      # Number of days in each histogram bin. 
    binCount=$((${maxDaysAllowed} / ${binSize}))    # Number of histogram bins.
    for ((i=0;i<${binCount};i++)); do
        binFileCount[$i]=0          # Running total count of files in bin.
        binFileSize[$i]=0           # Running total sum of file sizes in bin.
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

        # Increment the corresponding histogram bin.
        ageDays=$(((${currentTime} - ${accessTime}) / ${millisPerDay}))
        if [ "$ageDays" -gt "$maxDaysAllowed" ]; then
            echo "Some files have not been accessed in more than ${maxDaysAllowed} days."
            echo "The utility does not support files this old."
            exit 1
        fi
        binId=$((${ageDays} / ${binSize}))
        ((binFileCount[${binId}]+=1))
        ((binFileSize[${binId}]+=${fileSize}))
        ((fileCountSum+=1))
        ((fileSizeSum+=${fileSize}))
    done < "${1:-/dev/stdin}"
}

print_results() {

    oldestAccessTime=$((${currentTime} - ${minAccessTime}))
    youngestAccessTime=$((${currentTime} - ${maxAccessTime}))
    oldestAccessTimeDay=$((${oldestAccessTime} / ${millisPerDay}))
    youngestAccessTimeDay=$((${youngestAccessTime} / ${millisPerDay}))

    echo ""
    if [ ${fileCountSum} -gt 0 ]; then
        #echo "   Average atime age in days:  127.981  days"
        echo "   Oldest atime age file in days:  ${oldestAccessTimeDay}  days"
        echo "   Youngest atime age file in days:  ${youngestAccessTimeDay}  days"
        #echo "   Standard deviation atime age in days:  98.5828  days"
        echo ""
        echo "      LAST ACCESSED         FILE COUNT     SIZE (GB)     SIZE (%)"
        fileSizeSumGB=$(echo "scale=1; ${fileSizeSum} / 1024 / 1024 / 1024" | bc)
        lastbin=$(((${oldestAccessTimeDay} / ${binSize}) + 1))
        for ((i=0;i<${lastbin};i++)); do
            binStartAge=$(($i * ${binSize}))
            binEndAge=$((($i * ${binSize}) + ${binSize}))
            binFileSizeGB=$(echo "scale=1; ${binFileSize[$i]} / 1024 / 1024 / 1024" | bc)
            binFileSizePercent=$(echo "scale=3; 100 * ${binFileSize[$i]} / ${fileSizeSum}" | bc)
            printf "  [ %4s  - %4s days ]       %8s      %8s        %5.1f\n" ${binStartAge} ${binEndAge} ${binFileCount[$i]} ${binFileSizeGB} ${binFileSizePercent}
        done
        printf "      TOTAL       %20s      %8s        100.0\n" ${fileCountSum} ${fileSizeSumGB}
    fi
    echo ""
    # See https://www.admin-magazine.com/HPC/Articles/Understanding-the-Status-of-Your-Filesystem
}

read_input
print_results
