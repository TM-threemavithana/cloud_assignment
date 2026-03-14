import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class TopKVolatilityMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text outKey = new Text();
    private final Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty()) {
            return;
        }

        String[] parts = line.split("\\s+");
        if (parts.length < 7) {
            context.getCounter("TopKVolatilityMapper", "MALFORMED_LINES").increment(1);
            return;
        }

        String symbol = parts[0];
        String year = parts[1];
        String avgClose = parts[2];
        String maxHigh = parts[3];
        String minLow = parts[4];
        String avgRange = parts[5];
        String tradingDays = parts[6];

        try {
            Double.parseDouble(avgRange);
            Integer.parseInt(tradingDays);
        } catch (NumberFormatException ex) {
            context.getCounter("TopKVolatilityMapper", "PARSE_ERRORS").increment(1);
            return;
        }

        outKey.set(year);
        outValue.set(avgRange + "\t" + symbol + "\t" + avgClose + "\t" + maxHigh + "\t" + minLow + "\t" + tradingDays);
        context.write(outKey, outValue);
    }
}
