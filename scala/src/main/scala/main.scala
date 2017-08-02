import org.apache.spark
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.sql._
import org.apache.spark.rdd._
import org.apache.spark.sql.types._
import dispatch._, Defaults._

object TwitterSC {
    def main(args: Array[String]) {
        // Create StreamingContext with 2 threads and batch interval of 2 seconds
        val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterSC")
        val sc = new SparkContext(conf)
        sc.setLogLevel("ERROR")
        val ssc = new StreamingContext(sc, Seconds(2))
        // Create SparkSession for SQL
        val sparkSesh = SparkSession
        .builder()
        .config("spark.master", "local")
        .getOrCreate()
        // Set checkpoint for RDD recovery so that updateStateByKey() can be used
        ssc.checkpoint("checkpoint_TwitterSC")

        // Connect to Twitter application running at 9009
        val dataStream = ssc.socketTextStream("localhost", 9009)
        dataStream.print()

        // Split tweet into words for filtering for hashtags
        val words = dataStream.flatMap(_.split(" "))
        val hashtags = words.filter(word => word.contains('#')).map(x => (x, 1))
        // Add count of each hashtag to last count
        val tagTotals = hashtags.updateStateByKey[Int](updateTagCount _)
        // Process each RDD generated in each interval
        // TODO: implement processRDD function
        tagTotals.foreachRDD(processRDD(_, _, sparkSesh))

        // Run application
        ssc.start()
        ssc.awaitTermination()
    }

    def updateTagCount(newValues: Seq[Int], total: Option[Int]): Option[Int] = {
        val newCount = newValues.sum + total.getOrElse(0)
        Some(newCount)
    }

    def processRDD(rdd: RDD[(String, Int)], time: Time, sparkSesh: SparkSession) {
        println("-----------" + time + " -----------")
        try {
            // convert RDD to row RDD; need _1 because entries are tuples
            val rowRDD = rdd.map(entry => Row(entry._1, entry._2))
            // constructing schema to match structure of rows in rowRDD
            val fields = Array(StructField("hashtag", StringType, nullable=true),
                            StructField("count", IntegerType, nullable=true))
            val schema = StructType(fields)
            // create dataFrame using rowRDD and schema
            val hashtagsDF = sparkSesh.createDataFrame(rowRDD, schema)
            // create temp view using dataFrame
            hashtagsDF.createOrReplaceTempView("hashtags")
            // select top 10 hashtags and counts,
            // converting to Arrays of strings and numbers, respectively
            val hashtags = sparkSesh.sql("SELECT hashtag FROM hashtags ORDER BY count DESC LIMIT 10").collect().map(_.getString(0))
            val counts = sparkSesh.sql("SELECT count FROM hashtags ORDER BY count DESC LIMIT 10").collect().map(_.getInt(0))
            // send top hashtags to web application
            sendDataToApp(hashtags, counts, 10)
        } catch {
            case e : Throwable => println("processRDD exception: " + e)
        }
    }

    def sendDataToApp(hashtags: Array[String], counts: Array[Int], rows: Int) {
        val myRequest = url("http://localhost:5001/updateData")
        def postWithParams = myRequest << List(
            ("label" -> hashtags.mkString(",")),
            ("data" -> counts.mkString(","))
        )
        val response = postWithParams
    }
}
