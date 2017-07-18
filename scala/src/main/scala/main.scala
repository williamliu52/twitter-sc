import org.apache.spark
import org.apache.spark._
import org.apache,spark.streaming._
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd._

object TwitterSC {
    def main(args: Array[String]) {
        // Create StreamingContext with 2 threads and batch interval of 2 seconds
        val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterSC")
        val sc = new SparkContext(conf)
        sc.setLogLevel("ERROR")
        val ssc = new StreamingContext(sc, Seconds(2))
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
        tagTotals.foreachRDD(processRDD _)


        // Run application
        ssc.start()
        ssc.awaitTermination()
    }

    def updateTagCount(newValues: Seq[Int], total: Option[Int]): Option[Int] = {
        val newCount = newValues.sum + total.getOrElse(0)
        Some(newCount)
    }

    def processRDD(rdd: RDD[(String, Int)], time: Time) {

    }
}
