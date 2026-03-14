import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class TopKVolatilityDriver {

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: TopKVolatilityDriver <input path> <output path> [k]");
            System.exit(-1);
        }

        Configuration conf = new Configuration();
        int topK = (args.length == 3) ? Integer.parseInt(args[2]) : 10;
        conf.setInt("topk.volatility.k", topK);

        Job job = Job.getInstance(conf, "Top-K Cryptocurrency Volatility by Year");
        job.setJarByClass(TopKVolatilityDriver.class);

        job.setMapperClass(TopKVolatilityMapper.class);
        job.setReducerClass(TopKVolatilityReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
