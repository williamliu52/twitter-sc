import io.circe.syntax._
import org.apache.spark
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.sql._
import org.apache.spark.rdd._
import org.apache.spark.sql.types._
import scalaj.http._
import scala.concurrent.ExecutionContext.Implicits.global

object TwitterSC {
    def main(args: Array[String]) {
        // Create StreamingContext with 2 threads and batch interval of 2 seconds
        val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterSC")
        val sc = new SparkContext(conf)
        sc.setLogLevel("ERROR")
        val ssc = new StreamingContext(sc, Seconds(5))
        // Create SparkSession for SQL
        val sparkSesh = SparkSession
            .builder()
            .config("spark.master", "local")
            .getOrCreate()
        // Set checkpoint for RDD recovery so that updateStateByKey() can be used
        ssc.checkpoint("checkpoint_TwitterSC")

        // Connect to Twitter application running at port 9009
        val dataStream = ssc.socketTextStream("localhost", 9009)
        // dataStream.print()

        // Split tweet into words for filtering for video links
        val words = dataStream.flatMap(_.split(" "))
        val videos = words.filter(word => word.contains("https")).map(x => (x, 1))
        // Add count of each video to last count
        val vidCount = videos.updateStateByKey[Int](updateCount _)
        // Process each RDD generated in each interval
        vidCount.foreachRDD(processRDD(_, _, sparkSesh))

        // Run application
        ssc.start()
        ssc.awaitTermination()
    }

    // Function to increment hashtag value in the RDD
    def updateCount(newValues: Seq[Int], total: Option[Int]): Option[Int] = {
        val newCount = newValues.sum + total.getOrElse(0)
        Some(newCount)
    }

    def processRDD(rdd: RDD[(String, Int)], time: Time, sparkSesh: SparkSession) {
        try {
            // convert RDD to row RDD; need _1 because entries are tuples
            val rowRDD = rdd.map(entry => Row(entry._1, entry._2))
            // constructing schema to match structure of rows in rowRDD
            val fields = Array(StructField("vidUrl", StringType, nullable=true),
                            StructField("count", IntegerType, nullable=true))
            val schema = StructType(fields)
            // create dataFrame using rowRDD and schema
            val vidUrlDF = sparkSesh.createDataFrame(rowRDD, schema)
            // create temp view using dataFrame
            vidUrlDF.createOrReplaceTempView("videos")
            // select top 10 videos and counts,
            // converting to Arrays of strings and numbers, respectively
            val topVideos = sparkSesh.sql("SELECT vidUrl FROM videos ORDER BY count DESC LIMIT 10").collect().map(_.getString(0))
            val counts = sparkSesh.sql("SELECT count FROM videos ORDER BY count DESC LIMIT 10").collect().map(_.getInt(0))
            // send top videos to web application
            sendDataToApp(topVideos, "videos")
        } catch {
            case e : Throwable => println("processRDD exception: " + e)
        }
    }

    def sendDataToApp(items: Array[String], label: String) {
        // Encode array as JSON using circe
        // Docs: https://circe.github.io/circe/codec.html
        val json = items.asJson
        val requestBody = "{" + '"' + label + '"' + ":" + json + "}";
        // Create requests using the scalaj-http library
        // Doc: https://github.com/scalaj/scalaj-http
        try {
            Http("http://localhost:5000/updateData").postData(requestBody)
            .header("Content-Type", "application/json")
            .header("Charset", "UTF-8").asString
        } catch {
            case e : Throwable => println("Request error: " + e)
        }
    }
}
