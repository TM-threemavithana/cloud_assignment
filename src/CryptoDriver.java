import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * CryptoDriver — Entry point for the Hadoop MapReduce cryptocurrency analysis
 * job.
 *
 * Usage: hadoop jar crypto-analysis.jar CryptoDriver &lt;input_path&gt;
 * &lt;output_path&gt;
 *
 * This driver configures and submits a MapReduce job that:
 * 1. Reads a CSV file of daily cryptocurrency prices from HDFS
 * 2. Maps each row to (Symbol+Year → High,Low,Close) pairs
 * 3. Uses a Combiner (CryptoCombiner) for local pre-aggregation to reduce
 * shuffle
 * 4. Reduces all values per Symbol+Year into yearly summary metrics
 *
 * @author Cloud Computing Assignment — EE7222/EC7204
 */
public class CryptoDriver {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: CryptoDriver <input path> <output path>");
            System.exit(-1);
        }

        // Create a new Hadoop configuration and job
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Cryptocurrency Yearly Price Analysis");

        // Set JAR by class so Hadoop can find our classes
        job.setJarByClass(CryptoDriver.class);

        // Set Mapper, Combiner, and Reducer classes
        job.setMapperClass(CryptoMapper.class);
        job.setCombinerClass(CryptoCombiner.class); // Local pre-aggregation for efficiency
        job.setReducerClass(CryptoReducer.class);

        // Set output key/value types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Set input and output paths from command-line arguments
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Submit the job and wait for completion
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
