#!/bin/bash

"$@" &

while kill -0 $!; do
    printf '.' > /dev/tty
    sleep 2
done

echo -e '\n'
