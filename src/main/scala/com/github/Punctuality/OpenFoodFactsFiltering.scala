package com.github.Punctuality

import com.github.Punctuality.model.SugarEnergy
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkFiles
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.evaluation.ClusteringEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.rdd.RDD
import org.apache.spark.sql._
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.util.Try

object OpenFoodFactsFiltering extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder.appName("OpenFoodFactsFiltering")
      .master("spark://spark-master:7077")
      .config("spark.sql.caseSensitive", "true")
      .getOrCreate()

    val namenodeAddress = args.head

    val rdd = spark.sparkContext.textFile(s"hdfs://$namenodeAddress/openfoodfacts.jsonl")
    val jsonRDD = rdd.flatMap(str => Try(parse(str)).toOption)
    val sugarEnergyRDD: RDD[SugarEnergy] = jsonRDD.flatMap(json => Try(json.as[SugarEnergy]).toOption).cache()

    sugarEnergyRDD.take(10).foreach(sr => logger.info(sr.toString))

    logger.info(s"Total SE: ${sugarEnergyRDD.count()}")

    import spark.implicits._
    val nutrimentsDS = sugarEnergyRDD.toDS()

    val clusteringData: DataFrame = new VectorAssembler()
      .setInputCols(Array("sugar", "energy"))
      .setOutputCol("features")
      .transform(nutrimentsDS)

    clusteringData.printSchema()

//    clusteringData.write.save(s"http://vps-ecdd4e30.vps.ovh.net:9864/webhdfs/v1/preparedData.parquet")
        clusteringData.write.parquet(s"hdfs://$namenodeAddress/user/root/preparedData.parquet")
  }
}
