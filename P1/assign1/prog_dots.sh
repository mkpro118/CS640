#!/bin/bash

"$@" &
PSID=$!

while kill -0 $PSID > /dev/null 2>&1 ; do
    printf '.' > /dev/tty
    sleep 2
done

echo -e '\n'
