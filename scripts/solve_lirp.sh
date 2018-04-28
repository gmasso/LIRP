#!/usr/bin/env bash

ulimit -n 2048

nbThreads=3

dirName="../Instances/Complete/Small/"
typeInst="2l3dc0-10r"
echo "*********************************"
echo "Exploring instance directory $dirName for instances of type $typeInst..."
echo "*********************************"
echo " "

# Number of instances treated
tCount=0
# Count the total number of instances to treat beforehand
total=`ls $dirName$typeInst*.json | wc -l | sed 's/ //g'`

for i in $dirName$typeInst*.json; do
    # Get the name of the instance
    instID=`echo $i | sed 's/..\/Instances\/Complete\/Small\///' | sed 's/.json//'`

    # Create directories to store the log files and the solution files corresponding to every instance
    solDir="../Solutions/$instID"
    logDir="../Log files/$instID"

    # Create a directory to store the solutions for this instance if does not already exist
    if [ ! -e "$solDir" ]; then mkdir "$solDir"; fi
    # Create a directory to store the solutions for this instance if does not already exist
    if [ ! -e "$logDir" ]; then mkdir "$logDir"; fi

    echo "Treating instance $instID... ($tCount/$total)"
    java -jar -Djava.library.path=/home-isis/auto/dapi/gmasso16/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux Resolution.jar -loop_lvl=[1] $i &
    tCount=$(( tCount + 1 ))
    if (( $tCount % 3 == 0 )); then wait; fi
done

echo "All instance files have been treated successfully."
