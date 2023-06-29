package com.github.Punctuality

import com.github.Punctuality.model.SugarEnergy
import com.github.Punctuality.util.Decompressing
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkFiles
import org.apache.spark.sql._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.rdd.RDD

import scala.util.Try

object OpenFoodFactsClustering extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("OpenFoodFactsClustering")
      .master("spark://spark-master:7077")
      .config("spark.sql.caseSensitive", "true")
      .getOrCreate()

    val namenodeAddress = args.head

    val clusteringData = spark.read.parquet(s"hdfs://$namenodeAddress/user/root/preparedData.parquet").cache()
//    val clusteringData = spark.read.table("preparedClusteringData").cache()

    clusteringData.printSchema()

    // Trains a k-means model.
    val kmeans = new KMeans()
      .setK(4)
      .setSeed(1L)
      .setFeaturesCol("features")
      .setPredictionCol("cluster")
    val model = kmeans.fit(clusteringData)

    // Make predictions
    val predictions: DataFrame = model.transform(clusteringData).cache()

    // Evaluate clustering by computing Silhouette score
    val evaluator = new ClusteringEvaluator().setFeaturesCol("features").setPredictionCol("cluster")

    val silhouette = evaluator.evaluate(predictions)
    logger.info(s"Silhouette with squared euclidean distance = $silhouette")

    // Shows the result.
    logger.info("Cluster Centers: ")
    model.clusterCenters.foreach(array => logger.info(array.toString()))

    val groupedDataset = predictions.groupBy("cluster").count().cache()

    groupedDataset.show()
  }
}
