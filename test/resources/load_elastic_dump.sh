#!/bin/sh

docker run --rm -v $(pwd)/test/resources/elastic_dump:/tmp elasticdump/elasticsearch-dump \
multielasticdump --direction=load --input=/tmp --output=$1 --includeType=$2