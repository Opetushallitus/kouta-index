#!/bin/sh

#docker run -a stdout --rm -ti -v $(pwd)/test/resources/elastic_dump:/tmp elasticdump/elasticsearch-dump \
npx multielasticdump --direction=load --input=$(pwd)/test/resources/elastic_dump --output=$1 --includeType=data,mapping,analyzer,alias,settings,template