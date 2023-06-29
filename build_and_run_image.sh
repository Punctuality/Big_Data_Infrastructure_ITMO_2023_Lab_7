#!/bin/bash

cd ./contrib/ || exit 1

docker-compose up -d

cd ../

docker build --rm=true --build-arg HDFS_ADDRESS=${HDFS_NAMENODE_ADDRESS} \
  --build-arg MAIN_CLASS=OpenFoodFactsFiltering -t spark-app-lab7-filtering ./
docker build --rm=true --build-arg HDFS_ADDRESS=${HDFS_NAMENODE_ADDRESS} \
  --build-arg MAIN_CLASS=OpenFoodFactsClustering -t spark-app-lab7-clustering ./

if [ $? -ne 0 ]; then
  echo "Docker build failed"
  exit 1
else
  docker run --net contrib_default --link spark-master:spark-master spark-app-lab7-filtering
  docker run --net contrib_default --link spark-master:spark-master spark-app-lab7-clustering
fi