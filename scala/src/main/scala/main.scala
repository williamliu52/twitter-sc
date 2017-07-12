import org.apache.spark
import org.apache.spark._
import org.apache,spark.streaming._
import org.apache.spark.sql.SparkSession

object TwitterSC {
    def main(args: Array[String]) {
        // Create StreamingContext with 2 threads and batch interval of 2 seconds
        val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterSC")
        val sc = new SparkContext(conf=conf).setLogLevel("ERROR")
        val ssc = new StreamingContext(sc, Seconds(2))
        // Set checkpoint for RDD recovery
        ssc.checkpoint("checkpoint_TwitterSC")

        val lines = ssc.socketTextStream("localhost", 9009)
    }
}
