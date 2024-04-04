#!/bin/bash

set -euo pipefail

# Check if argument is a number
if ! [[ "$1" =~ ^[0-9]+$ ]]
then
    echo "Error: Argument must be a positive integer."
    exit 1
fi

# Run the command for each number up to the argument
for ((i=1;i<=$1;i++))
do
    echo "[+] java -jar VirtualNetwork.jar -v s$i -a arp_cache &"
    java -jar VirtualNetwork.jar -v s$i -a arp_cache &> /dev/null &
done

