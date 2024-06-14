#!/bin/bash

easy_set() {
        echo "$1,$2" >> easy-db
}

easy_get() {
        grep "^$1," easy-db | sed -e "s/^$1,//" | tail -n 1
}